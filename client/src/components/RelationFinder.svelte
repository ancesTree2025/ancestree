<script lang="ts">
  import type { People } from '$lib/types';
  import NameInput from './NameInput.svelte';
  import PersonSearchIcon from '~icons/tabler/user-search';

  const {
    searchWithinTree,
    people,
    clearFilter
  }: { clearFilter: () => void; people: People; searchWithinTree: (result: string) => void } =
    $props();
</script>

<div class="flex flex-col gap-4">
  <div class="flex items-center text-base">
    <PersonSearchIcon class="mr-2" />
    <h1>Relation Finder</h1>
  </div>
  <NameInput
    onSubmit={searchWithinTree}
    displayAbove
    status={{ state: 'idle' }}
    namesInTree={people.map((p) => p[1].name)}
    clearSearch={clearFilter}
    type="RelationFinder"
    value={""}
  />
</div>
