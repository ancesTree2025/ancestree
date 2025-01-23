<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Marriages, People } from '$lib/familytree/models';
  import * as d3 from 'd3';

  const RECT_HEIGHT = 40;
  const RECT_WIDTH = 80;
  const RECT_RADIUS = 10;

  let people = $state<People>(new Map());

  let marriages = $state<Marriages>([]);

  $effect(() => {
    const tree = fetchTree('test');
    people = tree.people;
    marriages = tree.marriages;
  });
</script>

<svg width="800" height="600">
  {#each marriages as marriage}
    <!-- fetch Person for each parent, child -->
    {@const mother = people.get(marriage.parents[0])!}
    {@const father = people.get(marriage.parents[1])!}
    {@const children = marriage.children.map((id) => people.get(id)!)}

    <!-- Draw marriage lines -->
    {@const parentsX = (mother.x + father.x) / 2}
    {#if mother.y == father.y}
      <line x1={mother.x} y1={mother.y} x2={father.x} y2={father.y} class="stroke-line" />
    {:else}
      <line x1={mother.x} y1={mother.y} x2={parentsX} y2={mother.y} class="stroke-line" />
      <line x1={parentsX} y1={mother.y} x2={parentsX} y2={father.y} class="stroke-line" />
      <line x1={father.x} y1={father.y} x2={parentsX} y2={father.y} class="stroke-line" />
    {/if}

    {#if children.length > 0}
      <!-- Draw line between parents and children -->
      {@const parentsY = Math.max(mother.y, father.y)}
      {@const childrenY = Math.min(...children.map((child) => child.y))}
      {@const midY = (parentsY + childrenY) / 2}
      <line x1={parentsX} y1={parentsY} x2={parentsX} y2={midY} class="stroke-line" />

      <!-- Draw children line -->
      {@const leftChildX = Math.min(parentsX, ...children.map((child) => child.x))}
      {@const rightChildX = Math.max(parentsX, ...children.map((child) => child.x))}
      <line x1={leftChildX} y1={midY} x2={rightChildX} y2={midY} class="stroke-line" />

      <!-- Draw line from each child to children line -->
      {#each marriage.children as child}
        {@const childNode = people.get(child)!}
        <line x1={childNode.x} y1={midY} x2={childNode.x} y2={childNode.y} class="stroke-line" />
      {/each}
    {/if}
  {/each}
  {#each people as [id, person] (id)}
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
