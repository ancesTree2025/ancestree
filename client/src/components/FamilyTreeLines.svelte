<script lang="ts">
  import type { Marriage, PersonID, Position, Positions } from '$lib/types';
  import type { SvelteSet } from 'svelte/reactivity';

  const {
    marriage,
    positions,
    height,
    distance,
    offset,
    highlightSet,
    selectedID
  }: {
    marriage: Marriage;
    positions: Positions;
    height: number;
    distance: number;
    offset: number;
    highlightSet: SvelteSet<string>;
    selectedID: string;
  } = $props();

  const spouse1Id = $derived(marriage.parents[0]);
  const spouse2Id = $derived(marriage.parents[1]);

  const rawSpouse1 = $derived(positions[spouse1Id] as Position | undefined);
  const rawSpouse2 = $derived(positions[spouse2Id] as Position | undefined);
  const spouse1 = $derived(
    rawSpouse1 && rawSpouse2 && rawSpouse1?.x < rawSpouse2?.x ? rawSpouse1 : rawSpouse2
  );
  const spouse2 = $derived(
    rawSpouse1 && rawSpouse2 && rawSpouse1?.x < rawSpouse2?.x ? rawSpouse2 : rawSpouse1
  );
  const children = $derived(
    marriage.children.map((id) => [id, positions[id]] as [PersonID, Position | undefined])
  );

  const OVERLAP_OFFSET = 40;
  const HEIGHT_OFFSET = 20;

  const parentsX = $derived(((spouse1?.x ?? 0) + (spouse2?.x ?? 0)) / 2 + offset);
  const rawParentsY = $derived(Math.max(spouse1?.y ?? Infinity, spouse2?.y ?? Infinity));
  const parentsY = $derived(
    rawParentsY - (distance === 1 ? 0 : OVERLAP_OFFSET + HEIGHT_OFFSET * (distance - 2))
  );

  const childrenY = $derived(
    Math.min(Infinity, ...children.map((child) => child[1]?.y ?? Infinity)) - height * HEIGHT_OFFSET
  );
  const midY = $derived((rawParentsY + childrenY) / 2);
  const leftChildX = $derived(
    Math.min(parentsX, ...children.map((child) => child[1]?.x ?? Infinity))
  );
  const rightChildX = $derived(
    Math.max(parentsX, ...children.map((child) => child[1]?.x ?? -Infinity))
  );
</script>

{#if spouse1 && spouse2}
  <!-- Draw marriage line -->
  {#if distance === 1}
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
      x2={spouse1.x + OVERLAP_OFFSET}
      y2={spouse1.y - OVERLAP_OFFSET}
      class="stroke-rounded stroke-white stroke-line-border"
    />
    <line
      x1={spouse1.x + OVERLAP_OFFSET}
      y1={spouse1.y - OVERLAP_OFFSET}
      x2={spouse2.x - OVERLAP_OFFSET}
      y2={spouse2.y - OVERLAP_OFFSET}
      class="stroke-rounded stroke-white stroke-line-border"
    />
    <line
      x1={spouse2.x - OVERLAP_OFFSET}
      y1={spouse2.y - OVERLAP_OFFSET}
      x2={spouse2.x}
      y2={spouse2.y}
      class="stroke-rounded stroke-white stroke-line-border"
    />
  {/if}

  {#if children.length > 0}
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
    {#each children as child}
      {@const childPos = child[1]}
      {#if childPos}
        <line
          x1={childPos.x}
          y1={midY}
          x2={childPos.x}
          y2={childPos.y}
          class="stroke-rounded stroke-white stroke-line-border"
        />
      {/if}
    {/each}
  {/if}
{/if}

{#if spouse1 && spouse2}
  {@const parentsY = (spouse1.y + spouse2.y) / 2}
  {@const highlightMarriage = highlightSet.has(spouse1Id) && highlightSet.has(spouse2Id)}
  {@const highlightHalf = !highlightSet.isDisjointFrom(new Set(marriage.children))}
  {@const highlightSpouse1 = highlightMarriage || (spouse1Id === selectedID && highlightHalf)}
  {@const highlightSpouse2 = highlightMarriage || (spouse2Id === selectedID && highlightHalf)}

  <!-- Draw marriage line -->
  {#if distance === 1}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse1
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse2.x}
      y1={spouse2.y}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse2
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
  {:else}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse1.x + OVERLAP_OFFSET}
      y2={spouse1.y - OVERLAP_OFFSET}
      class="{highlightSpouse1
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse1.x + OVERLAP_OFFSET}
      y1={spouse1.y - OVERLAP_OFFSET}
      x2={parentsX - OVERLAP_OFFSET}
      y2={parentsY - OVERLAP_OFFSET}
      class="{highlightSpouse1
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse2.x - OVERLAP_OFFSET}
      y1={spouse2.y - OVERLAP_OFFSET}
      x2={parentsX - OVERLAP_OFFSET}
      y2={parentsY - OVERLAP_OFFSET}
      class="{highlightSpouse2
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse2.x - OVERLAP_OFFSET}
      y1={spouse2.y - OVERLAP_OFFSET}
      x2={spouse2.x}
      y2={spouse2.y}
      class="{highlightSpouse2
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
  {/if}

  {#if children.length > 0}
    {@const highlightChildren = spouse1Id === selectedID || spouse2Id === selectedID}
    <!-- Draw line between parents and children -->
    <line
      x1={parentsX}
      y1={parentsY}
      x2={parentsX}
      y2={midY}
      class="{highlightChildren || marriage.children.includes(selectedID)
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
    {#each children as child}
      {@const childPos = child[1]}
      {#if childPos}
        {#if child[0] === selectedID}
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
          class="{highlightChildren || child[0] === selectedID
            ? 'stroke-highlight_border'
            : 'stroke-node'} stroke-rounded stroke-line"
        />
      {/if}
    {/each}
  {/if}
{/if}
