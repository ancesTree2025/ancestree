<script lang="ts">
  // import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';

  let showNameBox = $state(true)

  function submitAction(name: string) {
    console.log("yay")
    showNameBox = false
  }
</script>

<div class="flex h-full flex-col">
  <nav>
    <h1>Ancestree</h1>
  </nav>

  {#if showNameBox}
  <div class="flex-1">
     <NameInput submitAction={submitAction}></NameInput>
  </div>
  {/if}
</div>

<style scoped>
</style>
