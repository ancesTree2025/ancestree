<script lang="ts">
  import { balanceTree } from '$lib/familytree/balanceTree';
  import { type Marriages, type Positions, type Tree } from '$lib/familytree/models';

  const {
    tree,
    getPersonInfo
  }: { tree?: Tree; getPersonInfo: (qid: string, name: string) => void } = $props();
  let visMarriages = $state<Marriages | undefined>(tree?.marriages);
  let positions = $state<Positions>({});

  $effect(() => {
    if (tree) {
      [positions, visMarriages] = balanceTree(tree, [-500, 0]);
    } else {
      positions = {};
    }
  });

  const RECT_HEIGHT = 60;
  const RECT_WIDTH = 120;
  const RECT_RADIUS = 10;

  let width = $state(0);
  let height = $state(0);
</script>

<svg class="h-full w-full" bind:clientWidth={width} bind:clientHeight={height}>
  <g style={`transform: translate(${width / 2}px, ${height / 2}px)`}>
    {#if tree && visMarriages}
      {#each visMarriages as marriage}
        <!-- fetch Person for each parent, child -->
        {@const mother = positions[marriage.parents[0]]}
        {@const father = positions[marriage.parents[1]]}
        {@const children = marriage.children.map((id) => positions[id])}

        {#if mother && father}
          <!-- Draw marriage lines -->
          {@const parentsX = (mother.x + father.x) / 2}
          {#if mother.y === father.y}
            <line
              x1={mother.x}
              y1={mother.y}
              x2={father.x}
              y2={father.y}
              class="stroke-node stroke-line"
            />
          {:else}
            <line
              x1={mother.x}
              y1={mother.y}
              x2={parentsX}
              y2={mother.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={parentsX}
              y1={mother.y}
              x2={parentsX}
              y2={father.y}
              class="stroke-node stroke-line"
            />
            <line
              x1={father.x}
              y1={father.y}
              x2={parentsX}
              y2={father.y}
              class="stroke-node stroke-line"
            />
          {/if}

          {#if children.length > 0}
            <!-- Draw line between parents and children -->
            {@const parentsY = Math.max(mother.y, father.y)}
            {@const childrenY = Math.min(...children.map((child) => child?.y ?? Infinity))}
            {@const midY = (parentsY + childrenY) / 2}
            <line
              x1={parentsX}
              y1={parentsY}
              x2={parentsX}
              y2={midY}
              class="stroke-node stroke-line"
            />

            <!-- Draw children line -->
            {@const leftChildX = Math.min(
              parentsX,
              ...children.map((child) => child?.x ?? Infinity)
            )}
            {@const rightChildX = Math.max(
              parentsX,
              ...children.map((child) => child?.x ?? -Infinity)
            )}
            <line
              x1={leftChildX}
              y1={midY}
              x2={rightChildX}
              y2={midY}
              class="stroke-node stroke-line"
            />

            <!-- Draw line from each child to children line -->
            {#each children as child}
              {#if child}
                <line
                  x1={child.x}
                  y1={midY}
                  x2={child.x}
                  y2={child.y}
                  class="stroke-node stroke-line"
                />
              {/if}
            {/each}
          {/if}
        {/if}
      {/each}
      {#each tree.people as [id, person]}
        {@const position = positions[id]}
        {#if position}
          <g transform="translate({position.x},{position.y})">
            <rect
              x={-RECT_WIDTH / 2}
              y={-RECT_HEIGHT / 2}
              width={RECT_WIDTH}
              height={RECT_HEIGHT}
              rx={RECT_RADIUS}
              class="fill-node"
            ></rect>
            <foreignObject
              x={-RECT_WIDTH / 2}
              y={-RECT_HEIGHT / 2}
              width={RECT_WIDTH}
              height={RECT_HEIGHT}
            >
              <button
                onclick={() => getPersonInfo(id, person.name)}
                class="flex h-full w-full cursor-pointer items-center justify-center text-center"
              >
                {person.name}
              </button>
            </foreignObject>
          </g>
        {/if}
      {/each}
    {/if}
  </g>
</svg>
