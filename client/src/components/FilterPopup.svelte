<script lang="ts">
  import { fly } from 'svelte/transition';

  /* eslint-disable  @typescript-eslint/no-explicit-any */
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
