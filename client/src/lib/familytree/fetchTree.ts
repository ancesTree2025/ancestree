import type { Marriages, Person, PersonID, Tree } from './models';

type PersonData = {
  data: {
    id: PersonID;
    name: string;
    gender: 'male' | 'female' | 'prefer not to say';
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

function apiResponseToTree(res: ApiResponse): { focusId: PersonID; tree: Tree } {
  // Create map of Person to list of children
  // Create list of marriages
  // For each marriage make its children the intersection of the children
  // of both members
  const people: Map<PersonID, Person & { depth: number }> = new Map();
  const marriages: Marriages = [];
  for (const person of res.nodes.concat(res.root)) {
    people.set(person.id, { name: person.data.name, x: 0, y: 0, depth: person.depth });
  }
  const children = new Map<PersonID, PersonID[]>();
  for (const edge of res.edges) {
    // node 1: parent, node 2: child
    if (people.get(edge.node1)!.depth < people.get(edge.node2)!.depth) {
      if (!children.has(edge.node1)) {
        children.set(edge.node1, []);
      }
      children.get(edge.node1)?.push(edge.node2);
    } else if (people.get(edge.node1)!.depth > people.get(edge.node2)!.depth) {
      if (!children.has(edge.node2)) {
        children.set(edge.node2, []);
      }
      children.get(edge.node2)?.push(edge.node1);
    } else {
      marriages.push({ parents: [edge.node1, edge.node2], children: [] });
    }
  }
  for (const marriage of marriages) {
    marriage.children = (children.get(marriage.parents[0]) ?? []).filter((child) =>
      (children.get(marriage.parents[1]) ?? []).includes(child)
    );
  }
  return {
    focusId: res.root.id,
    tree: {
      people,
      marriages
    }
  };
}

export async function fetchTree(name: string): Promise<{ focusId: PersonID; tree: Tree }> {
  const response = await fetch(`http://localhost:8080/${name}`);
  const json = await response.json();
  return apiResponseToTree(json);
}
