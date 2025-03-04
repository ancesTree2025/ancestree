<script lang="ts">
  import IconUserSearch from '~icons/tabler/user-search';

  const {
    onSubmit,
    clearSearch,
    names
  }: { onSubmit: (result: string) => void; clearSearch: () => void; names: string[] } = $props();

  let searchValue = $state<string>('');
  let searchCompletion = $state<string[]>([]);
  let hideCompletion = $state(false);

  function onChange() {
    hideCompletion = false;
    searchCompletion =
      searchValue === ''
        ? []
        : names.filter((name) => name.toLowerCase().includes(searchValue.toLowerCase()));
    clearSearch();
  }

  function onSearchSubmit(result: string) {
    hideCompletion = true;
    searchValue = result;
    onSubmit(result);
  }
</script>

<div class="relative flex w-60 items-center gap-3 self-start rounded-full bg-input pl-4">
  <IconUserSearch class="flex-none text-black opacity-50" />
  <form onsubmit={() => searchCompletion[0] && onSearchSubmit(searchCompletion[0])}>
    <input
      bind:value={searchValue}
      class="flex-1 bg-transparent py-2 outline-none"
      placeholder="Search in tree..."
      oninput={onChange}
    />
  </form>
  {#if searchCompletion.length && !hideCompletion}
    <div class="absolute left-0 right-0 top-full mx-5">
      <div class="rounded-lg bg-white shadow-lg">
        {#each searchCompletion as result}
          <button
            class="block w-full cursor-pointer p-2 text-left hover:bg-gray"
            onclick={() => onSearchSubmit(result)}
          >
            {result}
          </button>
        {/each}
      </div>
    </div>
  {/if}
</div>
