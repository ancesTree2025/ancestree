<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

  let name = $state<string | undefined>();
  let loading = $state(false);
  let error = $state<string | undefined>();

  let tree = $state<Tree | undefined>();

  $effect(() => {
    if (name) {
      loading = true;
      error = undefined;
      fetchTree(name).then((result) => {
        const [fetched, err] = result.toTuple();
        if (fetched) {
          tree = fetched;
        } else if (err) {
          error = err;
        }
        loading = false;
      });
    }
  });
</script>

<div class="flex h-full flex-col">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <h1 class="w-48 text-lg">Ancestree</h1>
    <div class="flex flex-1 justify-center">
      <NameInput bind:nameInput={name} {loading} {error} />
    </div>
    <div class="w-48"></div>
  </nav>
  <div class="flex-1">
    <FamilyTree {tree} />
  </div>
</div>

<style scoped>
</style>
