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
  console.log(marriageGroups);

  const depths = assignDepths(tree, marriageGroups, personMarriages, personParents);
  console.log(depths);

  // TODO: finish this
  return { positions: {}, treeWidth: 0 };
}

/**
 * Represents the assignments of people to "marriage groups" within a family tree.
 *
 * @typedef {Object} GroupAssigments
 *
 * @property {Record<GroupID, PersonID[]>} groups - A mapping from group ID to the members of that group.
 * @property {Record<PersonID, GroupID>} members - A mapping from person ID to the group they are in.
 */
type GroupAssigments = {
  groups: Record<GroupID, PersonID[]>;
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
  const marriageGroups: Record<GroupID, PersonID[]> = {};
  const memberOf: Record<PersonID, GroupID> = {};

  // initialise groups as singletons
  for (const [id] of tree.people) {
    marriageGroups[id] = [id];
    memberOf[id] = id;
  }

  // for each marriage, merge the groups of the parents
  // TODO: optimise the order of merging (multiple spouses may be put on all one side)
  for (const marriage of tree.marriages) {
    const groupId = memberOf[marriage.parents[0]];
    let newGroup: PersonID[] = [];
    for (const person of marriage.parents) {
      const group = marriageGroups[memberOf[person]];
      newGroup = newGroup.concat(group);
      delete marriageGroups[memberOf[person]];
      memberOf[person] = groupId;
    }
    marriageGroups[groupId] = newGroup;
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
): Record<GroupID, number> {
  const depths: Record<GroupID, number> = {};
  const minDepths: Record<GroupID, number> = {};
  const maxDepths: Record<GroupID, number> = {};

  // by default no groups have depths
  const unfound: Set<GroupID> = new Set(Object.keys(groups.groups));

  // give the focused node's group a min and max depth of zero
  const focusGroupId = groups.members[tree.focus];
  maxDepths[focusGroupId] = 0;
  minDepths[focusGroupId] = 0;

  while (unfound.size > 0) {
    // find the group ID with the lowest minimum depth, or highest maximum depth
    // this is needed for example when a grandchild has a minimum depth of 1 and
    // is assigned a depth of 1 before the child, meaning the child's depth would
    // be illegal
    let minimum = Infinity;
    let groupId: GroupID;
    for (const group of unfound) {
      const maximumDepth = maxDepths[group] ?? -Infinity;
      const minimumDepth = minDepths[group] ?? Infinity;
      const depth = Math.min(-maximumDepth, minimumDepth);
      if (depth < minimum) {
        minimum = depth;
        groupId = group;
      }
    }
    groupId = groupId!;
    unfound.delete(groupId);

    const group = groups.groups[groupId];

    // assign smallest legal depth
    const minDepth = minDepths[groupId] ?? -Infinity;
    const maxDepth = maxDepths[groupId] ?? Infinity;
    let depth = 0;
    if (minDepth > 0) {
      depth = minDepth;
    } else if (maxDepth < 0) {
      depth = maxDepth;
    }

    depths[groupId] = depth;

    for (const person of group) {
      // update the max depth of all the parents to be lower than this group's depth
      for (const marriage of personParents[person]) {
        const parents = marriage.parents;
        for (const parent of parents) {
          const groupId = groups.members[parent];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(groupId)) continue;

          maxDepths[groupId] = Math.min(maxDepths[groupId] ?? Infinity, depth - 1);
        }
      }
      // update the min depth of all the children to be higher than this group's depth
      for (const marriage of personMarriages[person]) {
        const children = marriage.children;
        for (const child of children) {
          const groupId = groups.members[child];
          // don't bother updating groups whose depths have been found
          if (!unfound.has(groupId)) continue;

          minDepths[groupId] = Math.max(minDepths[groupId] ?? -Infinity, depth + 1);
        }
      }
    }
  }

  return depths;
}
