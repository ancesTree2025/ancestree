import type { Tree } from '$lib/types';

class TreeHistory {
  private history: Tree[] = $state([]);
  private index: number = $state(-1);

  /**
   * When a tree is inserted to this history, it will delete newer trees
   * insert {@link tree} as the newest tree.
   *
   * @param tree to insert as the newest tree.
   */
  put(tree: Tree) {
    this.history = this.history.slice(0, this.index + 1);
    this.history.push(tree);
    this.index = this.history.length - 1;
    console.log(this.toString());
  }

  /**
   * Successive calls will keep undo-ing until no more.
   * Assumes that {@link canUndo} returns true.
   *
   * @return the tree immediately before the current rendered tree.
   */
  undo(): Tree {
    if (!this.canUndo()) throw new Error('cannot undo when there is nothing');

    const tree = this.history[--this.index];
    console.log(this.toString());
    return tree;
  }

  canUndo(): boolean {
    return this.index > 0;
  }

  /**
   * Successive calls will keep redo-ing until no more.
   * Assumes that {@link canRedo} returns true.
   *
   * @see
   * @return the tree immediately before the current rendered tree.
   */
  redo(): Tree {
    if (!this.canRedo()) throw new Error('cannot redo when there is nothing');

    const tree = this.history[++this.index];
    console.log(this.toString());
    return tree;
  }

  canRedo(): boolean {
    return this.index < this.history.length - 1;
  }

  toString(): string {
    return `TreeHistory[history=${this.history}, index=${this.index}]`;
  }
}

export const treeHistory = new TreeHistory();
