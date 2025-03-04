<script lang="ts">
  import * as d3 from 'd3';
  import { positionNodes } from '$lib';
  import {
    type People,
    type MarriageDistances,
    type MarriageHeights,
    type Marriages,
    type Positions,
    type Tree,
    type PersonID
  } from '$lib/types';
  import { onMount } from 'svelte';
  import FamilyTreeLines from './FamilyTreeLines.svelte';
  import { SvelteSet } from 'svelte/reactivity';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let positions = $state<Positions>({});
  let treeWidth = $state<number>();
  let marriageHeights = $state<MarriageHeights>([]);
  let marriageDistances = $state<MarriageDistances>([]);
  let marriageOffsets = $state<number[]>([]);
  let marriages = $state<Marriages>([]);
  let people = $state<People>([]);
  let focus = $state<string>('');

  $effect(() => {
    if (tree) {
      const result = positionNodes(tree);

      // to make sure marriage lines update after new positions calculated
      people = tree.people;
      marriages = tree.marriages;
      focus = tree.focus;

      positions = result.positions;
      treeWidth = result.treeWidth;
      marriageHeights = result.marriageHeights;
      marriageDistances = result.marriageDistances;
      marriageOffsets = result.marriageOffsets;
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
  let selectedID = $state('');
  const highlightSet = new SvelteSet<string>();

  function handleClick(id: string, name: string) {
    selectedID = id; // Update selected person ID
    getPersonInfo(id, name); // Fetch person info

    highlightSet.clear();
    highlightSet.add(id);

    if (!tree) return;

    marriages.forEach((marriage) => {
      if (marriage.children.includes(id)) {
        highlightSet.add(marriage.parents[0]);
        highlightSet.add(marriage.parents[1]);
      }

      if (marriage.parents.includes(id)) {
        marriage.children.forEach((child) => highlightSet.add(child));
      }
    });
  }

  function closeSidePanel() {
    highlightSet.clear();
    selectedID = '';
  }

  export { closeSidePanel };

  const zoomFactor = $derived(Math.min(1, treeWidth ? width / treeWidth : 1));

  const xOffset = $derived(
    treeWidth !== undefined && treeWidth < width ? (width - treeWidth) / 2 : width / 2
  );

  const yOffset = $derived(height / 2);
</script>

<svg id="svg-root" class="h-full w-full" bind:clientWidth={width} bind:clientHeight={height}>
  <g id="zoom-group">
    <g transform="translate({xOffset}, {yOffset}) scale({zoomFactor})">
      {#if tree}
        {#each marriages as marriage, i (`${focus} ${i}`)}
          <FamilyTreeLines
            {marriage}
            {positions}
            height={marriageHeights[i]!}
            distance={marriageDistances[i]!}
            offset={marriageOffsets[i]!}
            {highlightSet}
            {selectedID}
          />
        {/each}
        {#each people as [id, person, gender] (id)}
          {@const position = positions[id]}
          {#if position}
            <g
              transform="translate({position.x},{position.y})"
              class="transition-transform duration-200"
            >
              <rect
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
                rx={RECT_RADIUS}
                class="{people[0][0] === id
                  ? 'fill-highlight'
                  : gender === 'male'
                    ? 'fill-blue'
                    : gender === 'female'
                      ? 'fill-pink'
                      : 'fill-node'} {highlightSet.has(id)
                  ? 'stroke-highlight_border stroke-line'
                  : ''}"
              ></rect>
              <foreignObject
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
              >
                <button
                  onclick={() => handleClick(id, person.name)}
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
