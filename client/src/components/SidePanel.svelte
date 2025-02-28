<script lang="ts">
  import type { PersonInfo } from '$lib/types';
  import IconClose from '~icons/tabler/x';

  interface Props {
    name?: string;
    data?: PersonInfo;
    show: boolean;
    onclose: () => void;
  }
  const { name, data, show, onclose }: Props = $props();
</script>

<div class={`${show ? 'w-96' : 'w-0'} shadow-lg transition-all duration-500`}>
  <div class="flex w-96 flex-col gap-6 px-8 py-6">
    <div class="flex items-center justify-between">
      {#if data}
        <div class="text-xl font-semibold">{name}</div>
      {:else}
        <div class="h-8 w-3/4 animate-pulse rounded-lg bg-gray"></div>
      {/if}
      <button class="cursor-pointer p-4" onclick={onclose}>
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
    {:else}
      <img
        alt={name}
        class="mx-8 mb-4 aspect-square rounded-xl object-cover"
        src="https://upload.wikimedia.org/wikipedia/commons/a/ac/Default_pfp.jpg"
      />
      <div class="h-4 w-3/4 animate-pulse rounded-lg bg-gray"></div>
      <div class="h-4 w-1/2 animate-pulse rounded-lg bg-gray"></div>
      <div class="w-100 h-40 animate-pulse rounded-lg bg-gray"></div>
    {/if}
  </div>
</div>
