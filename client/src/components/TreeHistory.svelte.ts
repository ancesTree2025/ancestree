import type { PersonID, Tree } from '$lib/types';

export type TreeHistoryElem = {
  tree: Tree;
  relation?: {
    tree: Tree;
    relationDescriptor: string;
  };
  sidePanel: {
    qid: PersonID;
    name: string;
  };
};

class TreeHistory {
  private history: TreeHistoryElem[] = $state([]);
  private index: number = $state(-1);

  /**
   * When a tree is inserted to this history, it will delete newer trees
   * insert {@link tree} as the newest tree.
   *
   * @param tree to insert as the newest tree.
   */
  put(elem: TreeHistoryElem) {
    this.history = this.history.slice(0, this.index + 1);
    this.history.push(elem);
    this.index = this.history.length - 1;
  }

  updateSidePanel(sidePanel: { qid: PersonID; name: string }) {
    this.history[this.index] = {
      ...this.history[this.index],
      sidePanel
    };
  }
  /**
   * Successive calls will keep undo-ing until no more.
   * Assumes that {@link canUndo} returns true.
   *
   * @return the tree immediately before the current rendered tree.
   */
  undo(): TreeHistoryElem {
    if (!this.canUndo()) throw new Error('cannot undo when there is nothing');

    return this.history[--this.index];
  }

  canUndo(): boolean {
    return this.index > 0;
  }

  /**
   * Successive calls will keep redo-ing until no more.
   * Assumes that {@link canRedo} returns true.
   *
   * @return the tree immediately before the current rendered tree.
   */
  redo(): TreeHistoryElem {
    if (!this.canRedo()) throw new Error('cannot redo when there is nothing');

    return this.history[++this.index];
  }

  canRedo(): boolean {
    return this.index < this.history.length - 1;
  }

  toString(): string {
    return `TreeHistory[history=${JSON.stringify(this.history)}, index=${this.index}]`;
  }
}

export const treeHistory = new TreeHistory();
