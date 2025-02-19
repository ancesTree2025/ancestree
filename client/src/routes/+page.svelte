<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import SidePanel from '../components/SidePanel.svelte';

  import { fetchTree, fetchInfo } from '$lib';
  import type { Tree, LoadingStatus, PersonInfo } from '$lib/types';

  import { page } from '$app/state';

  const name = $state<string | undefined>();
  let status = $state<LoadingStatus>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  let familyTree: FamilyTree | null = null;

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
      tree = fetched;
      status = { state: 'idle' };
    } else if (error) {
      status = { state: 'error', error };
    }
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
</div>
