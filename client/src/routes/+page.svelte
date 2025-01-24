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
    <FamilyTree />
  </div>
</div>

<style scoped>
</style>
