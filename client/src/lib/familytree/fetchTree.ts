import type { Tree } from './models';

export function fetchTree(_: string): Tree {
  // TODO: populate with request
  return {
    people: new Map([
      ['a', { name: 'Alice', x: 125, y: 100 }],
      ['b', { name: 'Bob', x: 275, y: 100 }],
      ['c', { name: 'Charlie', x: 50, y: 200 }],
      ['d', { name: 'David', x: 200, y: 200 }],
      ['e', { name: 'Eve', x: 350, y: 200 }]
    ]),
    marriages: [
      {
        parents: ['a', 'b'],
        children: ['c', 'd', 'e']
      }
    ]
  };
}
