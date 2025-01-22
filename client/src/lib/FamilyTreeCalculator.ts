export type PersonData = {
  name: string;
  spouses: PersonData[];
  parents: PersonData[];
  children: PersonData[];
};

type PersonNode = { name: string; x: number; y: number };

export default class FamilyTreeCalculator {
  people: PersonNode[] = [];
  lines: { x1: number; x2: number; y1: number; y2: number }[] = [];

  RECT_HEIGHT = 40;
  RECT_WIDTH = 80;

  private horizontalLine(x: number, y: number, width: number) {
    return {
      x1: x,
      x2: x + width,
      y1: y,
      y2: y
    };
  }

  private verticalLine(x: number, y: number, height: number) {
    return {
      x1: x,
      x2: x,
      y1: y,
      y2: y + height
    };
  }

  createPerson({ name }: PersonData, x: number, y: number): PersonNode {
    const data = { name, x, y };
    this.people.push({ name, x, y });
    return data;
  }

  createSpouse({ x, y }: PersonNode, spouse: PersonData, spouseGap: number): PersonNode {
    const spouseNode = { name: spouse.name, x: x + this.RECT_WIDTH + spouseGap, y };
    this.people.push(spouseNode);
    const marriageLine = this.horizontalLine(
      x + this.RECT_WIDTH,
      y + this.RECT_HEIGHT / 2,
      spouseGap
    );
    this.lines.push(marriageLine);
    return spouseNode;
  }

  createChildren(
    p1: PersonNode,
    p2: PersonNode,
    children: PersonData[],
    generationGap: number,
    childGap: number
  ): PersonNode[] {
    const adjustedChildGap = childGap + this.RECT_WIDTH;
    const downLine = this.verticalLine(
      (p1.x + this.RECT_WIDTH + p2.x) / 2,
      p1.y + this.RECT_HEIGHT / 2,
      this.RECT_HEIGHT / 2 + generationGap / 2
    );
    this.lines.push(downLine);
    const childLineLength = (children.length - 1) * adjustedChildGap;
    const childLine = this.horizontalLine(
      downLine.x1 - childLineLength / 2,
      downLine.y2,
      childLineLength
    );
    this.lines.push(childLine);
    let x = childLine.x1 - this.RECT_WIDTH / 2;
    const childrenNodes = [];
    for (const child of children) {
      this.lines.push(this.verticalLine(x + this.RECT_WIDTH / 2, childLine.y1, generationGap / 2));
      const childNode = {
        name: child.name,
        x,
        y: downLine.y2 + generationGap / 2
      };
      childrenNodes.push(childNode);
      this.people.push(childNode);
      x += adjustedChildGap;
    }
    return childrenNodes;
  }
}
