<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import IconSettings from '~icons/tabler/settings';
  import SidePanel from '../components/SidePanel.svelte';

  import { fetchTree, fetchInfo } from '$lib';
  import type { Tree, LoadingStatus, PersonInfo, InfoChecklist } from '$lib/types';

  import { page } from '$app/state';
  import { fetchRelationship } from '$lib/familytree/fetchRelationship';
  import TreeSearchInput from '../components/TreeSearchInput.svelte';
  import { apiResponseToTree } from '$lib/familytree/fetchTree';

  let name = $state<string | undefined>();
  let status = $state<LoadingStatus>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  let filteredTree = $state<Tree | undefined>();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  let familyTree: FamilyTree | null = $state(null);

  let showSettings = $state(false);
  let maxWidth = $state(4);
  let maxHeight = $state(4);
  const checkboxOptions: InfoChecklist = [
    { key: 'image', label: 'Show Image', checked: true },
    { key: 'birth', label: 'Show Birth Date', checked: true },
    { key: 'death', label: 'Show Death Date', checked: true },
    { key: 'description', label: 'Show Description', checked: true },
    { key: 'wikiLink', label: 'Show Wikipedia Link', checked: true }
  ];

  function toggleSettings() {
    showSettings = !showSettings;
  }

  $effect(() => {
    if (name) {
      status = { state: 'loading' };
      fetchTree(name, useFakeData, maxWidth, maxHeight).then((result) => {
        const [fetched, error] = result.toTuple();
        if (fetched) {
          tree = fetched;
          console.log(fetched);
          status = { state: 'idle' };

          // Opening the side panel with the focus on search complete
          const [qid, personName] = fetched.people.find((p) => p[0] === fetched.focus)!;
          if (qid && personName) getPersonInfo(qid, personName.name);
        } else if (error) {
          status = { state: 'error', error };
        }
      });
    }
  });

  async function onSubmit(newName: string) {
    if (!newName.length) return;
    name = newName;
  }

  function searchWithinTree(result: string) {
    fetchRelationship(tree!.focus, tree!.people.find((tup) => tup[1].name === result)![0]!).then(
      (result) => {
        const response = result.getOrNull();
        if (response != null) {
          filteredTree = apiResponseToTree(response?.links);
        }
      }
    );
  }

  let sidePanelName = $state<string | undefined>(undefined);
  let sidePanelData = $state<PersonInfo | undefined>(undefined);

  async function getPersonInfo(qid: string, name: string) {
    sidePanelData = undefined;
    sidePanelName = undefined;
    const [fetched] = (await fetchInfo(qid, useFakeData, checkboxOptions)).toTuple();

    if (fetched) {
      sidePanelData = fetched;
      sidePanelName = name;
    }
  }
</script>

<div class="flex h-full flex-col overflow-x-hidden">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <a href="/" class="flex items-center gap-2">
      <img src="/logo.png" alt="Ancestree" class="size-8" />
      <h1 class="text-xl font-semibold text-dark-gray">Ancestree</h1>
    </a>
    <div class="flex flex-1 justify-center">
      <NameInput {onSubmit} {status} />
    </div>
    <div class="flex w-48 justify-end">
      <button class="p-2" onclick={toggleSettings}>
        <IconSettings />
      </button>
    </div>
  </nav>
  <div class="flex flex-1">
    <div class="flex-1">
      <FamilyTree bind:this={familyTree} {getPersonInfo} tree={filteredTree ?? tree} />
    </div>
    <SidePanel
      name={sidePanelName}
      show={true}
      data={sidePanelData}
      showImage={checkboxOptions[0].checked}
    />
  </div>
  {#if showSettings}
    <div class="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
      <div class="w-96 rounded bg-white p-6 shadow-lg">
        <h2 class="mb-4 text-lg font-bold">Settings</h2>
        <label class="mb-2 block"
          >Maximum Tree Width
          <div class="border-gray-400 flex h-8 w-12 items-center justify-center rounded border">
            {maxWidth}
          </div>
          <input type="range" min="1" max="10" bind:value={maxWidth} class="w-full" />
        </label>

        <label class="mb-2 block"
          >Maximum Tree Height
          <div class="border-gray-400 flex h-8 w-12 items-center justify-center rounded border">
            {maxHeight}
          </div>
          <input type="range" min="1" max="10" bind:value={maxHeight} class="w-full" />
        </label>
        <div class="mb-4">
          {#each checkboxOptions as option}
            <div class="mb-1 flex items-center gap-2">
              <label>
                <input id="checkbox-{option}" type="checkbox" bind:checked={option.checked} />
                {option.label}
              </label>
            </div>
          {/each}
        </div>
        <button class="bg-blue-500 mt-4 rounded p-2 text-black" onclick={toggleSettings}
          >Close</button
        >
      </div>
    </div>
  {/if}
  {#if tree}
    <div class="flex justify-center pb-60">
      <TreeSearchInput
        names={tree.people.map((p) => p[1].name)}
        onSubmit={searchWithinTree}
        clearSearch={() => {
          filteredTree = undefined;
        }}
      />
    </div>
  {/if}
</div>
