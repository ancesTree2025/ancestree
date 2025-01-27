<script lang="ts">
  import { balanceTree } from '$lib/familytree/balanceTree';
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
  <nav>
    <h1>Ancestree</h1>
  </nav>
  <NameInput bind:nameInput={name} {loading} />
  <div class="flex-1">
    <FamilyTree {tree} />
  </div>
</div>

<style scoped>
</style>
