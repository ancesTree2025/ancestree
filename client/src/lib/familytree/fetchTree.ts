import type { Marriages, People, PersonID, Tree } from './models';
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

const RETURN_SAMPLE_DATA = true;

const sampleData: ApiResponse = {
  root: {
    data: {
      id: '1',
      name: 'Henry VIII',
      gender: 'male'
    },
    id: '1',
    depth: 0
  },
  nodes: [
    {
      data: {
        id: '1',
        name: 'Henry VIII',
        gender: 'male'
      },
      id: '1',
      depth: 0
    },
    {
      data: {
        id: '2',
        name: 'Catherine of Aragon',
        gender: 'female'
      },
      id: '2',
      depth: 0
    },
    {
      data: {
        id: '3',
        name: 'Mary 1',
        gender: 'female'
      },
      id: '3',
      depth: 1
    },
    {
      data: {
        id: '4',
        name: 'Henry VII',
        gender: 'male'
      },
      id: '4',
      depth: -1
    },
    {
      data: {
        id: '5',
        name: 'Elizabeth of York',
        gender: 'female'
      },
      id: '5',
      depth: -1
    }
  ],
  edges: [
    {
      node1: '1',
      node2: '2'
    },
    {
      node1: '1',
      node2: '3'
    },
    {
      node1: '2',
      node2: '3'
    },
    {
      node1: '4',
      node2: '1'
    },
    {
      node1: '5',
      node2: '1'
    },
    {
      node1: '4',
      node2: '5'
    }
  ]
};

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

export async function fetchTree(name: string): Promise<Tree> {
  let json: object;
  if (RETURN_SAMPLE_DATA) {
    json = sampleData;
  } else {
    const response = await fetch(`http://localhost:8080/${name}`);
    json = await response.json();
  }
  return apiResponseToTree(apiResponseSchema.parse(json));
}
