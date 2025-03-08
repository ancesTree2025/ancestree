<script lang="ts">
  import FamilyTree from '../components/FamilyTree.svelte';
  import NameInput from '../components/NameInput.svelte';
  import PersonSearchIcon from '~icons/tabler/user-search';
  import FilterIcon from '~icons/tabler/filter';
  import AlignCenterIcon from '~icons/tabler/keyframe-align-center';
  import SidePanel from '../components/SidePanel.svelte';
  import { treeHistory } from '../components/TreeHistory.svelte';
  import Tooltip from '../components/Tooltip.svelte';

  import { fetchTree, fetchInfo, filterByOption } from '$lib';
  import type {
    Tree,
    LoadingStatus,
    PersonInfo,
    InfoChecklist,
    Person,
    PopupStatus,
    Position,
    PersonID
  } from '$lib/types';

  import { fetchRelationship } from '$lib/familytree/fetchRelationship';
  import { apiResponseToTree } from '$lib/familytree/fetchTree';
  import RelationFinder from '../components/RelationFinder.svelte';
  import FilterPopup from '../components/FilterPopup.svelte';
  import FilterContent from '../components/FilterContent.svelte';

  import { page } from '$app/state';

  import IconSettings from '~icons/tabler/settings';
  import IconArrowLeft from '~icons/tabler/arrow-narrow-left';
  import IconArrowRight from '~icons/tabler/arrow-narrow-right';

  let name = $state<string>('');
  let status = $state<LoadingStatus>({ state: 'idle' });
  let relationFinderStatus = $state<LoadingStatus>({ state: 'idle' });
  let currentName = '';
  let currentWidth = 4;
  let currentHeight = 4;

  let rawTree = $state<Tree | undefined>();
  let tree = $state<Tree | undefined>();
  let relation = $state<
    | {
        tree: Tree;
        relationDescriptor: string;
      }
    | undefined
  >();
  const useFakeData = page.url.searchParams.get('useFakeData') === 'true';

  let familyTree: FamilyTree | null = $state(null);
  let popupStatus = $state<PopupStatus>(null);

  let relationFinder = $state<RelationFinder>();

  let showSettings = $state(false);
  const maxWidth = 4;
  let maxHeight = $state(4);
  let currentCenter: Position = $state<Position>({ x: 0, y: 0 });
  const checkboxOptions: InfoChecklist = [
    { key: 'image', label: 'Show Image', checked: true },
    { key: 'birth', label: 'Show Birth Date', checked: true },
    { key: 'bcoords', label: 'Show Birth Location', checked: true },
    { key: 'death', label: 'Show Death Date', checked: true },
    { key: 'dcoords', label: 'Show Death Location', checked: true },
    { key: 'residence', label: 'Show Residence', checked: true },
    { key: 'rcoords', label: 'Show Map Location of Residence', checked: true },
    { key: 'description', label: 'Show Description', checked: true },
    { key: 'office', label: 'Show Offices Held', checked: true },
    { key: 'wikiLink', label: 'Show Wikipedia Link', checked: true }
  ];

  let filterSibling = $state(true);
  let filterSpouseFamily = $state(false);
  let filterAncestor = $state(true);
  let filterDescendant = $state(true);

  function toggleSettings() {
    showSettings = !showSettings;
  }

  async function onSubmit(newName: string) {
    if (!newName.length) return;
    relationFinder?.clear();
    currentName = newName;

    const withinTree = tree?.people.find((tup) => tup[1].name === newName);
    if (withinTree && currentWidth === maxWidth && currentHeight === maxHeight) {
      familyTree?.handleClick(withinTree[0], withinTree[1].name, null);
    } else {
      status = { state: 'loading' };
      fetchTree(newName, useFakeData, maxWidth, maxHeight).then((result) => {
        const [fetched, error] = result.toTuple();
        if (fetched) {
          rawTree = fetched;
          relation = undefined;
          treeHistory.put({
            tree: rawTree,
            relation,
            sidePanel: {
              name: sidePanelName ?? '',
              qid: sidePanelQid ?? ''
            }
          });

          status = { state: 'idle' };

          // Opening the side panel with the focus on search complete
          const [qid, personName] = getFocusQidAndName();
          if (qid && personName) getPersonInfo(qid, personName.name, { x: 0, y: 0 });
        } else if (error) {
          status = { state: 'error', error };
        }
      });
    }

    currentWidth = maxWidth;
    currentHeight = maxHeight;
  }

  $effect(() => {
    tree =
      rawTree &&
      filterByOption(rawTree, {
        sibling: filterSibling,
        spousefamily: filterSpouseFamily,
        ancestor: filterAncestor,
        descendant: filterDescendant
      });
  });

  /**
   * Assumes that tree is defined
   */
  function getFocusQidAndName(): [string, Person] {
    const [qid, personName] = rawTree!.people.find((p) => p[0] === rawTree!.focus)!;
    return [qid, personName];
  }

  function searchWithinTree(query: string) {
    if (!sidePanelQid) return;
    relationFinderStatus = { state: 'loading' };
    fetchRelationship(sidePanelQid, query).then((result) => {
      if (!tree) return;
      if (result.isError()) {
        relationFinderStatus = { state: 'error', error: result.errorOrNull() };
      }
      const response = result.getOrNull();
      if (response === null) return;

      relation = {
        relationDescriptor: response.relation,
        tree: apiResponseToTree(response?.links)
      };
      treeHistory.put({
        tree,
        relation,
        sidePanel: {
          name: sidePanelName ?? '',
          qid: sidePanelQid ?? ''
        }
      });
      relationFinderStatus = { state: 'idle' };
    });
  }

  let sidePanelQid = $state<PersonID | undefined>();
  let sidePanelName = $state<string | undefined>(undefined);
  let sidePanelData = $state<PersonInfo | undefined>(undefined);

  async function getPersonInfo(qid: string, name: string, position: Position) {
    sidePanelQid = undefined;
    sidePanelData = undefined;
    sidePanelName = undefined;
    currentCenter = position;
    const [fetched] = (await fetchInfo(qid, useFakeData, checkboxOptions)).toTuple();

    if (fetched) {
      sidePanelQid = qid;
      sidePanelData = fetched;
      sidePanelName = name;
      treeHistory.updateSidePanel({
        qid: sidePanelQid,
        name: sidePanelName
      });
    }
  }

  function switchPopup(to: PopupStatus) {
    popupStatus = popupStatus === to ? null : to;
  }

  function handleUndo() {
    relation = undefined;
    relationFinder?.clear();
    const historyElem = treeHistory.undo();
    tree = historyElem.tree;
    relation = historyElem.relation;
    sidePanelName = historyElem.sidePanel.name;
    sidePanelQid = historyElem.sidePanel.qid;
    getPersonInfo(sidePanelQid, sidePanelName, { x: 0, y: 0 });
  }
  function handleRedo() {
    relation = undefined;
    relationFinder?.clear();
    const historyElem = treeHistory.redo();
    tree = historyElem.tree;
    relation = historyElem.relation;
    sidePanelName = historyElem.sidePanel.name;
    sidePanelQid = historyElem.sidePanel.qid;
    getPersonInfo(sidePanelQid, sidePanelName, { x: 0, y: 0 });
  }

  async function expandNode(id: string, name: string, position: Position) {
    const result = await fetchTree(name, false);
    const childTree = result.getOrThrow();

    const oldPeople = rawTree!.people;
    const oldMarriages = rawTree!.marriages;

    const newPeople = childTree.people.filter((p) => !oldPeople.some((op) => op[0] === p[0]));

    const allMarriages = [...oldMarriages, ...childTree.marriages];
    const newMarriages = [];
    while (allMarriages.length > 0) {
      const marriage = allMarriages[0];

      const children = new Set<string>();
      const focuses = new Set<string>();
      for (let i = allMarriages.length - 1; i >= 0; i--) {
        const m = allMarriages[i];
        if (m.parents.every((p) => marriage.parents.includes(p))) {
          allMarriages.splice(i, 1);
          m.children.forEach((c) => children.add(c));
          m.focuses.forEach((f) => focuses.add(f));
        }
      }

      newMarriages.push({
        parents: marriage.parents,
        children: Array.from(children),
        focuses: Array.from(focuses),
        type: marriage.type
      });
    }

    rawTree = {
      focus: rawTree!.focus,
      people: [...oldPeople, ...newPeople],
      marriages: newMarriages,
      secondary: [...rawTree!.secondary, id],
      pivot: id,
      pivotPosition: position
    };
    treeHistory.put({
      tree: rawTree,
      relation: undefined,
      sidePanel: {
        name: sidePanelName ?? '',
        qid: sidePanelQid ?? ''
      }
    });
  }

  function collapseNode(id: string) {
    const marriages = [];
    for (const marriage of rawTree!.marriages) {
      const newFocuses = marriage.focuses.filter((f) => f !== id);
      if (newFocuses.length === 0) {
        continue;
      }
      marriages.push({
        parents: marriage.parents,
        children: marriage.children,
        focuses: newFocuses,
        type: marriage.type
      });
    }

    rawTree = {
      focus: rawTree!.focus,
      people: rawTree!.people,
      marriages,
      secondary: rawTree!.secondary.filter((s) => s !== id),
      pivot: rawTree!.pivot,
      pivotPosition: rawTree!.pivotPosition
    };
    treeHistory.put({
      tree: rawTree,
      relation: undefined,
      sidePanel: {
        name: sidePanelName ?? '',
        qid: sidePanelQid ?? ''
      }
    });
  }
</script>

<div class="flex h-full flex-col overflow-x-hidden">
  <nav class="flex items-center px-8 py-4 shadow-lg">
    <div class="flex flex-1 justify-start">
      <a href="/" class="flex items-center gap-2">
        <img src="/logo.png" alt="Ancestree" class="size-8" />
        <h1 class="text-xl font-semibold text-dark-gray">Ancestree</h1>
      </a>
    </div>
    <div class="flex flex-1 items-center justify-center gap-4">
      <div class="flex gap-2">
        <Tooltip title="Undo Tree" position="bm">
          <button
            class="rounded-lg p-1 transition-colors hover:bg-cream disabled:cursor-not-allowed disabled:opacity-50"
            onclick={() => handleUndo()}
            disabled={!treeHistory.canUndo()}
          >
            <IconArrowLeft />
          </button>
        </Tooltip>
        <Tooltip title="Redo Tree" position="bm">
          <button
            class="rounded-lg p-1 transition-colors hover:bg-cream disabled:cursor-not-allowed disabled:opacity-50"
            onclick={() => handleRedo()}
            disabled={!treeHistory.canRedo()}
          >
            <IconArrowRight />
          </button>
        </Tooltip>
      </div>
      <NameInput
        {onSubmit}
        {status}
        namesInTree={tree?.people.map((p) => p[1].name) ?? []}
        type="Search"
        bind:value={name}
      />
    </div>
    <div class="flex flex-1 justify-end text-xl">
      {#if tree}
        <Tooltip title="Relationship Finder" position="bm">
          <button
            class="p-2 {popupStatus === 'relationfinder' ? 'text-orange' : ''}"
            onclick={() => switchPopup('relationfinder')}
          >
            <PersonSearchIcon />
          </button>
        </Tooltip>
        <Tooltip title="Filter Tree" position="bm">
          <button
            class="p-2 {popupStatus === 'filter' ? 'text-orange' : ''}"
            onclick={() => switchPopup('filter')}
          >
            <FilterIcon />
          </button>
        </Tooltip>
        <Tooltip title="Recenter Tree" position="bm">
          <button
            class="p-2"
            onclick={() => familyTree?.recenter(currentCenter.x, currentCenter.y)}
          >
            <AlignCenterIcon />
          </button>
        </Tooltip>
      {/if}
      <Tooltip title="Settings" position="bl">
        <button class="p-2" onclick={toggleSettings}>
          <IconSettings />
        </button>
      </Tooltip>
    </div>
  </nav>
  <div class="flex min-h-0 flex-1">
    <div class="relative flex-1">
      <FamilyTree
        bind:this={familyTree}
        {getPersonInfo}
        tree={relation?.tree ?? tree}
        {expandNode}
        {collapseNode}
      />
      <div class="absolute bottom-8 right-8 flex flex-col items-start gap-4">
        <div class="w-80 rounded-xl bg-white p-6 text-base shadow-lg">
          <label class="mb-2 flex flex-col gap-2 font-medium"
            >Maximum Tree Height
            <div class="flex gap-5">
              <input
                type="range"
                min="1"
                max="5"
                bind:value={maxHeight}
                class="flex-1"
                onclick={() => onSubmit(currentName)}
              />
              <div>
                {maxHeight}
              </div>
            </div>
          </label>
        </div>
      </div>
    </div>
    <SidePanel
      name={sidePanelName}
      show={true}
      data={sidePanelData}
      showImage={checkboxOptions[0].checked}
    />
  </div>
  {#if showSettings}
    <div class="fixed inset-0 z-20 flex items-center justify-center bg-black bg-opacity-50">
      <div class="w-96 rounded-xl bg-white p-6 shadow-lg">
        <h2 class="mb-4 text-lg font-semibold">Settings</h2>
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
        <button
          class="mt-4 rounded-lg bg-orange px-4 py-2 font-semibold text-white"
          onclick={toggleSettings}>Close</button
        >
      </div>
    </div>
  {/if}
  {#if tree}
    <div class="absolute bottom-8 left-8 flex flex-col items-start gap-4">
      <FilterPopup show={popupStatus === 'relationfinder'}>
        <RelationFinder
          bind:this={relationFinder}
          status={relationFinderStatus}
          people={tree.people}
          {searchWithinTree}
          relationDescriptor={relation?.relationDescriptor}
          clearFilter={() => {}}
        />
      </FilterPopup>
      <FilterPopup show={popupStatus === 'filter'}>
        <FilterContent
          bind:sibling={filterSibling}
          bind:spousefamily={filterSpouseFamily}
          bind:ancestor={filterAncestor}
          bind:descendant={filterDescendant}
        />
      </FilterPopup>
    </div>
  {/if}
</div>
