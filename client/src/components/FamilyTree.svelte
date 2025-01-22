<script lang="ts">
  import * as d3 from 'd3';
  import { onMount } from 'svelte';
  type PersonData = {
    name: string;
    spouses: PersonData[];
    parents: PersonData[];
    children: PersonData[];
  };

  const RECT_HEIGHT = 40;
  const RECT_WIDTH = 80;
  const SPOUSE_GAP = 150;

  let x = 100;
  let y = 100;
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
    children: []
  };
  const horizontalLine = (x: number, y: number, width: number) => ({
    x1: x,
    x2: x + width,
    y1: y,
    y2: y,
  })
  const verticalLine = (x: number, y: number, height: number) => ({
    x1: x,
    x2: x,
    y1: y,
    y2: y + height,
  })
  const data: { name: string; x: number; y: number }[] = [];
  data.push({ name: focus.name, x, y });
  const spouse = focus.spouses[0];
  data.push({ name: spouse.name, x: x + RECT_WIDTH + SPOUSE_GAP, y });
  const lines: { x1: number; x2: number; y1: number; y2: number }[] = [];
  const marriageLine = horizontalLine(x + RECT_WIDTH, y + RECT_HEIGHT / 2, SPOUSE_GAP);
  lines.push(marriageLine);
  lines.push(verticalLine((marriageLine.x1 + marriageLine.x2) / 2, marriageLine.y1, 50))

  // Add the midpoint to the dat
  // Add links
  onMount(() => {
    const svg = d3.select('svg'),
      width = +svg.attr('width'),
      height = +svg.attr('height');

    svg
      .selectAll('.link')
      .data(lines)
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
      .data(data)
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
