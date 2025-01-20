<script>
  import Panzoom from '../components/Panzoom.svelte';
  import Node from '../components/Node.svelte';
</script>

<div class="flex h-full flex-col">
  <nav>
    <h1>Ancestree</h1>
  </nav>
  <div class="flex-1">
    <Panzoom>
      <Node name="King henry" x={100} y={100} />
    </Panzoom>
  </div>
</div>

<style scoped>
</style>
