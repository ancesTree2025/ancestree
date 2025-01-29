<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import { page } from '$app/stores';

  let name = $state<string | undefined>();
  let loading = $state(false);

  let tree = $state<Tree | undefined>();
  const useFakeData = $page.url.searchParams.get('useFakeData');

  $effect(() => {
    if (name) {
      loading = true;
      fetchTree(name, useFakeData).then((fetched) => {
        tree = fetched;
        loading = false;
      });
    }
  });
</script>

<div class="flex h-full flex-col">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <h1 class="w-48 text-lg">Ancestree</h1>
    <div class="flex flex-1 justify-center">
      <NameInput bind:nameInput={name} {loading} />
    </div>
    <div class="w-48"></div>
  </nav>
  <div class="flex-1">
    <FamilyTree {tree} />
  </div>
</div>

<style scoped>
</style>
