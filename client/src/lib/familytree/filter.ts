import type { FilterOption, PersonID, Tree } from '$lib/types';

export function filterByOption(tree: Tree, options: Record<FilterOption, boolean>): Tree {
  if (options.all) return tree;
  const personAssignment = new Set<PersonID>();

  const focus = tree.focus;
  personAssignment.add(focus);
  const ascVisited = new Set<PersonID>();
  const descVisited = new Set<PersonID>();
  assignAncestors(tree, personAssignment, focus, ascVisited, descVisited, options);
  assignDescendants(tree, personAssignment, focus, ascVisited, descVisited, options);

  console.log(personAssignment);

  return {
    ...tree,
    people: tree.people.filter((person) => personAssignment.has(person[0]))
  };
}

function assignAncestors(
  tree: Tree,
  personAssignment: Set<PersonID>,
  person: PersonID,
  ascVisited: Set<PersonID>,
  descVisited: Set<PersonID>,
  options: Record<FilterOption, boolean>
) {
  if (!options.ancestor) return;
  if (ascVisited.has(person)) return;
  ascVisited.add(person);
  personAssignment.add(person);
  const marriages = tree.marriages.filter((m) => m.children.includes(person));
  for (const marriage of marriages) {
    for (const parent of marriage.parents) {
      assignAncestors(tree, personAssignment, parent, ascVisited, descVisited, options);
    }
    if (options.sibling) {
      for (const child of marriage.children) {
        assignDescendants(tree, personAssignment, child, ascVisited, descVisited, options);
      }
    }
  }
}

function assignDescendants(
  tree: Tree,
  personAssignment: Set<PersonID>,
  person: PersonID,
  ascVisited: Set<PersonID>,
  descVisited: Set<PersonID>,
  options: Record<FilterOption, boolean>
) {
  if (!options.descendant) return;
  if (descVisited.has(person)) return;
  descVisited.add(person);
  personAssignment.add(person);
  const marriages = tree.marriages.filter((m) => m.parents.includes(person));
  for (const marriage of marriages) {
    for (const parent of marriage.parents) {
      personAssignment.add(parent);
      if (options.spousefamily) {
        assignAncestors(tree, personAssignment, parent, ascVisited, descVisited, options);
        assignDescendants(tree, personAssignment, parent, ascVisited, descVisited, options);
      }
    }
    for (const child of marriage.children) {
      assignDescendants(tree, personAssignment, child, ascVisited, descVisited, options);
    }
  }
}
