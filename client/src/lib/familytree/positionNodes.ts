import type { Marriages, PersonID, Positions, Tree } from './types';

/**
 * Assigns a position to each node in the family tree.
 *
 * @param tree - The family tree
 * @returns An object containing the positions of the nodes and the width of the tree.
 */
export function positionNodes(tree: Tree): {
  positions: Positions;
  treeWidth: number;
} {
  // for efficiency, keep mapping from person to marriages and from person to parents' marriage
  const personMarriages: Record<PersonID, Marriages> = Object.fromEntries(
    tree.people.map(([id]) => [id, tree.marriages.filter((m) => m.parents.includes(id))])
  );
  const personParents: Record<PersonID, Marriages> = Object.fromEntries(
    tree.people.map(([id]) => [id, tree.marriages.filter((m) => m.children.includes(id))])
  );

  const marriageGroups = getMarriageGroups(tree);

  const depths = assignDepths(tree, marriageGroups, personMarriages, personParents);

  const sortedLevels = sortDepths(marriageGroups, depths, personMarriages, personParents);
  console.log(sortedLevels);

  // TODO: finish this
  return { positions: {}, treeWidth: 0 };
}

/**
 * Represents the assignments of people to "marriage groups" within a family tree.
 *
 * @typedef {Object} GroupAssigments
 *
 * @property {Map<GroupID, PersonID[]>} groups - A mapping from group ID to the members of that group.
 * @property {Record<PersonID, GroupID>} members - A mapping from person ID to the group they are in.
 */
type GroupAssigments = {
  groups: Map<GroupID, PersonID[]>;
  members: Record<PersonID, GroupID>;
};
type GroupID = PersonID;

/**
 * Arrange people into marriage groups from a given family tree.
 *
 * @param tree - The family tree
 * @returns The marriage group assignments
 */
function getMarriageGroups(tree: Tree): GroupAssigments {
  const marriageGroups: Map<GroupID, PersonID[]> = new Map();
  const memberOf: Record<PersonID, GroupID> = {};

  // initialise groups as singletons
  for (const [id] of tree.people) {
    marriageGroups.set(id, [id]);
    memberOf[id] = id;
  }

  // for each marriage, merge the groups of the parents
  // TODO: optimise the order of merging (multiple spouses may be put on all one side)
  for (const marriage of tree.marriages) {
    const groupId = memberOf[marriage.parents[0]];
    let newGroup: PersonID[] = [];
    for (const person of marriage.parents) {
      const group = marriageGroups.get(memberOf[person])!;
      newGroup = newGroup.concat(group);
      marriageGroups.delete(memberOf[person]);
      memberOf[person] = groupId;
    }
    marriageGroups.set(groupId, newGroup);
  }

  return {
    groups: marriageGroups,
    members: memberOf
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
 * @returns A record mapping each group ID to its assigned depth
 */
function assignDepths(
  tree: Tree,
  groups: GroupAssigments,
  personMarriages: Record<PersonID, Marriages>,
  personParents: Record<PersonID, Marriages>
): Map<GroupID, number> {
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
    let groupId: GroupID;
    for (const group of unfound) {
      const maximumDepth = maxDepths.get(group) ?? -Infinity;
      const minimumDepth = minDepths.get(group) ?? Infinity;
      const depth = Math.min(-maximumDepth, minimumDepth);
      if (depth < minimum) {
        minimum = depth;
        groupId = group;
      }
    }
    groupId = groupId!;
    unfound.delete(groupId);

    const group = groups.groups.get(groupId)!;

    // assign smallest legal depth
    const minDepth = minDepths.get(groupId) ?? -Infinity;
    const maxDepth = maxDepths.get(groupId) ?? Infinity;
    let depth = 0;
    if (minDepth > 0) {
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
          const groupId = groups.members[parent];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(groupId)) continue;

          maxDepths.set(groupId, Math.min(maxDepths.get(groupId) ?? Infinity, depth - 1));
        }
      }
      // update the min depth of all the children to be higher than this group's depth
      for (const marriage of personMarriages[person]) {
        const children = marriage.children;
        for (const child of children) {
          const groupId = groups.members[child];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(groupId)) continue;

          minDepths.set(groupId, Math.max(minDepths.get(groupId) ?? -Infinity, depth + 1));
        }
      }
    }
  }

  return depths;
}

function sortDepths(
  groups: GroupAssigments,
  depths: Map<GroupID, number>,
  personMarriages: Record<PersonID, Marriages>,
  personParents: Record<PersonID, Marriages>
): Map<number, GroupID[]> {
  const depthNumbers: number[] = Array.from(new Set(depths.values())).sort((a, b) => b - a);
  const outputDepths: Map<number, GroupID[]> = new Map();

  let lastAssignment: GroupID[] | null = null;
  for (const depth of depthNumbers) {
    const distances = new Map<GroupID, Map<GroupID, number>>();
    const marriageGroups = depths
      .entries()
      .toArray()
      .filter(([, d]) => d === depth)
      .map(([id]) => id);

    // If children of A come before children of B, then A should come before B
    // This finds the average position of the group of each child of the nodes in each group,
    // and then sorts the average positions of each group to produce a list of contraints,
    // i.e. what nodes need to come before which, in this depth
    const constraints: [GroupID, GroupID][] = [];
    if (lastAssignment !== null) {
      const positions: Map<GroupID, number> = new Map();

      const positionTotal: Map<GroupID, number> = new Map();
      const positionCount: Map<GroupID, number> = new Map();
      for (const groupId of marriageGroups) {
        const group = groups.groups.get(groupId)!;
        for (const person of group) {
          for (const marriage of personMarriages[person]) {
            for (const child of marriage.children) {
              const childGroupId = groups.members[child];
              const position = lastAssignment.indexOf(childGroupId);
              positionTotal.set(groupId, (positionTotal.get(groupId) ?? 0) + position);
              positionCount.set(groupId, (positionCount.get(groupId) ?? 0) + 1);
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
    }

    for (const fromGroup of marriageGroups) {
      const from = groups.groups.get(fromGroup)![0];
      const map = new Map<GroupID, number>();
      // TODO: number of function calls can be halved
      for (const toGroup of marriageGroups) {
        const to = groups.groups.get(toGroup)![0];
        map.set(toGroup, distanceBetween(from, to, personMarriages, personParents));
      }
      distances.set(fromGroup, map);
    }

    let minimum = Infinity;
    let minimumPermutation: GroupID[] = [];

    const permutations = permute(marriageGroups);
    for (const permutation of permutations) {
      for (const [a, b] of constraints) {
        if (permutation.indexOf(a) > permutation.indexOf(b)) {
          continue;
        }
      }

      let total = 0;
      for (let x = 0; x < permutation.length - 1; x++) {
        for (let y = x + 1; y < permutation.length; y++) {
          const d = distances.get(permutation[x])!.get(permutation[y])!;
          total += d * (y - x);
        }
      }
      if (total < minimum) {
        minimum = total;
        minimumPermutation = permutation;
      }
    }

    outputDepths.set(depth, minimumPermutation);

    lastAssignment = minimumPermutation;
  }

  return outputDepths;
}

function permute<T>(xs: T[]): T[][] {
  if (xs.length === 0) return [[]];
  const result: T[][] = [];
  for (let i = 0; i < xs.length; i++) {
    const rest = xs.slice(0, i).concat(xs.slice(i + 1));
    const restPermutations = permute(rest);
    for (const perm of restPermutations) {
      result.push([xs[i], ...perm]);
    }
  }
  return result;
}

function distanceBetween(
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

    for (const marriage of personMarriages[current]) {
      for (const spouse of marriage.parents) {
        if (!distances.has(spouse)) {
          distances.set(spouse, d);
          queue.push(spouse);
        }
      }
      for (const child of marriage.children) {
        if (!distances.has(child)) {
          distances.set(child, d + 1);
          queue.push(child);
        }
      }
    }
    for (const marriage of personParents[current]) {
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
