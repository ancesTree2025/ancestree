import type { Tree } from './models';

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
