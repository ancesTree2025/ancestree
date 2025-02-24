<script lang="ts">
  import * as d3 from 'd3';
  import { positionNodes } from '$lib';
  import type { Positions, Tree } from '$lib/types';
  import { onMount } from 'svelte';
  import FamilyTreeLines from './FamilyTreeLines.svelte';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let positions = $state<Positions>({});
  let treeWidth = $state<number>();

  $effect(() => {
    if (tree) {
      const result = positionNodes(tree);
      positions = result.positions;
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
      {#if tree}
        {#each tree.marriages as marriage}
          <FamilyTreeLines {marriage} {positions} />
        {/each}
        {#each tree.people as [id, person]}
          {@const position = positions[id]}
          {#if position}
            <g transform="translate({position.x},{position.y})">
              <rect
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
                rx={RECT_RADIUS}
                class="fill-node"
              ></rect>
              <foreignObject
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
              >
                <button
                  onclick={() => getPersonInfo(id, person.name)}
                  class="flex h-full w-full cursor-pointer items-center justify-center text-center text-sm"
                >
                  {person.name}
                </button>
              </foreignObject>
            </g>
          {/if}
        {/each}
      {/if}
    </g>
  </g>
</svg>
