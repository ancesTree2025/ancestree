import type { Marriages, People, PersonID, Tree } from './models';

type PersonData = {
  data: {
    id: PersonID;
    name: string;
    gender: string;
  };
  id: PersonID;
  depth: number;
};

type ApiResponse = {
  root: PersonData;
  nodes: PersonData[];
  edges: {
    node1: PersonID;
    node2: PersonID;
  }[];
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
  const response = await fetch(`http://localhost:8080/${name}`);
  const json = await response.json();
  return apiResponseToTree(json);
}
