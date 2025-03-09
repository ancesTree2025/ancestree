<script lang="ts">
  import FilterIcon from '~icons/tabler/filter';

  let {
    sibling = $bindable(),
    spousefamily = $bindable(),
    ancestor = $bindable(),
    descendant = $bindable(),
    unmarried = $bindable()
  }: {
    sibling?: boolean;
    spousefamily?: boolean;
    ancestor?: boolean;
    descendant?: boolean;
    unmarried?: boolean;
  } = $props();
</script>

<div class="flex flex-col gap-4">
  <div class="flex items-center text-base">
    <FilterIcon class="mr-2" />
    <h1>Filter Tree</h1>
  </div>
  <div class="flex flex-col gap-2">
    <label>
      <input class="mr-2" type="checkbox" bind:checked={sibling} />
      Show siblings
    </label>

    <label>
      <input class="mr-2" type="checkbox" bind:checked={spousefamily} />
      Show spouse families
    </label>

    <label>
      <input class="mr-2" type="checkbox" bind:checked={ancestor} />
      Show ancestors
    </label>

    <label>
      <input class="mr-2" type="checkbox" bind:checked={descendant} />
      Show descendants
    </label>

    <label>
      <input class="mr-2" type="checkbox" bind:checked={unmarried} />
      Show unmarried
    </label>
  </div>
</div>
