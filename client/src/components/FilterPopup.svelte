<script lang="ts">
  import { fly } from 'svelte/transition';

  const { show, children }: { show: boolean; children: any } = $props();
</script>

{#if show}
  <div
    class="relative bottom-0 rounded-xl bg-white p-4 shadow-lg"
    transition:fly={{ y: 100, duration: 150 }}
  >
    {@render children()}
  </div>
{/if}
