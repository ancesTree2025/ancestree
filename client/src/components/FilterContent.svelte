<script lang="ts">
  import type { FilterOption } from '$lib/types';
  import FilterIcon from '~icons/tabler/filter';

  const { setOption }: { setOption: (option: FilterOption, to: boolean) => void } = $props();

  let sibling = $state(true);
  let spousefamily = $state(true);
  let ancestor = $state(true);
  let descendant = $state(true);

  $effect(() => {
    setOption('sibling', sibling);
  });

  $effect(() => {
    setOption('spousefamily', spousefamily);
  });

  $effect(() => {
    setOption('ancestor', ancestor);
  });

  $effect(() => {
    setOption('descendant', descendant);
  });
</script>

<div class="flex flex-col gap-4">
  <div class="flex items-center text-base">
    <FilterIcon class="mr-2" />
    <h1>Filter Tree</h1>
    <div class="flex flex-col gap-2">
      <label>
        <input type="checkbox" bind:checked={sibling} />
        Show siblings
      </label>

      <label>
        <input type="checkbox" bind:checked={spousefamily} />
        Show spouse families
      </label>

      <label>
        <input type="checkbox" bind:checked={ancestor} />
        Show ancestors
      </label>

      <label>
        <input type="checkbox" bind:checked={descendant} />
        Show descendants
      </label>
    </div>
  </div>
</div>
