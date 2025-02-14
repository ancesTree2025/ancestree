<script lang="ts">
  import * as d3 from 'd3';
  import { balanceTree } from '$lib';
  import type { Marriage, Positions, Tree } from '$lib/types';
  import { onMount } from 'svelte';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let positions = $state<Positions>({});
  let visMarriages = $state<[Marriage, number][] | undefined>(tree?.marriages.map((m) => [m, 0]));
  let treeWidth = $state<number>();

  $effect(() => {
    if (tree) {
      const result = balanceTree(tree);
      positions = result.positions;
      visMarriages = result.visMarriages;
      treeWidth = result.treeWidth;
    } else {
      positions = {};
    }
  });

  onMount(() => {
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

  const yOffset = $derived(height / 2);
</script>

<svg id="svg-root" class="h-full w-full" bind:clientWidth={width} bind:clientHeight={height}>
  <g id="zoom-group">
    <g transform="translate({xOffset}, {yOffset}) scale({zoomFactor})">
      {#if tree && visMarriages}
        {#each visMarriages as marriage}
          <!-- fetch Person for each parent, child -->
          {@const mother = positions[marriage[0].parents[0]]}
          {@const father = positions[marriage[0].parents[1]]}
          {@const children = marriage[0].children.map((id) => positions[id])}

          {#if mother && father}
            <!-- Draw marriage lines -->
            {@const parentsX = (mother.x + father.x) / 2}
            {#if mother.y === father.y}
              <line
                x1={mother.x}
                y1={mother.y}
                x2={father.x}
                y2={father.y}
                class="stroke-node stroke-line"
              />
            {:else}
              <line
                x1={mother.x}
                y1={mother.y}
                x2={parentsX}
                y2={mother.y}
                class="stroke-node stroke-line"
              />
              <line
                x1={parentsX}
                y1={mother.y}
                x2={parentsX}
                y2={father.y}
                class="stroke-node stroke-line"
              />
              <line
                x1={father.x}
                y1={father.y}
                x2={parentsX}
                y2={father.y}
                class="stroke-node stroke-line"
              />
            {/if}

            {#if children.length > 0}
              <!-- Draw line between parents and children -->
              {@const parentsY = Math.max(mother.y, father.y)}
              {@const childrenY = Math.min(...children.map((child) => child?.y ?? Infinity))}
              {@const midY = (parentsY + childrenY) / 2 - (marriage[1] % 2 === 0 ? 0 : 10)}
              <line
                x1={parentsX}
                y1={parentsY}
                x2={parentsX}
                y2={midY}
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
                x1={leftChildX}
                y1={midY}
                x2={rightChildX}
                y2={midY}
                class="stroke-node stroke-line"
              />

              <!-- Draw line from each child to children line -->
              {#each children as child}
                {#if child}
                  <line
                    x1={child.x}
                    y1={midY}
                    x2={child.x}
                    y2={child.y}
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
            <g transform="translate({position.x},{position.y})">
              {#if tree.people[0][0] === id}
                <rect
                  x={-RECT_WIDTH / 2}
                  y={-RECT_HEIGHT / 2}
                  width={RECT_WIDTH}
                  height={RECT_HEIGHT}
                  rx={RECT_RADIUS}
                  class="fill-[#B08E55]"
                ></rect>
              {:else}
                <rect
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
                rx={RECT_RADIUS}
                class="fill-node"
                ></rect>
              {/if}
              <foreignObject
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
              >
                {#if tree.people[0][0] === id}
                  <button
                    onclick={() => getPersonInfo(id, person.name)}
                    class="flex h-full w-full cursor-pointer items-center justify-center text-center text-sm text-white"
                  >
                    {person.name}
                  </button>
                {:else}
                  <button
                    onclick={() => getPersonInfo(id, person.name)}
                    class="flex h-full w-full cursor-pointer items-center justify-center text-center text-sm"
                  >
                    {person.name}
                  </button>
                {/if}
              </foreignObject>
            </g>
          {/if}
        {/each}
      {/if}
    </g>
  </g>
</svg>
