<script lang="ts">
  import type { PageData } from './$types';
  import { goto } from '$app/navigation';
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import SidePanel from '../components/SidePanel.svelte';

  import { fetchInfo } from '$lib';
  import type { LoadingStatus, PersonInfo } from '$lib/types';

  let status = $state<LoadingStatus>({ state: 'idle' });

  let familyTree: FamilyTree | null = null;

  const { data }: { data: PageData } = $props();

  async function onSubmit(name: string) {
    if (!name.length) return;

    goto(`/?useFakeData=${data.useFakeData}&q=${name}`);
  }

  let showSidePanel = $state(false);
  let sidePanelName = $state<string | undefined>(undefined);
  let sidePanelData = $state<PersonInfo | undefined>(undefined);

  async function getPersonInfo(qid: string, name: string) {
    const [fetched] = (await fetchInfo(qid, data.useFakeData)).toTuple();
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
      <FamilyTree bind:this={familyTree} {getPersonInfo} tree={data.tree} />
    </div>
    <SidePanel
      name={sidePanelName}
      show={showSidePanel}
      data={sidePanelData}
      onclose={closeSidePanel}
    />
  </div>
</div>
