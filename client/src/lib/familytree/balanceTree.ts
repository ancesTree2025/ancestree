import type { Positions, PersonID, Tree, Marriage } from './types';

/* visMarraiges are the marriages used for the purpose of the tree
     visualisation. If Focus has Wife 1, Wife 2 and Wife 3 then they
     will be drawn in a line as follows: Wife 1 == Focus == Wife 2 -- Wife 3.
     So any children Focus has with Wife 3 will be drawn as if they are
     children of Wife 2 and Wife 3 - so [Wife 2, Wife 3] are the visParents */

export function balanceTree(
  tree: Tree,
  BASE_WIDTH = 160,
  GENERATION_HEIGHT = 120
): {
  positions: Positions;
  visMarriages: [Marriage, number][];
  treeWidth: number;
} {
  const positions: Positions = {};

  const visMarriages: [Marriage, number][] = [];

  // The x position of the current "right edge" of the graph
  // Accumulates as nodes are added to the right.
  let right = 0;

  const subtree = new Set<PersonID>();
  const parent1 = findParents(tree.focus)[0];
  const subtreeX = placeSubtree(parent1 ?? tree.focus, subtree, parent1 ? -GENERATION_HEIGHT : 0);

  const supertree = new Set<PersonID>();
  const supertreeX = placeSupertree(
    parent1 ?? tree.focus,
    supertree,
    parent1 ? -GENERATION_HEIGHT : 0
  );

  right = 0;

  for (const person of subtree) {
    right = Math.max(positions[person].x + BASE_WIDTH / 2, right);
  }

  let left = right;

  for (const person of subtree) {
    left = Math.min(positions[person].x - BASE_WIDTH / 2, left);
  }

  // To make sure the focused node is at center, we need to shift
  // the nodes in the subtree and supertree
  adjustNodes(subtree, -left);
  adjustNodes(supertree, -supertreeX + subtreeX - left);

  return {
    positions,
    visMarriages,
    treeWidth: right - left
  };

  function findParents(id: PersonID): PersonID[] {
    return tree.marriages.find((m) => m.children.includes(id))?.parents ?? [];
  }

  // Shifts all nodes by a certain X
  function adjustNodes(set: Set<PersonID>, dx: number) {
    for (const node of set) {
      positions[node].x += dx;
    }
  }

  // Set the position of a node if they have not already been given a position
  function addPerson(personId: PersonID, x: number, y: number) {
    if (positions[personId] !== undefined) return;
    right = Math.max(right, x + BASE_WIDTH / 2);
    positions[personId] = { x, y };
  }

  /**
   * Assigns a position to the spouse node and descendant nodes
   * @param focused The ID of the focused node
   * @param subtree A mutable set of nodes currently included in the subtree (including focused node)
   * @returns The x position of the focused node
   */
  function placeSubtree(focused: PersonID, subtree: Set<PersonID>, y: number): number {
    subtree.add(focused);

    // finds any marriages for which this node is a parent
    const marriages = tree.marriages.filter((m) => m.parents.includes(focused));
    if (marriages.length === 0) {
      // Places a single node and pushes the right edge right by 1 base width
      const meX = right + BASE_WIDTH / 2;
      addPerson(focused, meX, y);
      return meX;
    }

    // assuming someone has at most one marriage, and assuming no cycles

    let meX: number = right;
    for (const [i, marriage] of marriages.entries()) {
      // assuming a marriage has only one spouse
      const spouse = marriage.parents.find((p) => p !== focused)!;
      if (i > 1) {
        visMarriages.push([
          {
            parents: [marriages[i - 1].parents.find((p) => p !== focused)!, spouse],
            children: marriage.children
          },
          0
        ]);
      } else {
        visMarriages.push([marriage, i]);
      }
      subtree.add(spouse);
      const children = marriage.children;

      // recursively place children
      const start = right;
      for (const child of children) {
        const subsubtree = new Set<string>();
        const minChildx = right + BASE_WIDTH / 2;
        const childx = placeSubtree(child, subsubtree, y + GENERATION_HEIGHT);
        // Happens in case of children having spouses
        if (childx < minChildx) {
          adjustNodes(subsubtree, minChildx - childx);
          right += minChildx - childx;
        }
        for (const person of subsubtree) {
          subtree.add(person);
        }
      }

      let personLeft: PersonID | undefined;
      let personRight: PersonID;

      if (i === 0) {
        if (marriages.length > 1) {
          personLeft = spouse;
          personRight = focused;
        } else {
          personLeft = focused;
          personRight = spouse;
        }
      } else {
        personLeft = undefined;
        personRight = spouse;
      }

      // render parents at the midpoint of the children's width
      const mid =
        children.length === 0 ? (i === 0 ? right + BASE_WIDTH : right) : (start + right) / 2;
      addPerson(personRight, mid + BASE_WIDTH / 2, y);
      if (personLeft !== undefined) {
        meX = mid - BASE_WIDTH / 2;
        addPerson(personLeft, meX, y);
        if (personRight === focused) {
          meX += BASE_WIDTH;
        }
      }
    }
    return meX;
  }

  /**
   * Assigns a position to ancestor nodes
   * @param focused The ID of the focused node
   * @param subtree A mutable set of nodes currently included in the supertree (excluding focused node)
   * @returns The x position of the focused node
   */
  function placeSupertree(person: PersonID, supertree: Set<PersonID>, y: number): number {
    // assuming someone has exactly two parents from one marriage, or no parent marriage
    const parentMarriage = tree.marriages.find((m) => m.children.includes(person));

    if (parentMarriage === undefined) {
      const meX = right + BASE_WIDTH / 2;
      addPerson(person, meX, y);
      return meX;
    }

    visMarriages.push([parentMarriage, 0]);

    const mother = parentMarriage.parents[0];
    const father = parentMarriage.parents[1];

    supertree.add(mother);
    supertree.add(father);

    const motherX = placeSupertree(mother, supertree, y - GENERATION_HEIGHT);
    const fatherX = placeSupertree(father, supertree, y - GENERATION_HEIGHT);

    const mid = (motherX + fatherX) / 2;
    addPerson(person, mid, y);

    return mid;
  }
}
