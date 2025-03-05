<script lang="ts">
  import type { PersonInfo } from '$lib/types';
  import { titleCase } from '$lib/utils';
  import defaultPfp from '$lib/assets/Default_pfp.jpg';

  interface Props {
    name?: string;
    data?: PersonInfo;
    show: boolean;
    showImage: boolean;
  }
  const { name, data, show, showImage }: Props = $props();
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
      {@const ignoreKeys = new Set(['image', 'wikiLink', 'description', 'bcoords', 'dcoords'])}
      {#each Object.entries(data) as [key, value]}
        {#if !ignoreKeys.has(key)}
          <div class="flex">
            <p class="w-16 font-bold">{titleCase(key)}</p>
            <p class="flex-1">{value}</p>
          </div>
        {/if}
      {/each}
      {#if data.bcoords || data.dcoords}
        <div class="mt-4 space-y-4">
          {#if data.bcoords}
            <div>
              <p><strong>Birthplace:</strong></p>
              <a
                href={`https://www.google.com/maps?q=${data.bcoords}`}
                target="_blank"
                class="block"
                aria-label="Link to Birthplace coordinates"
              >
                <iframe
                  src={`https://www.openstreetmap.org/export/embed.html?bbox=${Number(data.bcoords.split(',')[1]) - 0.01},${Number(data.bcoords.split(',')[0]) - 0.01},${Number(data.bcoords.split(',')[1]) + 0.01},${Number(data.bcoords.split(',')[0]) + 0.01}&layer=mapnik&marker=${data.bcoords.split(',')[0]},${data.bcoords.split(',')[1]}`}
                  width="100%"
                  height="200"
                  class="border-gray-300 pointer-events-none rounded-lg border"
                  allowfullscreen
                  loading="lazy"
                  title="Birthplace Map"
                ></iframe>
              </a>
            </div>
          {/if}

          {#if data.dcoords}
            <div>
              <p><strong>Death Place:</strong></p>
              <a
                href={`https://www.google.com/maps?q=${data.dcoords}`}
                target="_blank"
                class="block"
                aria-label="Link to Death place coordinates"
              >
                <iframe
                  src={`https://www.openstreetmap.org/export/embed.html?bbox=${Number(data.dcoords.split(',')[1]) - 0.01},${Number(data.dcoords.split(',')[0]) - 0.01},${Number(data.dcoords.split(',')[1]) + 0.01},${Number(data.dcoords.split(',')[0]) + 0.01}&layer=mapnik&marker=${data.dcoords.split(',')[0]},${data.dcoords.split(',')[1]}`}
                  width="100%"
                  height="200"
                  class="border-gray-300 pointer-events-none rounded-lg border"
                  allowfullscreen
                  loading="lazy"
                  title="Deathplace map"
                ></iframe>
              </a>
            </div>
          {/if}
        </div>
      {/if}

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
    {:else}
      {#if showImage}
        <img alt={name} class="mx-8 mb-4 aspect-square rounded-xl object-cover" src={defaultPfp} />
      {/if}
      <div class="h-4 w-3/4 animate-pulse rounded-lg bg-gray"></div>
      <div class="h-4 w-1/2 animate-pulse rounded-lg bg-gray"></div>
      <div class="w-100 h-40 animate-pulse rounded-lg bg-gray"></div>
    {/if}
  </div>
</div>
