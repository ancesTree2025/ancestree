<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';
  import IconAlert from '~icons/tabler/alert-triangle-filled';
  import { fade, scale } from 'svelte/transition';

  let {
    nameInput = $bindable(),
    loading,
    error
  }: { nameInput?: string; loading: boolean; error?: string } = $props();

  let name = $state('');

  function submitIfEnter(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      submitAction();
    }
  }

  function submitAction() {
    nameInput = name;
  }
</script>

<div class="bg-input flex w-80 items-center gap-3 rounded-full pl-4">
  <IconSearch class="text-black opacity-50" />
  <input
    class="flex-1 bg-transparent py-2 outline-none"
    bind:value={name}
    placeholder="Enter a name..."
    onkeydown={submitIfEnter}
  />
  {#if loading}
    <div transition:scale class="mr-3 flex h-5 w-5 items-center">
      <div class="loader h-5 w-5 bg-black p-1 opacity-50"></div>
    </div>
  {/if}
  {#if error}
    <div transition:scale class="mr-3 flex h-5 w-5 items-center">
      <IconAlert class="text-red" />
    </div>
  {/if}
</div>
