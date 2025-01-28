<script lang="ts">
  import { fetchTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import type { Status } from '$lib/status';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

  let name = $state<string | undefined>();
  let status = $state<Status>({ state: 'idle' });

  let tree = $state<Tree | undefined>();

  $effect(() => {
    if (name) {
      status = { state: 'loading' };
      fetchTree(name).then((result) => {
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
  <div class="flex-1">
    <FamilyTree {tree} />
  </div>
</div>

<style scoped>
</style>
