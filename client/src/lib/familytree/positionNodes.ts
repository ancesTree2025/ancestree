import type {
  GroupAssignments,
  GroupID,
  Marriages,
  PersonID,
  Position,
  Positions,
  Tree,
  MarriagePositions
} from './types';
import sr from 'seedrandom';

const SEED = '1234';
let rand = sr.alea(SEED);

type MarriageHeights = number[];

type MarriageDistances = number[];

type MarriageOffsets = number[];

/**
 * Assigns a position to each node in the family tree.
 *
 * @param tree - The family tree
 * @returns An object containing the positions of the nodes and the width of the tree.
 */
export function positionNodes(tree: Tree): {
  positions: Positions;
  marriagePositions: MarriagePositions;
  treeWidth: number;
} {
  rand = sr.alea(SEED, { state: true });
  // for efficiency, keep mapping from person to marriages and from person to parents' marriage
  const personMarriages: Record<PersonID, Marriages> = Object.fromEntries(
    tree.people.map(([id]) => [id, tree.marriages.filter((m) => m.parents.includes(id))])
  );
  const personParents: Record<PersonID, Marriages> = Object.fromEntries(
    tree.people.map(([id]) => [id, tree.marriages.filter((m) => m.children.includes(id))])
  );

  const { groups, highestGroup } = getMarriageGroups(tree);

  const { depths, highestGroup: nextHighestGroup } = assignDepths(
    tree,
    groups,
    personMarriages,
    personParents,
    highestGroup
  );

  const { sortedLevels, positions: personPositions } = sortDepths(
    groups,
    depths,
    personMarriages,
    personParents
  );

  const marriageDistances = findMarriageDistances(tree, personPositions);

  const arrangedLevels = arrangeNodes(sortedLevels, groups, personMarriages, nextHighestGroup);

  const marriageOffsets = getMarriageOffsets(
    tree,
    groups,
    depths,
    arrangedLevels,
    marriageDistances
  );

  const marriageHeights = getMarriageHeights(tree, arrangedLevels);

  const { positions, treeWidth } = calculatePositions(arrangedLevels, tree.focus);

  const marriagePositions = getMarriagePositions(
    tree,
    positions,
    marriageHeights,
    marriageDistances,
    marriageOffsets
  );

  return {
    positions,
    treeWidth,
    marriagePositions
  };
}

/**
 * Arrange people into marriage groups from a given family tree.
 *
 * @param tree - The family tree
 * @returns The marriage group assignments
 */
function getMarriageGroups(tree: Tree): { groups: GroupAssignments; highestGroup: number } {
  const marriageGroups: Map<GroupID, PersonID[]> = new Map();
  const memberOf: Record<PersonID, GroupID> = {};

  // initialise groups as singletons
  let i = 0;
  for (const [id] of tree.people) {
    marriageGroups.set(i, [id]);
    memberOf[id] = i;
    i++;
  }

  // for each marriage, merge the groups of the parents
  for (const marriage of tree.marriages) {
    const parents = marriage.parents.filter(
      (p) => tree.people.filter(([id]) => id === p).length > 0
    );
    if (parents.length === 0) continue;
    const groupId = memberOf[parents[0]];
    let newGroup: PersonID[] = [];
    for (const person of parents) {
      const group = marriageGroups.get(memberOf[person]);
      if (group === undefined) continue;
      newGroup = newGroup.concat(group);
      marriageGroups.delete(memberOf[person]);
      for (const person of group) {
        memberOf[person] = groupId;
      }
    }
    marriageGroups.set(groupId, newGroup);
  }

  return {
    groups: {
      groups: marriageGroups,
      members: memberOf
    },
    highestGroup: i
  };
}

/**
 * Assigns a "depth" to each group
 * Each parent and child should be on a different depth. In particularly
 * incestuous cases the child might be more than one depth away from the parent.
 *
 * @param tree - The family tree
 * @param groups - The group assignments from getMarriageGroups
 * @param personMarriages - A record of marriages a person is a parent of
 * @param personParents - A record of marriages a person is a child of
 * @param highestGroup - TODO
 * @returns A record mapping each group ID to its assigned depth
 */
function assignDepths(
  tree: Tree,
  groups: GroupAssignments,
  personMarriages: Record<PersonID, Marriages>,
  personParents: Record<PersonID, Marriages>,
  highestGroup: number
): { depths: Map<GroupID, number>; highestGroup: number } {
  const depths: Map<GroupID, number> = new Map();
  const minDepths: Map<GroupID, number> = new Map();
  const maxDepths: Map<GroupID, number> = new Map();

  // by default no groups have depths
  const unfound: Set<GroupID> = new Set(groups.groups.keys());

  // give the focused node's group a min and max depth of zero
  const focusGroupId = groups.members[tree.focus];
  maxDepths.set(focusGroupId, 0);
  minDepths.set(focusGroupId, 0);

  while (unfound.size > 0) {
    // find the group ID with the lowest minimum depth, or highest maximum depth
    // this is needed for example when a grandchild has a minimum depth of 1 and
    // is assigned a depth of 1 before the child, meaning the child's depth would
    // be illegal
    let minimum = Infinity;
    let groupId: GroupID | null = null;
    for (const group of unfound) {
      const maximumDepth = maxDepths.get(group) ?? -Infinity;
      const minimumDepth = minDepths.get(group) ?? Infinity;
      const depth = Math.min(-maximumDepth, minimumDepth);
      if (depth < minimum) {
        minimum = depth;
        groupId = group;
      }
    }
    if (groupId == null) break;
    unfound.delete(groupId);

    const group = groups.groups.get(groupId)!;

    // assign smallest legal depth
    const minDepth = minDepths.get(groupId) ?? -Infinity;
    const maxDepth = maxDepths.get(groupId) ?? Infinity;
    let depth = 0;
    if (maxDepth === Infinity && minDepth !== -Infinity) {
      depth = minDepth;
    } else if (minDepth === -Infinity && maxDepth !== Infinity) {
      depth = maxDepth;
    } else if (minDepth > 0) {
      depth = minDepth;
    } else if (maxDepth < 0) {
      depth = maxDepth;
    }

    depths.set(groupId, depth);

    for (const person of group) {
      // update the max depth of all the parents to be lower than this group's depth
      for (const marriage of personParents[person]) {
        const parents = marriage.parents;
        for (const parent of parents) {
          const parentGroupId = groups.members[parent];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(parentGroupId)) continue;

          maxDepths.set(
            parentGroupId,
            Math.min(maxDepths.get(parentGroupId) ?? Infinity, depth - 1)
          );
        }
      }
      // update the min depth of all the children to be higher than this group's depth
      for (const marriage of personMarriages[person]) {
        const children = marriage.children;
        for (const child of children) {
          const childGroupId = groups.members[child];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(childGroupId)) continue;

          minDepths.set(
            childGroupId,
            Math.max(minDepths.get(childGroupId) ?? -Infinity, depth + 1)
          );
        }
      }
    }
  }

  // find any people who have a parent > 1 depth away and make a fake group for them
  const groupIds = groups.groups.keys();
  for (const groupId of groupIds) {
    const group = groups.groups.get(groupId)!;
    const depth = depths.get(groupId)!;
    for (const person of group) {
      // parents should all be on same depth
      const parents = personParents[person].flatMap((m) => m.parents);
      const parentDepth = depths.get(groups.members[parents[0]])!;
      for (let d = parentDepth + 1; d < depth; d++) {
        const fakeGroupId = highestGroup++;
        groups.groups.set(fakeGroupId, [person]);
        groups.members[person] = fakeGroupId;
        depths.set(fakeGroupId, d);
      }
    }
  }

  return { depths, highestGroup };
}

/**
 * Sorts each marriage group within a depth, and returns the ordering of all nodes within that depth.
 *
 * @param groups - The group assignments from getMarriageGroups
 * @param depths - The depth assignments from assignDepths
 * @param personMarriages - A record of marriages a person is a parent of
 * @param personParents - A record of marriages a person is a child of
 * @returns A map from depth to sorted list of person IDs.
 */
function sortDepths(
  groups: GroupAssignments,
  depths: Map<GroupID, number>,
  personMarriages: Record<PersonID, Marriages>,
  personParents: Record<PersonID, Marriages>
): { sortedLevels: Map<number, PersonID[]>; positions: Map<PersonID, number> } {
  // process the depths from bottom to top
  const depthNumbers: number[] = Array.from(new Set(depths.values())).sort((a, b) => b - a);
  const outputDepths: Map<number, PersonID[]> = new Map();

  const positions = new Map<PersonID, number>();

  let lastAssignment: PersonID[] | null = null;
  for (const depth of depthNumbers) {
    // representation of arbitrary "closeness" of two groups
    const weights = new Map<GroupID, Map<GroupID, number>>();

    // get all the groups at this depth
    const marriageGroups = depths
      .entries()
      .toArray()
      .filter(([, d]) => d === depth)
      .map(([id]) => id);

    // If children of A come before children of B, then A should come before B
    // This finds the average position of the group of each child of the nodes in each group,
    // and then sorts the average positions of each group to produce a list of contraints,
    // i.e. what nodes need to come before which, in this depth
    // Also do the same for people when we come to sort the nodes in the marriage groups themselves
    // This is the sort of thing that could really do with a diagram icl BUT IT WORKS :)
    const constraints: [GroupID, GroupID][] = [];
    const peopleConstraints: [PersonID, PersonID][] = [];
    if (lastAssignment !== null) {
      const positions: Map<GroupID, number> = new Map();
      const peoplePositions: Map<PersonID, number> = new Map();

      // compute "average position" of each group
      const positionTotal: Map<GroupID, number> = new Map();
      const positionCount: Map<GroupID, number> = new Map();
      const peoplePositionTotal: Map<PersonID, number> = new Map();
      const peoplePositionCount: Map<PersonID, number> = new Map();
      for (const groupId of marriageGroups) {
        const group = groups.groups.get(groupId)!;
        for (const person of group) {
          for (const marriage of personMarriages[person]) {
            for (const child of marriage.children) {
              const position = lastAssignment.indexOf(child);
              if (position === -1) continue;
              positionTotal.set(groupId, (positionTotal.get(groupId) ?? 0) + position);
              positionCount.set(groupId, (positionCount.get(groupId) ?? 0) + 1);
              peoplePositionTotal.set(person, (peoplePositionTotal.get(person) ?? 0) + position);
              peoplePositionCount.set(person, (peoplePositionCount.get(person) ?? 0) + 1);
            }
          }
        }
      }
      for (const [groupId, count] of positionCount.entries()) {
        const total = positionTotal.get(groupId) ?? 0;
        const average = total / count;
        positions.set(groupId, average);
      }

      const sortedPositions: GroupID[] = Array.from(positions.entries())
        .sort((a, b) => a[1] - b[1])
        .map(([a]) => a);

      for (let i = 0; i < sortedPositions.length - 1; i++) {
        constraints.push([sortedPositions[i], sortedPositions[i + 1]]);
      }

      for (const [person, count] of peoplePositionCount.entries()) {
        const total = peoplePositionTotal.get(person) ?? 0;
        const average = total / count;
        peoplePositions.set(person, average);
      }

      const sortedPeoplePositions: PersonID[] = Array.from(peoplePositions.entries())
        .sort((a, b) => a[1] - b[1])
        .map(([a]) => a);

      for (let i = 0; i < sortedPeoplePositions.length - 1; i++) {
        peopleConstraints.push([sortedPeoplePositions[i], sortedPeoplePositions[i + 1]]);
      }
    }

    // calculate weights between each pair of nodes by BFS
    for (const fromGroup of marriageGroups) {
      const from = groups.groups.get(fromGroup)![0];
      const map = new Map<GroupID, number>();
      // TODO: number of function calls can be halved
      for (const toGroup of marriageGroups) {
        const to = groups.groups.get(toGroup)![0];
        map.set(toGroup, weightBetween(from, to, personMarriages, personParents));
      }
      weights.set(fromGroup, map);
    }

    const minimumGroupsPermutation = sortByWeights(marriageGroups, weights, constraints);

    // sort within each marriage group
    const minimumPermutation: PersonID[] = minimumGroupsPermutation.flatMap((id) => {
      const weights: Map<PersonID, Map<PersonID, number>> = new Map();

      // weights are 0 if not married, 1 if married
      const group = groups.groups.get(id)!;
      for (const person of group) {
        const map = new Map<PersonID, number>();
        for (const marriage of personMarriages[person]) {
          for (const parent of marriage.parents) {
            map.set(parent, 1);
          }
        }
        weights.set(person, map);
      }
      return sortByWeights(group, weights, peopleConstraints);
    });

    lastAssignment = minimumPermutation;

    outputDepths.set(depth, minimumPermutation);
  }

  for (const depth of outputDepths.values()) {
    for (const [i, person] of depth.entries()) {
      positions.set(person, i);
    }
  }

  return { sortedLevels: outputDepths, positions };
}

const UNORDERED_WEIGHT = 2;

/**
 * Sorts an array of elements with respect to weights and constraints.
 * Minimizes sum of weight * distance^2 between all pairs of nodes in resulting in array
 * This needs rewriting
 * @template T - The type of elements in the array.
 * @param {T[]} elements - The array of elements to be sorted.
 * @param {Map<T, Map<T, number>>} weights - A map of "from" node to a map of "to" nodes to weights. A smaller weight indicates the nodes should be closer closer.
 * @param {[T, T][]} constraints - An array of constraints where each constraint is a tuple [a, b] indicating
 *                                 that element `a` should come before element `b`. (soft constraint)
 * @returns {T[]} - The sorted array of elements that minimizes the total weight and respects the constraints.
 */
function sortByWeights<T>(
  elements: T[],
  weights: Map<T, Map<T, number>>,
  constraints: [T, T][]
): T[] {
  function randomNumber(start: number, end: number) {
    return start + Math.floor(rand.double() * (1 + end - start));
  }

  function mutate(solution: T[]): T[] {
    const result = [...solution];
    const i = randomNumber(0, solution.length - 1);
    let j = randomNumber(0, solution.length - 2);
    if (i === j) {
      j++;
    }
    [result[i], result[j]] = [result[j], result[i]];
    return result;
  }

  function crossover(solution1: T[], solution2: T[]): T[] {
    const i = randomNumber(0, solution1.length - 1);
    const result1: T[] = [];
    for (let j = 0; j <= i; j++) {
      result1.push(solution1[j]);
    }
    const result2 = solution2.filter((x) => !result1.includes(x));
    Array.prototype.push.apply(result1, result2);
    return result1;
  }

  function evaluate(solution: T[]) {
    let total = 0;
    for (const [a, b] of constraints) {
      const aPos = solution.indexOf(a);
      const bPos = solution.indexOf(b);
      if (aPos !== -1 && bPos !== -1 && aPos > bPos) {
        total += UNORDERED_WEIGHT;
      }
    }

    for (let x = 0; x < solution.length - 1; x++) {
      for (let y = x + 1; y < solution.length; y++) {
        // score is weight times (distance squared)
        const w = weights.get(solution[x])?.get(solution[y]) ?? 0;
        const d = y - x;
        total += w * d * d;
      }
    }
    return total;
  }

  function geneticAlgorithm() {
    const GENERATION_SIZE = 8;
    let solutions = generateRandomPermutations(elements, GENERATION_SIZE);
    for (let epoch = 0; epoch < 1000; epoch++) {
      const parents = [];
      while (solutions.length > 1) {
        const elem1 = solutions.splice(randomNumber(0, solutions.length - 1), 1)[0];
        const elem2 = solutions.splice(randomNumber(0, solutions.length - 1), 1)[0];
        if (evaluate(elem1) < evaluate(elem2)) {
          parents.push(elem1);
        } else {
          parents.push(elem2);
        }
      }
      solutions = [...parents];
      for (let i = 0; i < parents.length; i += 1) {
        const parent1 = parents[i];
        const parent2 = parents[(i + 1) % parents.length];
        solutions.push(mutate(crossover(parent1, parent2)));
      }
    }
    return solutions;
  }

  let minimum = Infinity;
  let minimumPermutation: T[] = [];

  const perms = elements.length > 1 ? geneticAlgorithm() : permutations(elements);
  for (const permutation of perms) {
    // if it violates any constraints then increase total
    const total = evaluate(permutation);
    if (total < minimum) {
      minimum = total;
      minimumPermutation = permutation;
    }
  }

  return minimumPermutation;
}

function factorial(n: number): number {
  let result = 1;
  for (let i = 2; i <= n; i++) {
    result *= i;
  }
  return result;
}

const MAX_PERMUTATIONS = 100000;

function permutations<T>(xs: T[]): T[][] {
  if (factorial(xs.length) > MAX_PERMUTATIONS) {
    return generateRandomPermutations(xs, MAX_PERMUTATIONS);
  }
  if (xs.length === 0) return [[]];
  const result: T[][] = [];
  for (let i = 0; i < xs.length; i++) {
    const rest = xs.slice(0, i).concat(xs.slice(i + 1));
    const restPermutations = permutations(rest);
    for (const perm of restPermutations) {
      result.push([xs[i], ...perm]);
    }
  }
  return result;
}

function generateRandomPermutations<T>(elements: T[], count: number): T[][] {
  const permutations: T[][] = [];
  for (let i = 0; i < count; i++) {
    const shuffled = [...elements];
    for (let j = 0; j < elements.length; j++) {
      const k = Math.floor(rand.double() * elements.length);
      [shuffled[j], shuffled[k]] = [shuffled[k], shuffled[j]];
    }
    permutations.push(shuffled);
  }
  return permutations;
}

/**
 * Finds some arbitrary weight between two nodes, by the strength of their relationship
 *
 * @param from - The ID of the first person
 * @param to - The ID of the second person
 * @param personMarriages - A record of marriages a person is a parent of
 * @param personParents - A record of marriages a person is a child of
 * @returns The calcualted weight
 */
function weightBetween(
  from: PersonID,
  to: PersonID,
  personMarriages: Record<PersonID, Marriages>,
  personParents: Record<PersonID, Marriages>
): number {
  const queue = [from];

  const distances = new Map<PersonID, number>();
  distances.set(from, 0);

  while (queue.length > 0) {
    const current = queue.shift()!;
    const d = distances.get(current)!;
    if (current === to) {
      return 1 / d;
    }

    const personMarriage = personMarriages[current];
    if (personMarriage === undefined) continue;

    for (const marriage of personMarriage) {
      for (const spouse of marriage.parents) {
        if (!distances.has(spouse)) {
          distances.set(spouse, d);
          queue.unshift(spouse);
        }
      }
      for (const child of marriage.children) {
        if (!distances.has(child)) {
          distances.set(child, d + 1);
          queue.push(child);
        }
      }
    }

    const personParent = personParents[current];
    if (personParent === undefined) continue;

    for (const marriage of personParent) {
      for (const parent of marriage.parents) {
        if (!distances.has(parent)) {
          distances.set(parent, d + 1);
          queue.push(parent);
        }
      }
      for (const sibling of marriage.children) {
        if (!distances.has(sibling)) {
          distances.set(sibling, d + 1);
          queue.push(sibling);
        }
      }
    }
  }

  return 0;
}

function arrangeNodes(
  depths: Map<number, PersonID[]>,
  groups: GroupAssignments,
  personMarriages: Record<PersonID, Marriages>,
  highestGroup: number
): Map<number, Map<PersonID, number>> {
  const [baseDepth, base] = [...depths.entries()].reduce((a, b) =>
    a[1].length > b[1].length ? a : b
  );

  const assignments: Map<number, Map<PersonID, number>> = new Map();
  const baseAssignment = new Map<PersonID, number>();
  for (let i = 0; i < base.length; i++) {
    baseAssignment.set(base[i], i * 2);
  }
  assignments.set(baseDepth, baseAssignment);

  // handle positions of parent layer
  let depth = baseDepth - 1;
  for (;;) {
    const nodes = depths.get(depth);
    if (nodes === undefined) break;

    const groupLeft: Map<GroupID, number> = new Map();
    const groupRight: Map<GroupID, number> = new Map();

    const visitedGroups = new Set<GroupID>();

    let lastRight = -2;

    // for each parent group, find average position of children in layer below,
    // and find left + right bounds of each parent group
    for (const node of nodes) {
      const groupId = groups.members[node];
      if (visitedGroups.has(groupId)) continue;
      visitedGroups.add(groupId);

      const group = groups.groups.get(groupId)!;
      let childrenLeft = Infinity;
      let childrenRight = -Infinity;
      for (const node of group) {
        const children = personMarriages[node].flatMap((m) => m.children);
        for (const child of children) {
          const pos = assignments.get(depth + 1)?.get(child);
          if (pos === undefined) continue;
          childrenLeft = Math.min(childrenLeft, pos);
          childrenRight = Math.max(childrenRight, pos);
        }
      }
      if (childrenLeft === Infinity) {
        childrenLeft = lastRight + 2;
        childrenRight = lastRight + 2;
      }
      const mid = (childrenLeft + childrenRight) / 2;
      const left = mid - (group.length - 1);
      const right = mid + (group.length - 1);
      groupLeft.set(groupId, left);
      groupRight.set(groupId, right);
      lastRight = right;
    }

    // TODO: prevent overlap of groups

    // allocate positions to each node by group position
    const groupCounts = new Map<GroupID, number>();
    const assignment = new Map<PersonID, number>();

    for (const node of nodes) {
      const groupId = groups.members[node];
      const left = groupLeft.get(groupId)!;
      const count = groupCounts.get(groupId) ?? 0;
      const pos = left + count * 2;
      groupCounts.set(groupId, count + 1);
      assignment.set(node, pos);
    }

    shuntOverlapping(nodes, assignment);
    assignments.set(depth, assignment);
    depth--;
  }

  const handled = new Set<PersonID>();
  const handledIds = new Set<GroupID>();
  const parentGroupIds = new Set(base.map((id) => groups.members[id]));

  // handles positions of child layer
  depth = baseDepth + 1;
  for (;;) {
    const nodes = depths.get(depth);
    if (nodes === undefined) break;

    const childrenGroupLeft: Map<GroupID, number> = new Map();
    const childrenGroupRight: Map<GroupID, number> = new Map();
    const childrenGroups: Map<GroupID, PersonID[]> = new Map();
    const childrenMember: Map<PersonID, GroupID[]> = new Map();

    // for each parent group, find average position of parents in layer above,
    // and find left + right bounds of children of each parent group
    for (const parentGroupId of parentGroupIds) {
      if (handledIds.has(parentGroupId)) continue;
      const parentGroup = groups.groups.get(parentGroupId)!;
      let parentsLeft = Infinity;
      let parentsRight = -Infinity;
      for (const parent of parentGroup) {
        const pos = assignments.get(depth - 1)?.get(parent);
        if (pos === undefined) continue;
        parentsLeft = Math.min(parentsLeft, pos);
        parentsRight = Math.max(parentsRight, pos);
      }

      // We identify a "children group" as the set of children (and their spouses)
      // of a certain parent group and refer to them by parent marriage group ID
      const childrenGroup = [
        ...new Set(
          parentGroup.flatMap((id) =>
            personMarriages[id]
              .flatMap((m) => m.children)
              .flatMap((c) => groups.groups.get(groups.members[c])!)
          )
        )
      ].filter((id) => {
        // if a node is more than 1 depth away from its parent, and we have already
        // assigned its position, do not include in this children group
        if (handled.has(id)) {
          const groupId = highestGroup++;
          childrenMember.set(id, [...(childrenMember.get(id) ?? []), groupId]);
          childrenGroups.set(groupId, [id]);

          const pos = assignments.get(depth - 1)!.get(id)!;
          childrenGroupLeft.set(groupId, pos);
          childrenGroupRight.set(groupId, pos);
          return false;
        }
        return true;
      });

      childrenGroup.forEach((id) =>
        childrenMember.set(id, [...(childrenMember.get(id) ?? []), parentGroupId])
      );
      childrenGroups.set(parentGroupId, childrenGroup);

      const mid = (parentsLeft + parentsRight) / 2;
      const left = mid - (childrenGroup.length - 1);
      const right = mid + (childrenGroup.length - 1);
      childrenGroupLeft.set(parentGroupId, left);
      childrenGroupRight.set(parentGroupId, right);

      handledIds.add(parentGroupId);
    }

    // TODO: prevent overlap of groups

    const groupCounts = new Map<GroupID, number>();
    const arrangement = new Map<PersonID, number>();

    handled.clear();
    parentGroupIds.clear();

    // allocate positions to each node by group position
    for (const node of nodes) {
      const groupId = childrenMember
        .get(node)!
        .sort((a, b) => childrenGroups.get(b)!.length - childrenGroups.get(a)!.length)[0];
      const count = groupCounts.get(groupId) ?? 0;

      const left = childrenGroupLeft.get(groupId)!;
      const pos = left + count * 2;
      groupCounts.set(groupId, count + 1);
      arrangement.set(node, pos);

      handled.add(node);
      parentGroupIds.add(groups.members[node]);
    }

    shuntOverlapping(nodes, arrangement);
    assignments.set(depth, arrangement);
    depth++;
  }

  return assignments;
}

function shuntOverlapping(level: PersonID[], assignments: Map<PersonID, number>) {
  let iters = 1000;
  let happy = false;
  while (!happy) {
    iters--;
    if (iters === 0) return;
    happy = true;

    for (let i = 0; i < level.length - 1; i++) {
      const index = i % 2 === 0 ? i / 2 : level.length - 2 - (i - 1) / 2;
      const person1 = level[index];
      const pos1 = assignments.get(person1)!;

      const person2 = level[index + 1];
      const pos2 = assignments.get(person2)!;
      if (pos2 - pos1 < 2) {
        happy = false;
        assignments.set(person1, pos1 - 0.5);
        assignments.set(person2, pos2 + 0.5);
      }
    }
  }
}

const NODE_WIDTH = 100;
const NODE_HEIGHT = 200;

function calculatePositions(
  levels: Map<number, Map<PersonID, number>>,
  focus: PersonID
): {
  positions: Positions;
  treeWidth: number;
} {
  let minX = Infinity;
  let maxX = -Infinity;
  let shiftX = 0;
  let shiftY = 0;
  const positions: Positions = {};

  for (const [depth, arrangement] of levels) {
    for (const [person, x] of arrangement) {
      const actualX = x * NODE_WIDTH;
      minX = Math.min(minX, actualX);
      maxX = Math.max(maxX, actualX);
      positions[person] = { x: actualX, y: depth * NODE_HEIGHT };
      if (person === focus) {
        shiftX = actualX;
        shiftY = depth * NODE_HEIGHT;
      }
    }
  }

  for (const id in positions) {
    positions[id].x -= shiftX;
    positions[id].y -= shiftY;
  }

  return { positions, treeWidth: maxX - minX };
}

function getMarriageHeights(
  tree: Tree,
  arrangements: Map<number, Map<PersonID, number>>
): MarriageHeights {
  const lefts: number[] = [];
  const mids: number[] = [];
  const rights: number[] = [];
  const hasChildren: boolean[] = [];
  const offsets: MarriageHeights = [];
  const depthIndices = new Map<number, number[]>();

  const xs = new Map<PersonID, number>();
  const depths = new Map<PersonID, number>();
  for (const [depth, arrangement] of arrangements) {
    for (const [person, x] of arrangement) {
      depths.set(person, depth);
      xs.set(person, x);
    }
  }

  let i = 0;
  for (const marriage of tree.marriages) {
    let mid = 0;
    for (const parent of marriage.parents) {
      mid += xs.get(parent)!;
    }
    mid /= marriage.parents.length;
    mids.push(mid);

    let left = mid;
    let right = mid;
    for (const child of marriage.children) {
      const x = xs.get(child)!;
      left = Math.min(left, x);
      right = Math.max(right, x);
    }
    lefts.push(left);
    rights.push(right);

    hasChildren.push(marriage.children.length > 0);

    offsets.push(0);

    const depth = depths.get(marriage.parents[0])!;

    depthIndices.set(depth, [...(depthIndices.get(depth) ?? []), i]);
    i++;
  }

  for (const indices of depthIndices.values()) {
    let highest = 0;
    for (let i = 0; i < indices.length; i++) {
      const first = indices[i];
      for (let j = 0; j < indices.length; j++) {
        if (i === j) continue;
        const second = indices[j];

        if (!hasChildren[first] || !hasChildren[second]) continue;

        const cond1 =
          lefts[first] <= lefts[second] &&
          lefts[second] <= rights[first] &&
          rights[first] < mids[second];
        const cond2 =
          mids[second] < lefts[first] &&
          lefts[first] <= rights[second] &&
          rights[second] <= rights[first];
        const cond3 = lefts[first] <= lefts[second] && rights[second] <= rights[first];

        if (offsets[first] === offsets[second] && (cond1 || cond2 || cond3)) {
          offsets[first] = Math.max(offsets[first] + 1);
          highest = Math.max(highest, offsets[first]);
        }
      }
    }

    for (let i = 0; i < indices.length; i++) {
      const first = indices[i];
      let hasConflict = false;
      const levels = new Set([offsets[first]]);
      for (let j = 0; j < indices.length; j++) {
        if (i === j) continue;
        const second = indices[j];

        if (!hasChildren[first] || !hasChildren[second]) continue;

        if (rights[first] >= lefts[second] && lefts[first] <= rights[second]) {
          levels.add(offsets[second]);
          if (offsets[first] === offsets[second]) {
            hasConflict = true;
          }
        }
      }
      if (hasConflict) {
        for (;;) {
          if (!levels.has(offsets[first])) break;
          offsets[first]++;
          highest = Math.max(highest, offsets[first]);
        }
      }
    }

    for (let i = 0; i < indices.length; i++) {
      offsets[indices[i]] -= highest / 2;
    }
  }

  return offsets;
}

function findMarriageDistances(tree: Tree, positions: Map<PersonID, number>): MarriageDistances {
  const distances: MarriageDistances = [];
  for (const marriage of tree.marriages) {
    let min = Infinity;
    let max = -Infinity;
    for (const parent of marriage.parents) {
      const pos = positions.get(parent)!;
      min = Math.min(min, pos);
      max = Math.max(max, pos);
    }
    const distance = max - min;
    distances.push(distance);
  }
  return distances;
}

function getMarriageOffsets(
  tree: Tree,
  groups: GroupAssignments,
  depths: Map<GroupID, number>,
  positions: Map<number, Map<PersonID, number>>,
  distances: MarriageDistances
): MarriageOffsets {
  const bounds: Map<number, Map<number, [number, number][]>> = new Map();
  let i = 0;
  for (const marriage of tree.marriages) {
    const groupId = groups.members[marriage.parents[0]];
    if (groupId === undefined) {
      i++;
      continue;
    }
    const depth = depths.get(groupId);
    if (depth === undefined) {
      i++;
      continue;
    }
    const distance = distances[i];
    const layer = positions.get(depth)!;
    let left = Infinity;
    let right = -Infinity;
    for (const person of marriage.parents) {
      const pos = layer.get(person)!;
      left = Math.min(left, pos);
      right = Math.max(right, pos);
    }
    if (!bounds.has(depth)) bounds.set(depth, new Map());
    const depthBound = bounds.get(depth)!;
    if (!depthBound.has(distance)) depthBound.set(distance, []);
    depthBound.get(distance)!.push([left, right]);
    i++;
  }

  const offsets = [];
  i = 0;
  for (const marriage of tree.marriages) {
    const distance = distances[i];
    const groupId = groups.members[marriage.parents[0]];
    if (groupId === undefined) {
      offsets.push(0);
      i++;
      continue;
    }
    const depth = depths.get(groupId);
    if (depth === undefined) {
      i++;
      continue;
    }
    const layer = positions.get(depth)!;
    const leftParent = Math.min(...marriage.parents.map((p) => layer.get(p) ?? Infinity));
    const rightParent = Math.max(...marriage.parents.map((p) => layer.get(p) ?? -Infinity));
    const mid = (leftParent + rightParent) / 2;

    let happy = false;
    let left = mid;
    while (!happy) {
      happy = true;
      for (let d = 1; d < distance; d++) {
        const depthBounds = bounds.get(depth)!;

        const minMaxes = depthBounds.get(d)!;

        for (const [min, max] of minMaxes) {
          if (max < leftParent) continue;
          if (min <= left && left <= max) {
            happy = false;
            left = min - 1;
          }
        }
      }
    }

    happy = false;
    let right = mid;
    while (!happy) {
      happy = true;
      for (let d = 1; d < distance; d++) {
        for (const [min, max] of bounds.get(depth)!.get(d)!) {
          if (min < leftParent) continue;
          if (min <= right && right <= max) {
            happy = false;
            right = max + 1;
          }
        }
      }
    }

    if (left <= leftParent && right >= rightParent) {
      offsets.push(0);
    } else if (
      mid - (left <= leftParent ? -Infinity : left) <
      (right >= rightParent ? Infinity : right) - mid
    ) {
      offsets.push((left - mid) * NODE_WIDTH);
    } else {
      offsets.push((right - mid) * NODE_WIDTH);
    }
    i++;
  }
  return offsets;
}

function getMarriagePositions(
  tree: Tree,
  positions: Positions,
  heights: MarriageHeights,
  distances: MarriageDistances,
  offsets: MarriageOffsets
): MarriagePositions {
  const output: MarriagePositions = [];
  let i = -1;
  for (const marriage of tree.marriages) {
    i++;
    const height = heights[i];
    const distance = distances[i];
    const offset = offsets[i];

    const parents = marriage.parents.map((p) => positions[p] as Position | undefined);
    const children = [];
    const childrenIDs = [];

    for (const child of marriage.children) {
      const pos = positions[child];
      if (pos === undefined) continue;
      children.push(pos);
      childrenIDs.push(child);
    }

    const parent1 = parents[0];
    const parent2 = parents[1];
    if (parent1 === undefined || parent2 === undefined) continue;

    output.push({
      parent1,
      parent2,
      children,
      height,
      distance,
      offset,
      parent1ID: marriage.parents[0],
      parent2ID: marriage.parents[1],
      childrenIDs,
      type: marriage.type
    });
  }

  return output;
}
