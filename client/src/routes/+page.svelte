<script lang="ts">
  import type { Tree } from '$lib/familytree/models';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

</script>

<div class="flex h-full flex-col">
  <nav>
    <h1>Ancestree</h1>
  </nav>
  <div class="flex-1">
    <!-- <Panzoom>
      <Node name="King henry" x={100} y={100} />
    </Panzoom> -->
    <FamilyTree />
  </div>
</div>

  <div class="flex-1">
    <NameInput></NameInput>
  </div>

<style scoped>
</style>
