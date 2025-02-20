<script lang="ts">
  import * as d3 from 'd3';
  import { balanceTree } from '$lib';
  import { zip } from '$lib/utils';
  import type { Marriage, Positions, Tree } from '$lib/types';
  import { onMount } from 'svelte';
  import { SvelteSet } from 'svelte/reactivity';

  const { tree, getPersonInfo }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } =
    $props();
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
  let selectedID = $state('');
  const highlightSet = new SvelteSet<string>();

  function handleClick(id: string, name: string) {
    selectedID = id; // Update selected person ID
    getPersonInfo(id, name); // Fetch person info

    highlightSet.clear();
    highlightSet.add(id);

    if (!visMarriages) return;

    visMarriages.forEach((marriage) => {
      if (marriage[0].children.includes(id)) {
        highlightSet.add(marriage[0].parents[0]);
        highlightSet.add(marriage[0].parents[1]);
      }

      if (marriage[0].parents.includes(id)) {
        marriage[0].children.forEach((child) => highlightSet.add(child));
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
          {@const motherID = marriage[0].parents[0]}
          {@const fatherID = marriage[0].parents[1]}
          {@const mother = positions[motherID]}
          {@const father = positions[fatherID]}
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
                class="{highlightSet.has(motherID) && highlightSet.has(fatherID)
                  ? 'stroke-highlight_border'
                  : 'stroke-node'} stroke-line"
              />

              {#if (motherID === selectedID || fatherID === selectedID) && !highlightSet.isDisjointFrom(new Set(marriage[0].children))}
                {@const startPoint = selectedID === motherID ? mother.x : father.x}
                <line
                  x1={startPoint}
                  y1={mother.y}
                  x2={parentsX}
                  y2={father.y}
                  class="stroke-highlight_border stroke-line"
                />
              {/if}
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
                class="{motherID === selectedID ||
                fatherID === selectedID ||
                marriage[0].children.includes(selectedID)
                  ? 'stroke-highlight_border'
                  : 'stroke-node'} stroke-line"
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

              {#if motherID === selectedID || fatherID === selectedID}
                <line
                  x1={leftChildX}
                  y1={midY}
                  x2={rightChildX}
                  y2={midY}
                  class="stroke-highlight_border stroke-line"
                />
              {:else}
                <line
                  x1={leftChildX}
                  y1={midY}
                  x2={rightChildX}
                  y2={midY}
                  class="stroke-node stroke-line"
                />
              {/if}

              <!-- Draw line from each child to children line -->
              {#each zip(children, marriage[0].children) as childAndID}
                {@const child = childAndID[0]}
                {@const childID = childAndID[1]}
                {#if child}
                  {#if childID === selectedID}
                    <line
                      x1={child.x}
                      y1={midY}
                      x2={parentsX}
                      y2={midY}
                      class="stroke-highlight_border stroke-line"
                    />
                  {/if}

                  <line
                    x1={child.x}
                    y1={midY}
                    x2={child.x}
                    y2={child.y}
                    class="{motherID === selectedID ||
                    fatherID === selectedID ||
                    childID === selectedID
                      ? 'stroke-highlight_border'
                      : 'stroke-node'} stroke-line"
                  />
                {/if}
              {/each}
            {/if}
          {/if}
        {/each}
        {#each tree.people as [id, person, gender]}
          {@const position = positions[id]}
          {#if position}
            <g transform="translate({position.x},{position.y})">
              <rect
                x={-RECT_WIDTH / 2}
                y={-RECT_HEIGHT / 2}
                width={RECT_WIDTH}
                height={RECT_HEIGHT}
                rx={RECT_RADIUS}
                class="{tree.people[0][0] === id
                  ? 'fill-highlight'
                  : gender === 'male'
                    ? 'fill-blue'
                    : gender === 'female'
                      ? 'fill-red'
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
                  class="flex h-full w-full cursor-pointer items-center justify-center text-center text-sm {tree
                    .people[0][0] === id
                    ? 'text-white'
                    : ''}"
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
