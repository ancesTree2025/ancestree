import { Result } from 'typescript-result';
import type { Marriages, People, PersonID, Tree } from './types';
import exampleTree from '../data/exampleTree.json';
import { z } from 'zod';
import { env } from '$env/dynamic/public';

export const personIdSchema = z.string();

const personSchema = z.object({
  data: z.object({
    id: personIdSchema,
    name: z.string(),
    gender: z.string()
  }),
  id: personIdSchema,
  depth: z.number()
});

export const treeSchema = z.object({
  root: personSchema,
  nodes: z.array(personSchema),
  edges: z.array(
    z.object({
      node1: personIdSchema,
      node2: personIdSchema,
      tag: z.union([z.literal(''), z.literal('UN')])
    })
  )
});

type ApiResponse = z.infer<typeof treeSchema>;

export function apiResponseToTree(res: ApiResponse): Tree {
  // Create mappings from people ids to their info and depths
  const people: People = [];
  const depths: Map<PersonID, number> = new Map();

  for (const person of res.nodes) {
    people.push([person.id, { name: person.data.name }, person.data.gender]);
    depths.set(person.id, person.depth);
  }

  // Create list of marriages, and mapping from parents to children

  const simpleMarriages: { person1: PersonID; person2: PersonID; type: 'married' | 'unmarried' }[] =
    [];
  const children = new Map<PersonID, PersonID[]>();

  for (const edge of res.edges) {
    const depth1 = depths.get(edge.node1)!;
    const depth2 = depths.get(edge.node2)!;

    // If two nodes have same depth then they are married

    if (depth1 === depth2) {
      simpleMarriages.push({
        person1: edge.node1,
        person2: edge.node2,
        type: edge.tag === 'UN' ? 'unmarried' : 'married'
      });
      continue;
    }

    // Find parent and child from depths

    let parent: PersonID;
    let child: PersonID;

    if (depth1 < depth2) {
      parent = edge.node1;
      child = edge.node2;
    } else {
      parent = edge.node2;
      child = edge.node1;
    }

    // Add child to parent's children

    let parentChildren = children.get(parent);
    if (parentChildren === undefined) {
      parentChildren = [];
      children.set(parent, parentChildren);
    }
    parentChildren.push(child);
  }

  // Merge marriage between parents and their children

  const marriages: Marriages = [];

  for (const parents of simpleMarriages) {
    marriages.push({
      parents: [parents.person1, parents.person2],
      children: (children.get(parents.person1) ?? []).filter((child) =>
        (children.get(parents.person2) ?? []).includes(child)
      ),
      focuses: [res.root.id],
      type: parents.type
    });
  }

  return {
    focus: res.root.id,
    secondary: [res.root.id],
    pivot: res.root.id,
    pivotPosition: { x: 0, y: 0 },
    people,
    marriages
  };
}

export async function fetchTree(
  name: string,
  useFakeData: boolean,
  width: number,
  height: number
): Promise<Result<Tree, string>> {
  if (useFakeData) {
    return Result.ok(exampleTree as Tree);
  }

  const response = await Result.fromAsyncCatching(
    fetch(`${env.PUBLIC_API_BASE_URL}/${name}?width=${width}&height=${height}`)
  ).mapError(() => 'Could not connect to server');

  if (response.getOrNull()?.status === 404) {
    return Result.error('Person not found');
  }
  const parsed = response.mapCatching(
    async (response) => (await response.json()) as ApiResponse,
    () => 'Could not parse server response'
  );
  return parsed.mapCatching(
    (json) => apiResponseToTree(treeSchema.parse(json)),
    (err) => {
      console.error(err);
      return 'Server data in wrong format';
    }
  );
}
