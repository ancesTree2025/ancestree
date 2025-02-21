<script lang="ts">
  import IconUserSearch from '~icons/tabler/user-search';

  const { onSubmit, names }: { onSubmit: (result: string) => void; names: string[] } = $props();

  let searchValue = $state<string>('');
  let searchCompletion = $state<string[]>([]);
  let hideCompletion = $state(false);

  function onChange(e: Event) {
    hideCompletion = false;
    searchCompletion = names.filter((name) => name.includes(searchValue));
  }

  function onSearchSubmit(result: string) {
    hideCompletion = true;
    searchValue = result;
    onSubmit(result);
  }
</script>

<div class="flex justify-center pb-60">
  <div class="bg-input relative flex w-60 items-center gap-3 self-start rounded-full pl-4">
    <IconUserSearch class="flex-none text-black opacity-50" />
    <input
      bind:value={searchValue}
      class="flex-1 bg-transparent py-2 outline-none"
      placeholder="Search in tree..."
      oninput={onChange}
    />
    {#if searchCompletion.length && !hideCompletion}
      <div class="absolute left-0 right-0 top-full mx-5">
        <div class="rounded-lg bg-white shadow-lg">
          {#each searchCompletion as result}
            <button
              class="hover:bg-gray block w-full cursor-pointer p-2 text-left"
              onclick={() => onSearchSubmit(result)}
            >
              {result}
            </button>
          {/each}
        </div>
      </div>
    {/if}
  </div>
</div>
