import { expect, test } from 'vitest';
import { balanceTree } from '../src/lib/familytree/balanceTree';
import { strictEqual } from 'assert';

test('handles basic family tree with one generation above and below', () => {
  expect(
    balanceTree(
      {
        focus: 'Focus',
        people: ['Focus', 'Wife', 'Child 1', 'Child 2', 'Child 3', 'Mother', 'Father'].map((id) => [
          id,
          { name: id }
        ]),
        marriages: [
          {
            parents: ['Focus', 'Wife'],
            children: ['Child 1', 'Child 2', 'Child 3']
          },
          {
            parents: ['Mother', 'Father'],
            children: ['Focus']
          }
        ]
      },
      [80, 0],
      160,
      120
    )[0]
  ).toStrictEqual({
    'Child 1': { x: 0, y: 120 },
    'Child 2': { x: 160, y: 120 },
    'Child 3': { x: 320, y: 120 },
    Focus: { x: 80, y: 0 },
    Wife: { x: 240, y: 0 },
    Mother: { x: 0, y: -120 },
    Father: { x: 160, y: -120 }
  });
});

test('handles a child having a spouse', () => {
  expect(
    balanceTree(
      {
        focus: 'Focus',
        people: ['Focus', 'Wife', 'Child 1', 'Child 2', 'Child 3', 'Daughter in Law'].map((id) => [
          id,
          { name: id }
        ]),
        marriages: [
          {
            parents: ['Focus', 'Wife'],
            children: ['Child 1', 'Child 2', 'Child 3']
          },
          {
            parents: ['Child 2', 'Daughter in Law'],
            children: []
          }
        ]
      },
      [160, 0],
      160,
      120
    )[0]
  ).toStrictEqual({
    'Child 1': { x: 0, y: 120 },
    'Child 2': { x: 160, y: 120 },
    'Daughter in Law': { x: 320, y: 120 },
    'Child 3': { x: 480, y: 120 },
    Focus: { x: 160, y: 0 },
    Wife: { x: 320, y: 0 }
  });
});
/*
test('handles two spouses', () => {
  expect(
    balanceTree(
      {
        focus: 'Focus',
        people: ['Focus', 'Wife 1', 'Wife 2', 'Child 1', 'Child 2'].map((id) => [id, { name: id }]),
        marriages: [
          {
            parents: ['Focus', 'Wife 1'],
            children: ['Child 1']
          },
          {
            parents: ['Focus', 'Wife 2'],
            children: ['Child 2']
          }
        ]
      },
      [0, 0],
      160,
      120
    )[0]
  ).toStrictEqual({
    'Focus': { x: 0, y: 0 },
    'Wife 1': { x: 160, y: 0 },
    'Wife 2': { x: 320, y: 0 },
    'Child 1': { x: 80, y: 120 },
    'Child 2': { x: 240, y: 120 }
  });
});
*/