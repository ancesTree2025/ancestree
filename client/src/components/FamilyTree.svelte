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
      d3.select('#zoom-group').attr('transform', event.transform);
    }
  });

  const RECT_HEIGHT = 60;
  const RECT_WIDTH = 120;
  const RECT_RADIUS = 10;

  let height = $state(0);
  let width = $state(0);

  let ZOOM_FACTOR = $derived(Math.min(1, treeWidth ? width / treeWidth : 1));

  let xOffset = $derived(
    treeWidth !== undefined && treeWidth < width ? (width - treeWidth) / 2 : 0
  );

  let transformX = (coord: number) => {
    let result = coord * ZOOM_FACTOR;
    if (treeWidth !== undefined && treeWidth < width) {
      result += (width - treeWidth) / 2;
    }
    return result;
  };

  let transformY = (coord: number) => ZOOM_FACTOR * coord;
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
              x1={ZOOM_FACTOR * mother.x + xOffset}
              y1={ZOOM_FACTOR * mother.y}
              x2={ZOOM_FACTOR * father.x + xOffset}
              y2={ZOOM_FACTOR * father.y}
              class="stroke-node stroke-line"
            />
          {:else}
            <line
              x1={ZOOM_FACTOR * mother.x + xOffset}
              y1={ZOOM_FACTOR * mother.y}
              x2={ZOOM_FACTOR * parentsX + xOffset}
              y2={ZOOM_FACTOR * mother.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={ZOOM_FACTOR * parentsX + xOffset}
              y1={ZOOM_FACTOR * mother.y}
              x2={ZOOM_FACTOR * parentsX + xOffset}
              y2={ZOOM_FACTOR * father.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={ZOOM_FACTOR * father.x + xOffset}
              y1={ZOOM_FACTOR * father.y}
              x2={ZOOM_FACTOR * parentsX + xOffset}
              y2={ZOOM_FACTOR * father.y}
              class="stroke-node stroke-line"
            />
          {/if}

          {#if children.length > 0}
            <!-- Draw line between parents and children -->
            {@const parentsY = Math.max(mother.y, father.y)}
            {@const childrenY = Math.min(...children.map((child) => child?.y ?? Infinity))}
            {@const midY = (parentsY + childrenY) / 2}
            <line
              x1={ZOOM_FACTOR * parentsX + xOffset}
              y1={ZOOM_FACTOR * parentsY}
              x2={ZOOM_FACTOR * parentsX + xOffset}
              y2={ZOOM_FACTOR * midY}
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
              x1={ZOOM_FACTOR * leftChildX + xOffset}
              y1={ZOOM_FACTOR * midY}
              x2={ZOOM_FACTOR * rightChildX + xOffset}
              y2={ZOOM_FACTOR * midY}
              class="stroke-node stroke-line"
            />

            <!-- Draw line from each child to children line -->
            {#each children as child}
              {#if child}
                <line
                  x1={ZOOM_FACTOR * child.x + xOffset}
                  y1={ZOOM_FACTOR * midY}
                  x2={ZOOM_FACTOR * child.x + xOffset}
                  y2={ZOOM_FACTOR * child.y}
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
          <g transform="translate({ZOOM_FACTOR * position.x + xOffset},{ZOOM_FACTOR * position.y})">
            <rect
              x={(-RECT_WIDTH * ZOOM_FACTOR) / 2}
              y={(-RECT_HEIGHT * ZOOM_FACTOR) / 2}
              width={RECT_WIDTH * ZOOM_FACTOR}
              height={RECT_HEIGHT * ZOOM_FACTOR}
              rx={RECT_RADIUS * ZOOM_FACTOR}
              class="fill-node"
            ></rect>
            <foreignObject
              x={(-RECT_WIDTH * ZOOM_FACTOR) / 2}
              y={(-RECT_HEIGHT * ZOOM_FACTOR) / 2}
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
