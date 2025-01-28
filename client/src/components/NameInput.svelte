<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';
  import IconAlert from '~icons/tabler/alert-triangle-filled';
  import { scale } from 'svelte/transition';

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

<div class="bg-input relative flex w-80 items-center gap-3 rounded-full pl-4">
  <IconSearch class="text-black opacity-50" />
  <input
    class="flex-1 bg-transparent py-2 outline-none"
    bind:value={name}
    placeholder="Enter a name..."
    onkeydown={submitIfEnter}
  />
  {#if loading}
    <div class="absolute right-3 flex h-full w-5 items-center">
      <div
        transition:scale={{ duration: 150 }}
        class="loader h-5 w-5 bg-black p-1 opacity-50"
      ></div>
    </div>
  {/if}
  {#if error}
    <div class="absolute right-3 flex h-full w-5 items-center">
      <button
        transition:scale={{ duration: 150 }}
        class="text-red peer z-10 transition-transform duration-150 hover:scale-125"
      >
        <IconAlert />
      </button>
      <div
        class="absolute bottom-0 left-1/2 flex h-0 w-0 -translate-y-4 flex-col items-center opacity-0 transition-all peer-hover:translate-y-0 peer-hover:opacity-100"
      >
        <p class="bg-red mt-2 text-nowrap rounded-xl px-8 py-1 text-center text-sm text-white">
          {error}
        </p>
      </div>
    </div>
  {/if}
</div>
