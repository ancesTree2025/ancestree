<script lang="ts">
  import type { LoadingStatus, People } from '$lib/types';
  import NameInput from './NameInput.svelte';
  import PersonSearchIcon from '~icons/tabler/user-search';

  const {
    searchWithinTree,
    people,
    status,
    relationDescriptor,
    clearFilter
  }: {
    clearFilter: () => void;
    people: People;
    relationDescriptor?: string;
    status: LoadingStatus;
    searchWithinTree: (result: string) => void;
  } = $props();

  let nameInput = $state<NameInput>();

  export function clear() {
    nameInput?.clear();
  }
</script>

<div class="flex flex-col gap-4">
  <div class="flex items-center text-base">
    <PersonSearchIcon class="mr-2" />
    <h1>Relation Finder</h1>
  </div>
  <NameInput
    bind:this={nameInput}
    onSubmit={searchWithinTree}
    displayAbove={false}
    {status}
    namesInTree={people.map((p) => p[1].name)}
    clearSearch={clearFilter}
    type="RelationFinder"
    value=""
  />
  {#if relationDescriptor}
    <div>{relationDescriptor}</div>
  {/if}
</div>
