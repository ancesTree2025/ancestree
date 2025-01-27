<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

  let name = $state<string | undefined>();
  let loading = $state(false);

  let tree = $state<Tree | undefined>();

  $effect(() => {
    if (name) {
      loading = true;
      fetchTree(name).then((fetched) => {
        tree = fetched;
        loading = false;
      });
    }
  });
</script>

<div class="flex h-full flex-col">
  <nav class="flex items-center px-8 py-4 shadow-lg">
    <h1 class="mr-8">Ancestree</h1>
    <NameInput bind:nameInput={name} {loading} />
  </nav>
  <div class="flex-1">
    <FamilyTree {tree} />
  </div>
</div>

<style scoped>
</style>
