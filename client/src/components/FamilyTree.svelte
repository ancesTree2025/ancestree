<script lang="ts">
  import { balanceTree } from '$lib/familytree/balanceTree';
  import { type Marriages, type Positions, type Tree } from '$lib/familytree/models';
  import * as d3 from 'd3';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let visMarriages = $state<Marriages | undefined>(tree?.marriages);
  let positions = $state<Positions>({});
  let treeWidth = $state<number>();

  $effect(() => {
    if (tree) {
      [positions, visMarriages, treeWidth] = balanceTree(tree, [500, 300]);
    } else {
      positions = {};
    }
  });

  $effect(() => {
    d3.select('#svg-root').call(d3.zoom().on('zoom', zoomed) as any);

    function zoomed(event: any) {
      d3.select("#zoom-group").attr('transform', event.transform);
    }
  });

  const RECT_HEIGHT = 60;
  const RECT_WIDTH = 120;
  const RECT_RADIUS = 10;

  let height = $state(0);
  let width = $state(0);

  let ZOOM_FACTOR = $derived(Math.min(1, treeWidth ? width / treeWidth : 1));
</script>


<svg id="svg-root" class="h-full w-full" bind:clientWidth={width} bind:clientHeight={height}>
  <g id="zoom-group">
    {#if tree && visMarriages}
      {#each visMarriages as marriage}
        <!-- fetch Person for each parent, child -->
        {@const mother = positions[marriage.parents[0]]}
        {@const father = positions[marriage.parents[1]]}
        {@const children = marriage.children.map((id) => positions[id])}

        {#if mother && father}
          <!-- Draw marriage lines -->
          {@const parentsX = (mother.x + father.x) / 2}
          {#if mother.y === father.y}
            <line
              x1={mother.x * ZOOM_FACTOR}
              y1={mother.y * ZOOM_FACTOR}
              x2={father.x * ZOOM_FACTOR}
              y2={father.y * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />
          {:else}
            <line
              x1={mother.x * ZOOM_FACTOR}
              y1={mother.y * ZOOM_FACTOR}
              x2={parentsX * ZOOM_FACTOR}
              y2={mother.y * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />
            <line
              x1={parentsX * ZOOM_FACTOR}
              y1={mother.y * ZOOM_FACTOR}
              x2={parentsX * ZOOM_FACTOR}
              y2={father.y * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />
            <line
              x1={father.x * ZOOM_FACTOR}
              y1={father.y * ZOOM_FACTOR}
              x2={parentsX * ZOOM_FACTOR}
              y2={father.y * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />
          {/if}

          {#if children.length > 0}
            <!-- Draw line between parents and children -->
            {@const parentsY = Math.max(mother.y, father.y)}
            {@const childrenY = Math.min(...children.map((child) => child?.y ?? Infinity))}
            {@const midY = (parentsY + childrenY) / 2}
            <line
              x1={parentsX * ZOOM_FACTOR}
              y1={parentsY * ZOOM_FACTOR}
              x2={parentsX * ZOOM_FACTOR}
              y2={midY * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />

            <!-- Draw children line -->
            {@const leftChildX = Math.min(
              parentsX,
              ...children.map((child) => child?.x ?? Infinity)
            )}
            {@const rightChildX = Math.max(
              parentsX,
              ...children.map((child) => child?.x ?? -Infinity)
            )}
            <line
              x1={leftChildX * ZOOM_FACTOR}
              y1={midY * ZOOM_FACTOR}
              x2={rightChildX * ZOOM_FACTOR}
              y2={midY * ZOOM_FACTOR}
              class="stroke-node stroke-line"
            />

            <!-- Draw line from each child to children line -->
            {#each children as child}
              {#if child}
                <line
                  x1={child.x * ZOOM_FACTOR}
                  y1={midY * ZOOM_FACTOR}
                  x2={child.x * ZOOM_FACTOR}
                  y2={child.y * ZOOM_FACTOR}
                  class="stroke-node stroke-line"
                />
              {/if}
            {/each}
          {/if}
        {/if}
      {/each}
      {#each tree.people as [id, person]}
        {@const position = positions[id]}
        {#if position}
          <g transform="translate({position.x * ZOOM_FACTOR},{position.y * ZOOM_FACTOR})">
            <rect
              x={-RECT_WIDTH * ZOOM_FACTOR / 2}
              y={-RECT_HEIGHT  * ZOOM_FACTOR/ 2}
              width={RECT_WIDTH * ZOOM_FACTOR}
              height={RECT_HEIGHT * ZOOM_FACTOR}
              rx={RECT_RADIUS * ZOOM_FACTOR}
              class="fill-node"
            ></rect>
            <foreignObject
              x={-RECT_WIDTH * ZOOM_FACTOR / 2}
              y={-RECT_HEIGHT * ZOOM_FACTOR / 2}
              width={RECT_WIDTH * ZOOM_FACTOR}
              height={RECT_HEIGHT * ZOOM_FACTOR}
            >
              <button
                onclick={() => getPersonInfo(id, person.name)}
                style="font-size: {16 * ZOOM_FACTOR}px;"
                class="flex h-full w-full cursor-pointer items-center justify-center text-center"
              >
                {person.name}
              </button>
            </foreignObject>
          </g>
        {/if}
      {/each}
    {/if}
  </g>
</svg>
