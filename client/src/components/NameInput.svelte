<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';
  import IconAlert from '~icons/tabler/alert-triangle-filled';
  import type { Status } from '$lib/status';
  import { scale } from 'svelte/transition';

  let { nameInput = $bindable(), status }: { nameInput?: string; status: Status } = $props();

  let name = $state('');
  let searching = $state(false);
  let timer: number | null = null;
  $effect(() => {
    if (name) {
      searching = true;
      timer && clearTimeout(timer);
      timer = setTimeout(searchByName, 500);

      return () => {
        timer && clearTimeout(timer);
        searching = false;
      };
    }
  })
  function searchByName() {
    console.log('searching by name', name);
    searching = false;
  }

  function submitIfEnter(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      submitAction();
    }
  }

  function submitAction() {
    nameInput = name;
  }

  // common tailwind classes for status icons
  const ICON_CLASS = 'absolute right-3 flex h-full w-5 items-center';
  const HOVER_CLASS = 'scale-100 hover:scale-125 transition-transform duration-150';
</script>

<div class="relative flex w-80 items-center gap-3 rounded-full bg-input pl-4">
  <IconSearch class="text-black opacity-50" />
  <input
    class="flex-1 bg-transparent py-2 outline-none"
    bind:value={name}
    placeholder="Enter a name..."
    onkeydown={submitIfEnter}
  />
  {#if status.state === 'loading' || searching}
    <div class={`${ICON_CLASS}`} transition:scale={{ duration: 150 }}>
      <div class="loader h-5 w-5 bg-black p-1 opacity-50"></div>
    </div>
  {/if}
  {#if status.state === 'error'}
    <div class={ICON_CLASS}>
      <button class={`${HOVER_CLASS} peer z-10`} transition:scale={{ duration: 150 }}>
        <IconAlert class="text-red"></IconAlert>
      </button>
      <div
        class="absolute bottom-0 left-1/2 flex h-0 w-0 -translate-y-4 flex-col items-center opacity-0 transition-all peer-hover:translate-y-0 peer-hover:opacity-100"
      >
        <p class="mt-2 text-nowrap rounded-xl bg-red px-8 py-1 text-center text-sm text-white">
          {status.error}
        </p>
      </div>
    </div>
  {/if}
</div>
