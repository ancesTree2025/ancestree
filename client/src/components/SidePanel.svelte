<script lang="ts">
  import type { PersonInfo } from '$lib/types';
  import IconClose from '~icons/tabler/x';
  import { titleCase } from '$lib/utils';

  interface Props {
    name?: string;
    data?: PersonInfo;
    show: boolean;
  }
  const { name, data, show }: Props = $props();
</script>

<div class={`${show ? 'w-96' : 'w-0'} shadow-lg transition-all duration-500`}>
  <div class="flex w-96 flex-col gap-6 px-8 py-6">
    <div class="flex items-center justify-between">
      {#if data}
        <div class="text-xl font-semibold">{name}</div>
      {:else}
        <div class="h-8 w-3/4 animate-pulse rounded-lg bg-gray"></div>
      {/if}
    </div>
    {#if data}
      {#if data.image}
        <img alt={name} class="mx-8 mb-4 aspect-square rounded-xl object-cover" src={data.image} />
      {/if}
      {@const ingoreKeys = new Set(['image', 'wikiLink', 'description'])}
      {#each Object.entries(data) as [key, value]}
        {#if !ingoreKeys.has(key)}
          <div class="flex">
            <p class="w-16 font-bold">{titleCase(key)}</p>
            <p class="flex-1">{value}</p>
          </div>
        {/if}
      {/each}

      {#if data.description}
        <p class="mt-4">
          {data.description}
        </p>
      {/if}

      {#if data.wikiLink}
        <p class="mt-4">
          <a href={data.wikiLink}>Wikipedia</a>
        </p>
      {/if}

    {/if}
  </div>
</div>
