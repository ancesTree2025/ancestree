<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import SidePanel from '../components/SidePanel.svelte';

  import IconUserSearch from '~icons/tabler/user-search';

  import { fetchTree, fetchInfo } from '$lib';
  import type { Tree, LoadingStatus, PersonInfo } from '$lib/types';

  import { page } from '$app/state';
  import { fetchRelationship } from '$lib/familytree/fetchRelationship';

  const name = $state<string | undefined>();
  let status = $state<LoadingStatus>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  let familyTree: FamilyTree | null = $state(null);

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

  let searchCompletion = $state<string[]>([]);

  async function onSubmit(name: string) {
    if (!name.length) return;

    status = { state: 'loading' };
    const result = await fetchTree(name, useFakeData);
    const [fetched, error] = result.toTuple();
    if (fetched) {
      tree = fetched;
      status = { state: 'idle' };
    } else if (error) {
      status = { state: 'error', error };
    }
  }

  let searchValue = $state<string>();
  let hideCompletion = $state(false);

  function onChange(e: Event) {
    hideCompletion = false;
    const value = (e.target as HTMLInputElement).value;
    searchCompletion =
      tree?.people.map((p) => p[1].name).filter((name) => name.includes(value)) ?? [];
  }

  function searchWithinTree(result: string) {
    searchValue = result;
    hideCompletion = true;
    fetchRelationship(
      tree!.focus,
      tree!.people.find((tup) => tup[1].name === result)![0]!,
      true
    ).then((result) => {
      // if parents includes person filter children to qid
      // if children include person keep both parents if one matches qid, else remove marriage
      const newRelationship = result.getOrNull();
      if (newRelationship != null) {
        tree = {
          ...tree!,
          marriages: tree!.marriages.flatMap((marriage) => {
            if (newRelationship.chain.some((person) => marriage.parents.includes(person))) {
              const filteredChildren = marriage.children.filter((p) =>
                newRelationship.chain.includes(p)
              );
              return filteredChildren.length === 0
                ? []
                : [
                    {
                      parents: marriage.parents,
                      children: marriage.children.filter((p) => newRelationship.chain.includes(p))
                    }
                  ];
            } else if (newRelationship.chain.some((person) => marriage.children.includes(person))) {
              const filteredParents = marriage.parents.filter((p) =>
                newRelationship.chain.includes(p)
              );
              return filteredParents.length === 0
                ? []
                : [
                    {
                      children: marriage.parents.filter((p) => newRelationship.chain.includes(p)),
                      parents: filteredParents
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
    <div class="w-48"></div>
  </nav>
  <div class="flex flex-1">
    <div class="flex-1">
      <FamilyTree bind:this={familyTree} {getPersonInfo} {tree} />
    </div>
    <SidePanel
      name={sidePanelName}
      show={showSidePanel}
      data={sidePanelData}
      onclose={closeSidePanel}
    />
  </div>
  {#if tree}
    <div class="flex justify-center pb-60">
      <div class="relative flex w-60 items-center gap-3 self-start rounded-full bg-input pl-4">
        <IconUserSearch class="flex-none text-black opacity-50" />
        <input
          bind:value={searchValue}
          class="flex-1 bg-transparent py-2 outline-none"
          placeholder="Search in tree..."
          oninput={onChange}
        />
        {#if searchCompletion.length && !hideCompletion}
          <div class="absolute left-0 right-0 top-full mx-5">
            <div class="rounded-lg bg-white shadow-lg">
              {#each searchCompletion as result}
                <button
                  class="block w-full cursor-pointer p-2 text-left hover:bg-gray"
                  onclick={() => searchWithinTree(result)}
                >
                  {result}
                </button>
              {/each}
            </div>
          </div>
        {/if}
      </div>
    </div>
  {/if}
</div>
