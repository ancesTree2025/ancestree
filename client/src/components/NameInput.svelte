<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';
  import IconAlert from '~icons/tabler/alert-triangle-filled';
  import IconTree from '~icons/tabler/sitemap';
  import type { LoadingStatus } from '$lib/types';
  import { scale } from 'svelte/transition';
  import { fetchNames } from '$lib';

  interface Props {
    status: LoadingStatus;
    onSubmit: (name: string) => void;
    clearSearch?: () => void;
    namesInTree: string[];
  }
  const { status, onSubmit, clearSearch = () => {}, namesInTree }: Props = $props();

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
  let treeSuggestions = $state<string[]>([]);
  let externalSuggestions = $state<string[]>([]);
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
      externalSuggestions = [];
    }
  });
  async function searchForNames() {
    treeSuggestions = searchQuery
      ? namesInTree
          .filter((name) => name.toLowerCase().includes(searchQuery.toLowerCase()))
          .slice(0, 3)
      : [];
    const result = await fetchNames(searchQuery);
    try {
      const MAX_SUGGESTIONS = 6;
      externalSuggestions = result
        .getOrThrow()
        .filter((name) => !treeSuggestions.includes(name))
        .slice(0, MAX_SUGGESTIONS - treeSuggestions.length);
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
    externalSuggestions = [];
    name = selectedName; // Replaces the typed name with the selected name
    searchQuery = '';
    onSubmit(name);
  }

  function onChange(event: Event) {
    const { value } = event.target as HTMLInputElement;
    name = value;
    searchQuery = value;
  }

  // common tailwind classes for status icons
  const ICON_CLASS = 'absolute right-3 flex h-full w-5 items-center';
  const HOVER_CLASS = 'scale-100 hover:scale-125 transition-transform duration-150';
</script>

<div class="bg-input relative flex w-80 items-center gap-3 rounded-full pl-4">
  <IconSearch class="text-black opacity-50" />
  <form onsubmit={() => selectName(name)}>
    <input
      class="flex-1 bg-transparent py-2 outline-none"
      value={name}
      oninput={onChange}
      placeholder="Enter a name..."
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
        <p class="bg-red mt-2 text-nowrap rounded-xl px-8 py-1 text-center text-sm text-white">
          {status.error}
        </p>
      </div>
    </div>
  {/if}
  {#if externalSuggestions.length || treeSuggestions.length}
    <div class="absolute left-0 right-0 top-full mx-5">
      <div class="rounded-lg bg-white shadow-lg">
        {#each treeSuggestions as result}
          <button
            class="hover:bg-gray block w-full cursor-pointer p-2 text-left"
            onclick={() => selectName(result)}
          >
            <div class="flex items-center gap-3">
              <IconTree class="text-black opacity-50" />{result}
            </div>
          </button>
        {/each}
        {#each externalSuggestions as result}
          <button
            class="hover:bg-gray block w-full cursor-pointer p-2 text-left"
            onclick={() => selectName(result)}
          >
            <div class="flex items-center gap-3">
              <IconTree class="invisible text-black opacity-50" />{result}
            </div>
          </button>
        {/each}
      </div>
    </div>
  {/if}
</div>
