<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import SettingsImg from '$lib/assets/—Pngtree—settings icon_4419959.png'
  import SidePanel from '../components/SidePanel.svelte';

  import { fetchTree, fetchInfo } from '$lib';
  import type { Tree, LoadingStatus, PersonInfo } from '$lib/types';

  import { page } from '$app/state';
  import { fetchRelationship } from '$lib/familytree/fetchRelationship';
  import TreeSearchInput from '../components/TreeSearchInput.svelte';

  const name = $state<string | undefined>();
  let status = $state<LoadingStatus>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  let filteredTree = $state<Tree | undefined>();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  let familyTree: FamilyTree | null = $state(null);

  let showSettings = $state(false);
  function toggleSettings() {
    showSettings = !showSettings;
  }

  $effect(() => {
    if (name) {
      status = { state: 'loading' };
      fetchTree(name, useFakeData).then((result) => {
        const [fetched, error] = result.toTuple();
        if (fetched) {
          tree = fetched;
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

  async function onSubmit(name: string) {
    if (!name.length) return;

    status = { state: 'loading' };
    const result = await fetchTree(name, useFakeData);
    const [fetched, error] = result.toTuple();
    if (fetched) {
      filteredTree = undefined;
      tree = fetched;
      status = { state: 'idle' };
    } else if (error) {
      status = { state: 'error', error };
    }
  }

  function searchWithinTree(result: string) {
    fetchRelationship(
      tree!.focus,
      tree!.people.find((tup) => tup[1].name === result)![0]!,
      useFakeData
    ).then((result) => {
      const newRelationship = result.getOrNull();
      if (newRelationship != null) {
        filteredTree = {
          ...tree!,
          marriages: tree!.marriages.flatMap((marriage) => {
            if (newRelationship.links.some((person) => marriage.parents.includes(person))) {
              const filteredChildren = marriage.children.filter((p) =>
                newRelationship.links.includes(p)
              );
              return filteredChildren.length === 0 &&
                marriage.parents.some((p) => !newRelationship.links.includes(p))
                ? []
                : [
                    {
                      parents: marriage.parents,
                      children: filteredChildren
                    }
                  ];
            } else if (newRelationship.links.some((person) => marriage.children.includes(person))) {
              const filteredParents = marriage.parents.filter((p) =>
                newRelationship.links.includes(p)
              );
              return filteredParents.length === 0
                ? []
                : [
                    {
                      parents: filteredParents,
                      children: marriage.children.filter((p) => newRelationship.links.includes(p))
                    }
                  ];
            }
            return [];
          })
        };
      }
    });
  }

  let showSidePanel = $state(false);
  let sidePanelName = $state<string | undefined>(undefined);
  let sidePanelData = $state<PersonInfo | undefined>(undefined);

  async function getPersonInfo(qid: string, name: string) {
    const [fetched] = (await fetchInfo(qid, useFakeData)).toTuple();
    if (fetched) {
      sidePanelData = fetched;
      sidePanelName = name;
      showSidePanel = true;
    }
  }

  function closeSidePanel() {
    if (familyTree) {
      familyTree.closeSidePanel();
    }

    showSidePanel = false;
  }
</script>

<div class="flex h-full flex-col overflow-x-hidden">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <h1 class="w-48 text-lg">Ancestree</h1>
    <div class="flex flex-1 justify-center">
      <NameInput {onSubmit} {status} />
    </div>
    <div class="w-48 flex justify-end">
      <button class="p-2" onclick={toggleSettings}>
        <img src={SettingsImg} alt="Settings" class="w-10"/>
      </button>
    </div>
  </nav>
  <div class="flex flex-1">
    <div class="flex-1">
      <FamilyTree bind:this={familyTree} {getPersonInfo} tree={filteredTree ?? tree} />
    </div>
    <SidePanel
      name={sidePanelName}
      show={showSidePanel}
      data={sidePanelData}
      onclose={closeSidePanel}
    />
  </div>
  {#if showSettings}
    <div class="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
      <div class="bg-white p-6 rounded shadow-lg w-96">
        <h2 class="text-lg font-bold mb-4">Settings</h2>
        <p>Settings content goes here.</p>
        <button class="mt-4 p-2 bg-blue-500 text-black rounded" onclick={toggleSettings}>Close</button>
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
