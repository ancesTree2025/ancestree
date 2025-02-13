<script lang="ts">
  import type { PersonInfo } from '$lib/types';
  import IconClose from '~icons/tabler/x';

  const {
    name,
    data,
    show,
    close
  }: { name?: string; data?: PersonInfo; show: boolean; close: () => void } = $props();
</script>

<div class={`${show ? 'w-96' : 'w-0'} shadow-lg transition-all duration-500`}>
  <div class="flex w-96 flex-col gap-6 px-8 py-6">
    <div class="flex items-center justify-between">
      <div class="text-xl font-semibold">{name}</div>
      <button class="cursor-pointer p-4" onclick={close}>
        <IconClose />
      </button>
    </div>
    {#if data}
      <img alt={name} class="mx-8 mb-4 aspect-square rounded-xl object-cover" src={data.image} />
      {#each Object.entries(data.attributes) as [key, value]}
        <div class="flex">
          <p class="w-16 font-bold">{key}</p>
          <p class="flex-1">{value}</p>
        </div>
      {/each}
      <p class="mt-4">
        {data.description} <a href={data.wikipedia_link}>Wikipedia</a>
      </p>
    {/if}
  </div>
</div>
