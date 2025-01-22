<script lang="ts">
  import * as d3 from 'd3';
  import FamilyTreeCalculator, { type PersonData } from '../lib/FamilyTreeCalculator';
  import { onMount } from 'svelte';

  const RECT_HEIGHT = 40;
  const RECT_WIDTH = 80;
  const SPOUSE_GAP = 150;

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

  // Add the midpoint to the dat
  // Add links
  onMount(() => {
    const svg = d3.select('svg'),
      width = +svg.attr('width'),
      height = +svg.attr('height');

    svg
      .selectAll('.link')
      .data(calc.lines)
      .enter()
      .append('line')
      .attr('class', 'link')
      .attr('x1', (d) => d.x1)
      .attr('y1', (d) => d.y1)
      .attr('x2', (d) => d.x2)
      .attr('y2', (d) => d.y2);

    // Add nodes
    const nodes = svg
      .selectAll('.node')
      .data(calc.people)
      .enter()
      .append('g')
      .attr('class', 'node')
      .attr('transform', (d) => `translate(${d.x},${d.y})`);

    nodes
      .append('rect')
      .attr('height', RECT_HEIGHT)
      .attr('width', RECT_WIDTH)
      .attr('style', 'stroke-width: 1; stroke: black')
      .attr('fill', 'white');

    nodes
      .append('text')
      .attr('dx', RECT_WIDTH / 2)
      .attr('dy', RECT_HEIGHT / 2 + 5)
      .text((d) => d.name || '');
  });
</script>

<svg width="800" height="600"></svg>
