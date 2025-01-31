<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import type { Status } from '$lib/status';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  // @ts-expect-error $app/stores is valid but not recognised
  import { page } from '$app/stores';

  let name = $state<string | undefined>();
  let status = $state<Status>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  const useFakeData = $page.url.searchParams.get('useFakeData');

  $effect(() => {
    if (name) {
      status = { state: 'loading' };
      fetchTree(name, useFakeData).then((result) => {
        const [fetched, error] = result.toTuple();
        if (fetched) {
          tree = fetched;
          status = { state: 'idle' };
        } else if (error) {
          status = { state: 'error', error };
        }
      });
    }
  });
</script>

<div class="flex h-full flex-col">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <h1 class="w-48 text-lg">Ancestree</h1>
    <div class="flex flex-1 justify-center">
      <NameInput bind:nameInput={name} {status} />
    </div>
    <div class="w-48"></div>
  </nav>
  <div class="flex flex-1">
    <div class="flex-1">
      <FamilyTree {tree} />
    </div>
    <div class="flex w-96 flex-col gap-6 p-4 pt-10 text-center shadow-lg">
      <div class="text-lg font-bold">Henry VII</div>
      <img
        src="https://www.rmg.co.uk/sites/default/files/styles/full_width_1440/public/Henry%20VIII.jpg?itok=jdBCof8r"
      />
      <div class="text-left"><b>Born:</b> 28 June 1491, Palace of Placentia</div>
      <div class="text-left">
        <b>Died:</b> 28 January 1547 (age 55 years), Palace of Whitehall, London
      </div>
      <div class="text-left">
        Henry VIII was King of England from 22 April 1509 until his death. Henry is known for his
        six marriages and his efforts to have his first marriage annulled. <a
          href="https://en.wikipedia.org/wiki/Henry_VIII">Wikipedia</a
        >
      </div>
    </div>
  </div>
</div>

<style scoped>
</style>
