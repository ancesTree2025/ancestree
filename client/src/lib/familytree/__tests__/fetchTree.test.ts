import { expect, test } from 'vitest';
import { apiResponseToTree } from '../fetchTree';
import type { People } from '../types';

// mock.module('$env/dynamic/public', () => {
//   return {
//     env: {
//       PUBLIC_API_BASE_URL: ''
//     }
//   }
// })

function basicPersonData(id: string, depth: number) {
  return {
    data: {
      id,
      name: id,
      gender: 'prefer not to say' as const
    },
    id,
    depth
  };
}

test('Handles two marriages with different children', () => {
  const resultingPeople: People = [];

  for (const { id } of [
    { id: 'Wife 1', depth: 1 },
    { id: 'Child 1', depth: 2 },
    { id: 'Wife 2', depth: 1 },
    { id: 'Child 2', depth: 2 },
    { id: 'Focus', depth: 1 }
  ]) {
    resultingPeople.push([id, { name: id }, '']);
  }

  const result = apiResponseToTree({
    root: basicPersonData('Focus', 1),
    nodes: [
      basicPersonData('Focus', 1),
      basicPersonData('Wife 1', 1),
      basicPersonData('Child 1', 2),
      basicPersonData('Wife 2', 1),
      basicPersonData('Child 2', 2)
    ],
    edges: [
      {
        node1: 'Focus',
        node2: 'Wife 1'
      },
      {
        node1: 'Focus',
        node2: 'Wife 2'
      },
      {
        node1: 'Focus',
        node2: 'Child 1'
      },
      {
        node1: 'Wife 1',
        node2: 'Child 1'
      },
      {
        node1: 'Focus',
        node2: 'Child 2'
      },
      {
        node1: 'Wife 2',
        node2: 'Child 2'
      }
    ]
  });

  expect(result.focus).toBe('Focus');
  expect(new Map(result.people.map(([id, name, ..._rest]) => [id, name]))).toStrictEqual(
    new Map(resultingPeople.map(([id, name, ..._rest]) => [id, name]))
  );
  expect(new Set(result.marriages)).toStrictEqual(
    new Set([
      { parents: ['Focus', 'Wife 1'], children: ['Child 1'] },
      { parents: ['Focus', 'Wife 2'], children: ['Child 2'] }
    ])
  );
});
