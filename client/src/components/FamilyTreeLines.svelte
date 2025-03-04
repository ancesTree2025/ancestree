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

  const rawSpouse1Id = $derived(marriage.parents[0]);
  const rawSpouse2Id = $derived(marriage.parents[1]);
  const rawSpouse1 = $derived<Position | undefined>(positions[rawSpouse1Id]);
  const rawSpouse2 = $derived<Position | undefined>(positions[rawSpouse2Id]);

  const swap = $derived(
    rawSpouse1 !== undefined && rawSpouse2 !== undefined && rawSpouse1.x > rawSpouse2.x
  );
  const spouse1 = $derived(swap ? rawSpouse2 : rawSpouse1);
  const spouse2 = $derived(swap ? rawSpouse1 : rawSpouse2);
  const spouse1Id = $derived(swap ? rawSpouse2Id : rawSpouse1Id);
  const spouse2Id = $derived(swap ? rawSpouse1Id : rawSpouse2Id);
  const children = $derived(
    marriage.children.map<[PersonID, Position | undefined]>((id) => [id, positions[id]])
  );

  const OVERLAP_OFFSET = 40;
  const HEIGHT_OFFSET = 20;

  const parentsX = $derived(((spouse1?.x ?? NaN) + (spouse2?.x ?? NaN)) / 2 + offset);
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
      x2={spouse1.x + spouse1.y - parentsY}
      y2={parentsY}
      class="{highlightSpouse1
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse1.x + spouse1.y - parentsY}
      y1={parentsY}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse1
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse2.x - spouse2.y + parentsY}
      y1={parentsY}
      x2={parentsX}
      y2={parentsY}
      class="{highlightSpouse2
        ? 'stroke-highlight_border'
        : 'stroke-node'} stroke-rounded stroke-line"
    />
    <line
      x1={spouse2.x - spouse2.y + parentsY}
      y1={parentsY}
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
    {#each children as [childId, childPos]}
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
