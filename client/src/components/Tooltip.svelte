<script lang="ts">
  import type { Snippet } from 'svelte';

  type Props = {
    title: string;
    children: Snippet<[]>;
    position?: "top" | "bottom";
  };

  const { title, children, position = "top" }: Props = $props();
</script>

<div class="group relative">
  {@render children()}

  <div
    class={`absolute ${position == 'top' ? 'bottom-full' : 'top-full'} left-1/2 -translate-x-1/2 w-full min-w-max rounded-md bg-black/90 p-2 text-sm text-white opacity-0 transition-opacity group-hover:opacity-100`}
  >
    {title}
  </div>
</div>
