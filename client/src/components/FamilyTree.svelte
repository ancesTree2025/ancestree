<script lang="ts">
  import { balanceTree } from '$lib/familytree/balanceTree';
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';

  let { tree }: { tree?: Tree } = $props<{ treeData?: Tree }>();

  const RECT_HEIGHT = 40;
  const RECT_WIDTH = 80;
  const RECT_RADIUS = 10;

  $effect(() => {
    const fetched = fetchTree('test');
    balanceTree('a', fetched, [200, 200]);
    tree = fetched;
  });
</script>

<svg width="800" height="600">
  {#if tree}
    {#each tree.marriages as marriage}
      <!-- fetch Person for each parent, child -->
      {@const mother = tree.people.get(marriage.parents[0])!}
      {@const father = tree.people.get(marriage.parents[1])!}
      {@const children = marriage.children.map((id) => tree!.people.get(id)!)}

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
        {#each children as child}
          <line x1={child.x} y1={midY} x2={child.x} y2={child.y} class="stroke-line" />
        {/each}
      {/if}
    {/each}
    {#each tree.people as [id, person] (id)}
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
  {/if}
</svg>
