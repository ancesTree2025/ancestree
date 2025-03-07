<script lang="ts">
  import * as d3 from 'd3';
  import { positionNodes } from '$lib';
  import {
    type People,
    type Marriages,
    type Positions,
    type Tree,
    type MarriagePositions,
    type Position
  } from '$lib/types';
  import { onMount } from 'svelte';
  import FamilyTreeLines from './FamilyTreeLines.svelte';
  import { SvelteSet } from 'svelte/reactivity';
  import { scale } from 'svelte/transition';

  const {
    tree,
    getPersonInfo,
    expandNode,
    collapseNode
  }: {
    tree?: Tree;
    getPersonInfo: (qid: string, name: string, position: Position) => void;
    expandNode: (id: string, name: string, position: Position) => Promise<void>;
    collapseNode: (id: string) => void;
  } = $props();
  let positions = $state<Positions>({});
  let treeWidth = $state<number>();
  let marriagePositions = $state<MarriagePositions>([]);
  let marriages = $state<Marriages>([]);
  let people = $state<People>([]);

  /**
   * list of nodes where the loading must be shown.
   */
  let loadingStatusOnNode = $state<string[]>([]);

  $effect(() => {
    if (tree) {
      const result = positionNodes(tree);

      // to make sure marriage lines update after new positions calculated
      people = tree.people;
      marriages = tree.marriages;

      positions = result.positions;
      marriagePositions = result.marriagePositions;
      treeWidth = result.treeWidth;
    } else {
      positions = {};
    }
  });

  onMount(() => {
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    d3.select('#svg-root')
      .call(d3.zoom().on('zoom', zoomed) as any)
      .on('dblclick.zoom', null);

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

  export function handleClick(id: string, name: string, position: Position | null) {
    if (position) {
      recenter(position.x * -1, position.y * -1);
      position = { x: position.x * -1, y: position.y * -1 };
    } else {
      position = { x: 0, y: 0 };
    }

    selectedID = id; // Update selected person ID
    getPersonInfo(id, name, position); // Fetch person info

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

  export function recenter(x: number = 0, y: number = 0) {
    const svg = d3.select('#svg-root');
    const zoomGroup = d3.select('#zoom-group');

    const initialTransform = d3.zoomIdentity.translate(x, y).scale(1);

    svg
      .transition()
      .duration(500)
      .call(d3.zoom().transform as any, initialTransform);
    zoomGroup.attr('transform', initialTransform as any);
  }

  function closeSidePanel() {
    highlightSet.clear();
    selectedID = '';
  }

  export { closeSidePanel };

  const zoomFactor = $derived(1);

  const xOffset = $derived(
    treeWidth !== undefined && treeWidth < width ? (width - treeWidth) / 2 : width / 2
  );

  const yOffset = $derived(height / 2);

  async function onExpandNode(id: string, name: string) {
    loadingStatusOnNode = [...loadingStatusOnNode, id];
    if (tree?.secondary.includes(id)) {
      collapseNode(id);
    } else {
      await expandNode(id, name, positions[id]);
    }
    loadingStatusOnNode = loadingStatusOnNode.filter((_id) => id !== _id);
  }
</script>

<svg id="svg-root" class="h-full w-full" bind:clientWidth={width} bind:clientHeight={height}>
  <g id="zoom-group">
    <g transform="translate({xOffset}, {yOffset}) scale({zoomFactor})">
      {#each marriagePositions as marriagePosition}
        <FamilyTreeLines {marriagePosition} {highlightSet} {selectedID} />
      {/each}
      {#if tree}
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
                class="{gender === 'male'
                  ? tree.secondary.includes(id)
                    ? 'fill-dark-blue'
                    : 'fill-blue'
                  : gender === 'female'
                    ? tree.secondary.includes(id)
                      ? 'fill-dark-pink'
                      : 'fill-pink'
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
                  onclick={() => handleClick(id, person.name, position)}
                  ondblclick={() => onExpandNode(id, person.name)}
                  class="relative flex h-full w-full cursor-pointer items-center justify-center text-center text-sm {tree.secondary.includes(
                    id
                  )
                    ? 'text-white'
                    : 'text-black'}"
                >
                  {person.name}
                  {#if loadingStatusOnNode.includes(id)}
                    <span class="absolute grid h-full w-full place-items-center bg-gray opacity-70">
                      <div class="absolute right-1 top-2" transition:scale={{ duration: 150 }}>
                        <div class="loader h-5 w-5 bg-black p-1 opacity-50"></div>
                      </div>
                    </span>
                  {/if}
                </button>
              </foreignObject>
            </g>
          {/if}
        {/each}
      {/if}
    </g>
  </g>
</svg>
