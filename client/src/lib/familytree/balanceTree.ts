import type { Positions, PersonID, Tree } from './models';

export function balanceTree(
  tree: Tree,
  center: [number, number],
  BASE_WIDTH = 160,
  GENERATION_HEIGHT = 120
): Positions {
  const positions: Positions = {};

  // The x position of the current "right edge" of the graph
  // Accumulates as nodes are added to the right.
  let right = 0;

  let y = center[1];
  const subtree = new Set<PersonID>();
  const subtreeX = placeSubtree(tree.focus, subtree);

  y = center[1];
  const supertree = new Set<PersonID>();
  const supertreeX = placeSupertree(tree.focus, supertree);

  // To make sure the focused node is at center, we need to shift
  // the nodes in the subtree and supertree
  adjustNodes(subtree, center[0] - subtreeX);
  adjustNodes(supertree, center[0] - supertreeX);

  return positions;

  // Shifts all nodes by a certain X
  function adjustNodes(set: Set<PersonID>, dx: number) {
    for (const node of set) {
      positions[node].x += dx;
    }
  }

  // Set the position of a node if they have not already been given a position
  function addPerson(personId: PersonID, x: number, y: number) {
    if (positions[personId] !== undefined) return;
    positions[personId] = { x, y };
  }

  /**
   * Assigns a position to the spouse node and descendant nodes
   * @param focused The ID of the focused node
   * @param subtree A mutable set of nodes currently included in the subtree (including focused node)
   * @returns The x position of the focused node
   */
  function placeSubtree(focused: PersonID, subtree: Set<PersonID>): number {
    subtree.add(focused);

    // finds any marriages for which this node is a parent
    const marriages = tree.marriages.filter((m) => m.parents.includes(focused));
    if (marriages.length === 0) {
      // Places a single node and pushes the right edge right by 1 base width
      const meX = right + BASE_WIDTH / 2;
      addPerson(focused, meX, y);
      right += BASE_WIDTH;
      return meX;
    }

    // assuming someone has at most one marriage, and assuming no cycles
    if (marriages.length == 1) {
      const marriage = marriages[0];
      // assuming a marriage has only one spouse
      const spouse = marriage.parents.find((p) => p !== focused)!;
      subtree.add(spouse);
      const children = marriage.children;
      // if we only have one child, the width of the two parents (2 * BASE_WIDTH)
      // is larger than the width of the child (BASE_WIDTH), so pad both sides
      // by half of BASE_WIDTH
      if (children.length < 2) {
        right += BASE_WIDTH / 2;
      }

      // recursively place children
      const left = right;
      y += GENERATION_HEIGHT;
      for (const child of children) {
        const subsubtree = new Set<string>();
        const minRight = right + BASE_WIDTH / 2
        const childx = placeSubtree(child, subsubtree);
        // Happens in case of children having spouses
        if (childx < minRight) {
          adjustNodes(subsubtree, minRight - childx);
          right += minRight - childx + BASE_WIDTH / 2;
        }
        for (const person of subsubtree) {
          subtree.add(person);
        }
      }
      y -= GENERATION_HEIGHT;

      // render parents at the midpoint of the children's width
      const mid = (left + right) / 2;
      const meX = mid - BASE_WIDTH / 2;
      addPerson(spouse, mid + BASE_WIDTH / 2, y);
      addPerson(focused, meX, y);

      if (children.length < 2) {
        right += BASE_WIDTH / 2;
      }

      return meX;
    }
    return right;
  }

  /**
   * Assigns a position to ancestor nodes
   * @param focused The ID of the focused node
   * @param subtree A mutable set of nodes currently included in the supertree (excluding focused node)
   * @returns The x position of the focused node
   */
  function placeSupertree(person: PersonID, supertree: Set<PersonID>): number {
    // assuming someone has exactly two parents from one marriage, or no parent marriage
    const parentMarriage = tree.marriages.find((m) => m.children.includes(person));

    if (parentMarriage == undefined) {
      const meX = right + BASE_WIDTH / 2;
      addPerson(person, meX, y);
      right += BASE_WIDTH;
      return meX;
    }

    const mother = parentMarriage.parents[0];
    const father = parentMarriage.parents[1];

    supertree.add(mother);
    supertree.add(father);

    y -= GENERATION_HEIGHT;
    const motherX = placeSupertree(mother, supertree);
    const fatherX = placeSupertree(father, supertree);
    y += GENERATION_HEIGHT;

    const mid = (motherX + fatherX) / 2;
    addPerson(person, mid, y);

    return mid;
  }
}
