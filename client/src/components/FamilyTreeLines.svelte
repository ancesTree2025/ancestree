<script lang="ts">
  import type { MarriagePosition, Position } from '$lib/types';
  import type { SvelteSet } from 'svelte/reactivity';

  const {
    marriagePosition,
    highlightSet,
    selectedID
  }: {
    marriagePosition: MarriagePosition;
    highlightSet: SvelteSet<string>;
    selectedID: string;
  } = $props();

  let spouse1 = $state<Position>({ x: 0, y: 0 });
  let spouse2 = $state<Position>({ x: 0, y: 0 });
  let spouse1Id = $state<string>('');
  let spouse2Id = $state<string>('');

  $effect(() => {
    const swap = marriagePosition.parent1.x > marriagePosition.parent2.x;
    spouse1 = swap ? marriagePosition.parent2 : marriagePosition.parent1;
    spouse2 = swap ? marriagePosition.parent1 : marriagePosition.parent2;
    spouse1Id = swap ? marriagePosition.parent2ID : marriagePosition.parent1ID;
    spouse2Id = swap ? marriagePosition.parent1ID : marriagePosition.parent2ID;
  });

  const OVERLAP_OFFSET = 40;
  const HEIGHT_OFFSET = 20;

  const parentsX = $derived((spouse1.x + spouse2.x) / 2 + marriagePosition.offset);
  const rawParentsY = $derived(Math.max(spouse1.y, spouse2.y));
  const parentsY = $derived(
    rawParentsY -
      (marriagePosition.distance === 1
        ? 0
        : OVERLAP_OFFSET + HEIGHT_OFFSET * (marriagePosition.distance - 2))
  );

  const childrenY = $derived(
    Math.min(Infinity, ...marriagePosition.children.map((child) => child.y)) -
      marriagePosition.height * HEIGHT_OFFSET
  );
  const midY = $derived((rawParentsY + childrenY) / 2);
  const leftChildX = $derived(
    Math.min(parentsX, ...marriagePosition.children.map((child) => child.x))
  );
  const rightChildX = $derived(
    Math.max(parentsX, ...marriagePosition.children.map((child) => child.x))
  );

  const dasharray = $derived(
    marriagePosition.type === 'married' ? 'stroke-rounded' : '[stroke-dasharray:10,10]'
  );
</script>

{#if spouse1 && spouse2}
  <!-- Draw marriage line -->
  {#if marriagePosition.distance === 1}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse2.x}
      y2={spouse2.y}
      class="stroke-rounded stroke-white stroke-line-border"
    />
  {:else}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse1.x + spouse1.y - parentsY}
      y2={parentsY}
      class="stroke-rounded stroke-white stroke-line-border"
    />
    <line
      x1={spouse1.x + spouse1.y - parentsY}
      y1={parentsY}
      x2={spouse2.x - spouse2.y + parentsY}
      y2={parentsY}
      class="stroke-rounded stroke-white stroke-line-border"
    />
    <line
      x1={spouse2.x - spouse2.y + parentsY}
      y1={parentsY}
      x2={spouse2.x}
      y2={spouse2.y}
      class="stroke-rounded stroke-white stroke-line-border"
    />
  {/if}

  {#if marriagePosition.children.length > 0}
    <!-- Draw line between parents and children -->
    <line
      x1={parentsX}
      y1={parentsY}
      x2={parentsX}
      y2={midY}
      class="stroke-rounded stroke-white stroke-line-border"
    />

    <!-- Draw children line -->
    <line
      x1={leftChildX}
      y1={midY}
      x2={rightChildX}
      y2={midY}
      class="stroke-rounded stroke-white stroke-line-border"
    />

    <!-- Draw line from each child to children line -->
    {#each marriagePosition.children as childPos}
      <line
        x1={childPos.x}
        y1={midY}
        x2={childPos.x}
        y2={childPos.y}
        class="stroke-rounded stroke-white stroke-line-border"
      />
    {/each}
  {/if}

  {@const highlightMarriage = highlightSet.has(spouse1Id) && highlightSet.has(spouse2Id)}
  {@const highlightHalf = !highlightSet.isDisjointFrom(new Set(marriagePosition.childrenIDs))}
  {@const highlightSpouse1 = highlightMarriage || (spouse1Id === selectedID && highlightHalf)}
  {@const highlightSpouse2 = highlightMarriage || (spouse2Id === selectedID && highlightHalf)}

  <!-- Draw marriage line -->
  {#if marriagePosition.distance === 1}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse1 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
    <line
      x1={spouse2.x}
      y1={spouse2.y}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse2 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
  {:else}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse1.x + spouse1.y - parentsY}
      y2={parentsY}
      class="{highlightSpouse1 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
    <line
      x1={spouse1.x + spouse1.y - parentsY}
      y1={parentsY}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse1 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
    <line
      x1={spouse2.x - spouse2.y + parentsY}
      y1={parentsY}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse2 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
    <line
      x1={spouse2.x - spouse2.y + parentsY}
      y1={parentsY}
      x2={spouse2.x}
      y2={spouse2.y}
      class="{highlightSpouse2 ? 'stroke-highlight_border' : 'stroke-node'} stroke-line {dasharray}"
    />
  {/if}

  {#if marriagePosition.children.length > 0}
    {@const highlightChildren = spouse1Id === selectedID || spouse2Id === selectedID}
    <!-- Draw line between parents and children -->
    <line
      x1={parentsX}
      y1={parentsY}
      x2={parentsX}
      y2={midY}
      class="{highlightChildren || marriagePosition.childrenIDs.includes(selectedID)
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />

    <!-- Draw children line -->
    <line
      x1={leftChildX}
      y1={midY}
      x2={rightChildX}
      y2={midY}
      class="{highlightChildren
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />

    <!-- Draw line from each child to children line -->
    {#each marriagePosition.children as childPos, i}
      {@const childId = marriagePosition.childrenIDs[i]}
      {#if childPos}
        {@const selectedChild = childId === selectedID}
        {#if selectedChild}
          <line
            x1={childPos.x}
            y1={midY}
            x2={parentsX}
            y2={midY}
            class="stroke-rounded stroke-highlight_border stroke-line"
          />
        {/if}
        <line
          x1={childPos.x}
          y1={midY}
          x2={childPos.x}
          y2={childPos.y}
          class="{highlightChildren || selectedChild
            ? 'stroke-highlight_border'
            : 'stroke-node'} stroke-rounded stroke-line"
        />
      {/if}
    {/each}
  {/if}
{/if}
