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
export const apiResponseToTree = (res: ApiResponse): Tree => {
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
    } else {
      marriages.push({ parents: [edge.node1, edge.node2], children: [] });
    }
  }
  for (const marriage of marriages) {
    marriage.children = children
      .get(marriage.parents[0])!
      .filter((child) => children.get(marriage.parents[1])!.includes(child));
  }
  return {
    people,
    marriages
  };
};

export function fetchTree(_: string): Tree {
  // TODO: populate with request
  return {
    people: new Map([
      ['a', { name: 'Alice', x: 0, y: 0 }],
      ['b', { name: 'Bob', x: 0, y: 0 }],
      ['c', { name: 'Charlie', x: 0, y: 0 }],
      ['d', { name: 'David', x: 0, y: 0 }],
      ['e', { name: 'Eve', x: 0, y: 0 }],
      ['f', { name: 'Felix', x: 0, y: 0 }],
      ['g', { name: 'Gerald', x: 0, y: 0 }],
      ['h', { name: 'Harry', x: 0, y: 0 }],
      ['i', { name: 'Imogen', x: 0, y: 0 }],
      ['j', { name: 'Jack', x: 0, y: 0 }],
      ['k', { name: 'Kieran', x: 0, y: 0 }],
      ['l', { name: 'Louis', x: 0, y: 0 }],
      ['m', { name: 'Maria', x: 0, y: 0 }],
      ['n', { name: 'Nadia', x: 0, y: 0 }],
      ['o', { name: 'Oliver', x: 0, y: 0 }],
      ['p', { name: 'Peter', x: 0, y: 0 }],
      ['q', { name: 'Quentin', x: 0, y: 0 }]
    ]),
    marriages: [
      {
        parents: ['a', 'b'],
        children: ['c', 'd', 'e']
      },
      {
        parents: ['c', 'f'],
        children: ['g']
      },
      {
        parents: ['d', 'k'],
        children: ['h', 'i', 'j']
      },
      {
        parents: ['l', 'm'],
        children: ['a']
      },
      {
        parents: ['n', 'o'],
        children: ['l']
      },
      {
        parents: ['p', 'q'],
        children: ['m']
      }
    ]
  };
}
