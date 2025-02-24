<script lang="ts">
  import type { Marriage, Position, Positions } from '$lib/types';

  const { marriage, positions }: { marriage: Marriage; positions: Positions } = $props();

  const spouse1 = $derived(positions[marriage.parents[0]] as Position | undefined);
  const spouse2 = $derived(positions[marriage.parents[1]] as Position | undefined);
  const children = $derived(marriage.children.map((id) => positions[id] as Position | undefined));

  const parentsX = $derived(((spouse1?.x ?? 0) + (spouse2?.x ?? 0)) / 2);
  const parentsY = $derived(Math.max(spouse1?.y ?? Infinity, spouse2?.y ?? Infinity));

  const childrenY = $derived(Math.min(Infinity, ...children.map((child) => child?.y ?? Infinity)));
  const midY = $derived((parentsY + childrenY) / 2);
  const leftChildX = $derived(Math.min(parentsX, ...children.map((child) => child?.x ?? Infinity)));
  const rightChildX = $derived(
    Math.max(parentsX, ...children.map((child) => child?.x ?? -Infinity))
  );
</script>

{#if spouse1 && spouse2}
  <!-- Draw marriage lines -->
  {#if spouse1.y === spouse2.y}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse2.x}
      y2={spouse2.y}
      class="stroke-line-border stroke-rounded stroke-white"
    />
  {:else}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={parentsX}
      y2={spouse1.y}
      class="stroke-line-border stroke-rounded stroke-white"
    />
    <line
      x1={parentsX}
      y1={spouse1.y}
      x2={parentsX}
      y2={spouse2.y}
      class="stroke-line-border stroke-rounded stroke-white"
    />
    <line
      x1={spouse2.x}
      y1={spouse2.y}
      x2={parentsX}
      y2={spouse2.y}
      class="stroke-line-border stroke-rounded stroke-white"
    />
  {/if}

  {#if children.length > 0}
    <!-- Draw line between parents and children -->
    <line
      x1={parentsX}
      y1={parentsY}
      x2={parentsX}
      y2={midY}
      class="stroke-line-border stroke-rounded stroke-white"
    />

    <!-- Draw children line -->
    <line
      x1={leftChildX}
      y1={midY}
      x2={rightChildX}
      y2={midY}
      class="stroke-line-border stroke-rounded stroke-white"
    />

    <!-- Draw line from each child to children line -->
    {#each children as child}
      {#if child}
        <line
          x1={child.x}
          y1={midY}
          x2={child.x}
          y2={child.y}
          class="stroke-line-border stroke-rounded stroke-white"
        />
      {/if}
    {/each}
  {/if}
{/if}

{#if spouse1 && spouse2}
  <!-- Draw marriage lines -->
  {#if spouse1.y === spouse2.y}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={spouse2.x}
      y2={spouse2.y}
      class="stroke-node stroke-line stroke-rounded"
    />
  {:else}
    <line
      x1={spouse1.x}
      y1={spouse1.y}
      x2={parentsX}
      y2={spouse1.y}
      class="stroke-node stroke-line stroke-rounded"
    />
    <line
      x1={parentsX}
      y1={spouse1.y}
      x2={parentsX}
      y2={spouse2.y}
      class="stroke-node stroke-line stroke-rounded"
    />
    <line
      x1={spouse2.x}
      y1={spouse2.y}
      x2={parentsX}
      y2={spouse2.y}
      class="stroke-node stroke-line stroke-rounded"
    />
  {/if}

  {#if children.length > 0}
    <!-- Draw line between parents and children -->
    <line
      x1={parentsX}
      y1={parentsY}
      x2={parentsX}
      y2={midY}
      class="stroke-node stroke-line stroke-rounded"
    />

    <!-- Draw children line -->
    <line
      x1={leftChildX}
      y1={midY}
      x2={rightChildX}
      y2={midY}
      class="stroke-node stroke-line stroke-rounded"
    />

    <!-- Draw line from each child to children line -->
    {#each children as child}
      {#if child}
        <line
          x1={child.x}
          y1={midY}
          x2={child.x}
          y2={child.y}
          class="stroke-node stroke-line stroke-rounded"
        />
      {/if}
    {/each}
  {/if}
{/if}
