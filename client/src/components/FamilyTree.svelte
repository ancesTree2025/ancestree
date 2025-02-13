<script lang="ts">
  import { balanceTree } from '$lib';
  import type { Marriages, Positions, Tree } from '$lib/types';
  import * as d3 from 'd3';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let positions = $state<Positions>({});
  let visMarriages = $state<Marriages | undefined>(tree?.marriages);
  let treeWidth = $state<number>();

  $effect(() => {
    if (tree) {
      const result = balanceTree(tree, 300);
      positions = result.positions;
      visMarriages = result.visMarriages;
      treeWidth = result.treeWidth;
    } else {
      positions = {};
    }
  });

  $effect(() => {
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    d3.select('#svg-root').call(d3.zoom().on('zoom', zoomed) as any);

    /* eslint-disable  @typescript-eslint/no-explicit-any */
    function zoomed(event: any) {
      d3.select('#zoom-group').attr('transform', event.transform);
    }
  });

  const RECT_HEIGHT = 60;
  const RECT_WIDTH = 120;
  const RECT_RADIUS = 10;

  let height = $state(0);
  let width = $state(0);

  const zoomFactor = $derived(Math.min(1, treeWidth ? width / treeWidth : 1));

  const xOffset = $derived(
    treeWidth !== undefined && treeWidth < width ? (width - treeWidth) / 2 : 0
  );
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
              x1={zoomFactor * mother.x + xOffset}
              y1={zoomFactor * mother.y}
              x2={zoomFactor * father.x + xOffset}
              y2={zoomFactor * father.y}
              class="stroke-node stroke-line"
            />
          {:else}
            <line
              x1={zoomFactor * mother.x + xOffset}
              y1={zoomFactor * mother.y}
              x2={zoomFactor * parentsX + xOffset}
              y2={zoomFactor * mother.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={zoomFactor * parentsX + xOffset}
              y1={zoomFactor * mother.y}
              x2={zoomFactor * parentsX + xOffset}
              y2={zoomFactor * father.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={zoomFactor * father.x + xOffset}
              y1={zoomFactor * father.y}
              x2={zoomFactor * parentsX + xOffset}
              y2={zoomFactor * father.y}
              class="stroke-node stroke-line"
            />
          {/if}

          {#if children.length > 0}
            <!-- Draw line between parents and children -->
            {@const parentsY = Math.max(mother.y, father.y)}
            {@const childrenY = Math.min(...children.map((child) => child?.y ?? Infinity))}
            {@const midY = (parentsY + childrenY) / 2}
            <line
              x1={zoomFactor * parentsX + xOffset}
              y1={zoomFactor * parentsY}
              x2={zoomFactor * parentsX + xOffset}
              y2={zoomFactor * midY}
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
              x1={zoomFactor * leftChildX + xOffset}
              y1={zoomFactor * midY}
              x2={zoomFactor * rightChildX + xOffset}
              y2={zoomFactor * midY}
              class="stroke-node stroke-line"
            />

            <!-- Draw line from each child to children line -->
            {#each children as child}
              {#if child}
                <line
                  x1={zoomFactor * child.x + xOffset}
                  y1={zoomFactor * midY}
                  x2={zoomFactor * child.x + xOffset}
                  y2={zoomFactor * child.y}
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
          <g transform="translate({zoomFactor * position.x + xOffset},{zoomFactor * position.y})">
            <rect
              x={(-RECT_WIDTH * zoomFactor) / 2}
              y={(-RECT_HEIGHT * zoomFactor) / 2}
              width={RECT_WIDTH * zoomFactor}
              height={RECT_HEIGHT * zoomFactor}
              rx={RECT_RADIUS * zoomFactor}
              class="fill-node"
            ></rect>
            <foreignObject
              x={(-RECT_WIDTH * zoomFactor) / 2}
              y={(-RECT_HEIGHT * zoomFactor) / 2}
              width={RECT_WIDTH * zoomFactor}
              height={RECT_HEIGHT * zoomFactor}
            >
              <button
                onclick={() => getPersonInfo(id, person.name)}
                style="font-size: {16 * zoomFactor}px;"
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
