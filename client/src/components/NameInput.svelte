<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';
  import IconAlert from '~icons/tabler/alert-triangle-filled';
  import type { Status } from '$lib/status';
  import { scale } from 'svelte/transition';
  import { fetchNames } from '$lib';

  let { status, onSubmit }: { status: Status, onSubmit: (_: string) => void } = $props();

  let name = $state('');

  /**
   * A loading state to notify the user when
   * the search for autocomplete suggestions is in progress
   */
  let searching = $state(false);
  /**
   * The query the user uses to search a person.
   * This is a duplicate of the name state, but helps
   * in preventing a re-trigger.
   */
  let searchQuery = $state('');
  /**
   * A list of search results to show as autocomplete suggestions
   */
  let searchResults = $state<string[]>([]);
  /**
   * A timer to debounce the search request
   * Debouncing is done to prevent making too many requests.
   * The search request is made only after the user has stopped typing for 500ms.
   */
  $effect(() => {
    if (searchQuery) {
      searching = true;
      const timer = setTimeout(searchForNames, 500);

      // On cleanup, clear the timer and set searching to false
      return () => {
        if (timer) clearTimeout(timer);
        searching = false;
      };
    } else {
      searching = false;
      searchResults = [];
    }
  });
  async function searchForNames() {
    const result = await fetchNames(searchQuery);
    try {
      searchResults = result.getOrThrow();
    } catch (_e) {
      // TODO: handle error
    } finally {
      searching = false;
    }
  }
  /**
   * Handler to select a name from the autocomplete suggestions
   * @param selectedName the chosen name from the autocomplete suggestions
   */
  function selectName(selectedName: string) {
    searchResults = [];
    name = selectedName; // Replaces the typed name with the selected name
    searchQuery = '';
    onSubmit(name);
  }

  function onChange(event: Event) {
    const { value } = event.target as HTMLInputElement;
    name = value;
    searchQuery = value;
  }

  function submitIfEnter(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      onSubmit(name);
    }
  }

  // common tailwind classes for status icons
  const ICON_CLASS = 'absolute right-3 flex h-full w-5 items-center';
  const HOVER_CLASS = 'scale-100 hover:scale-125 transition-transform duration-150';
</script>

<div class="relative flex w-80 items-center gap-3 rounded-full bg-input pl-4">
  <IconSearch class="text-black opacity-50" />
  <form onsubmit={() => onSubmit(name)}>
    <input
      class="flex-1 bg-transparent py-2 outline-none"
      value={name}
      oninput={onChange}
      placeholder="Enter a name..."
      onkeydown={submitIfEnter}
    />
  </form>
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
  {#if searchResults.length}
    <div class="absolute left-0 right-0 top-full mx-5">
      <div class="rounded-lg bg-white shadow-lg">
        {#each searchResults as result}
          <button
            class="block w-full cursor-pointer p-2 text-left hover:bg-gray"
            onclick={() => selectName(result)}
          >
            {result}
          </button>
        {/each}
      </div>
    </div>
  {/if}
</div>
