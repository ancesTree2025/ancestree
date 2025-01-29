import type { Marriages, People, PersonID, Tree } from './models';
import exampleData from './exampleData.json';
import { z } from 'zod';

const personIdSchema = z.string();

const personSchema = z.object({
  data: z.object({
    id: personIdSchema,
    name: z.string(),
    gender: z.string()
  }),
  id: personIdSchema,
  depth: z.number()
});

const apiResponseSchema = z.object({
  root: personSchema,
  nodes: z.array(personSchema),
  edges: z.array(
    z.object({
      node1: personIdSchema,
      node2: personIdSchema
    })
  )
});

type ApiResponse = z.infer<typeof apiResponseSchema>;

export function apiResponseToTree(res: ApiResponse): Tree {
  // Create mappings from people ids to their info and depths
  const people: People = [];
  const depths: Map<PersonID, number> = new Map();

  for (const person of res.nodes) {
    people.push([person.id, { name: person.data.name }]);
    depths.set(person.id, person.depth);
  }

  // Create list of marriages, and mapping from parents to children

  const simpleMarriages: [PersonID, PersonID][] = [];
  const children = new Map<PersonID, PersonID[]>();

  for (const edge of res.edges) {
    const depth1 = depths.get(edge.node1)!;
    const depth2 = depths.get(edge.node2)!;

    // If two nodes have same depth then they are married

    if (depth1 === depth2) {
      simpleMarriages.push([edge.node1, edge.node2]);
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
      parents: parents,
      children: (children.get(parents[0]) ?? []).filter((child) =>
        (children.get(parents[1]) ?? []).includes(child)
      )
    });
  }

  return {
    focus: res.root.id,
    people,
    marriages
  };
}

export async function fetchTree(name: string, useFakeData: boolean): Promise<Tree> {
  let json: object;
  if (useFakeData) {
    json = exampleData;
  } else {
    const response = await fetch(`http://localhost:8080/${name}`);
    json = await response.json();
  }
  return apiResponseToTree(apiResponseSchema.parse(json));
}
