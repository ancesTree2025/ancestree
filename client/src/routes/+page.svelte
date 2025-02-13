<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import SidePanel from '../components/SidePanel.svelte';

  import { fetchTree, fetchInfo } from '$lib';
  import type { Tree, LoadingStatus, PersonInfo } from '$lib/types';

  import { page } from '$app/state';

  let name = $state<string | undefined>();
  let status = $state<LoadingStatus>({ state: 'idle' });

  let tree = $state<Tree | undefined>();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  $effect(() => {
    if (name) {
      status = { state: 'loading' };
      fetchTree(name, useFakeData).then((result) => {
        const [fetched, error] = result.toTuple();
        if (fetched) {
          tree = fetched;
          status = { state: 'idle' };

          const [qid, personName] = fetched.people.find((p => p[0] === fetched.focus));
          if (qid && personName) {
            getPersonInfo(qid, personName.name);
          }
        } else if (error) {
          status = { state: 'error', error };
        }
      });
    }
  });

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
</script>

<div class="flex h-full flex-col overflow-x-hidden">
  <nav class="flex items-center gap-12 px-8 py-4 shadow-lg">
    <h1 class="w-48 text-lg">Ancestree</h1>
    <div class="flex flex-1 justify-center">
      <NameInput bind:nameInput={name} {status} />
    </div>
    <div class="w-48"></div>
  </nav>
  <div class="flex flex-1">
    <div class="flex-1">
      <FamilyTree {getPersonInfo} {tree} />
    </div>
    <SidePanel
      name={sidePanelName}
      show={showSidePanel}
      data={sidePanelData}
      onclose={() => (showSidePanel = false)}
    />
  </div>
</div>

<style scoped>
</style>
