<script lang="ts">
  import type { Tree } from '$lib/familytree/models';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

  let showNameBox = $state(true);
  let treeData = $state<Tree | undefined>();
</script>

<div class="flex h-full flex-col">
  <nav>
    <h1>Ancestree</h1>
  </nav>
  <div class="flex-1">
    <!-- <Panzoom>
      <Node name="King henry" x={100} y={100} />
    </Panzoom> -->
    <NameInput bind:treeData />
    <FamilyTree {treeData} />
  </div>
</div>

{#if showNameBox}
  <div class="flex-1">
    <NameInput></NameInput>
  </div>
{/if}

<style scoped>
</style>
