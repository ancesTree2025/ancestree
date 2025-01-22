<script lang="ts">
  import * as d3 from 'd3';
  import FamilyTreeCalculator, { type PersonData } from '../lib/FamilyTreeCalculator';

  const RECT_HEIGHT = 40;
  const RECT_WIDTH = 80;
  const RECT_RADIUS = 10;

  let focus: PersonData = {
    name: 'abc',
    spouses: [
      {
        name: 'def',
        spouses: [],
        parents: [],
        children: []
      }
    ],
    parents: [],
    children: [
      {
        name: 'def',
        spouses: [],
        parents: [],
        children: []
      },
      {
        name: 'def',
        spouses: [],
        parents: [],
        children: []
      },
      {
        name: 'def',
        spouses: [],
        parents: [],
        children: []
      }
    ]
  };
  const calc = new FamilyTreeCalculator();
  const personNode = calc.createPerson(focus, 100, 100);
  const spouseNode = calc.createSpouse(personNode, focus.spouses[0], 150);
  calc.createChildren(personNode, spouseNode, focus.children, 50, 50);
</script>

<svg width="800" height="600">
  {#each calc.lines as line}
    <line x1={line.x1} y1={line.y1} x2={line.x2} y2={line.y2} class="stroke-line"></line>
  {/each}
  {#each calc.people as person}
    <g transform="translate({person.x},{person.y})">
      <rect
        x={-RECT_WIDTH / 2}
        y={-RECT_HEIGHT / 2}
        width={RECT_WIDTH}
        height={RECT_HEIGHT}
        rx={RECT_RADIUS}
        class="fill-node-bg"
      ></rect>
      <text class="" text-anchor="middle" dominant-baseline="middle">{person.name}</text>
    </g>
  {/each}
</svg>
