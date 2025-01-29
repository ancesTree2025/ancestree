<!-- Component to accept a name input from the user -->

<script lang="ts">
  import IconSearch from '~icons/tabler/search';

  let { nameInput = $bindable(), loading }: { nameInput?: string; loading: boolean } = $props<{
    nameInput?: string;
    loading?: boolean;
  }>();

  let name = $state('');

  function submitIfEnter(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      submitAction();
    }
  }

  function submitAction() {
    nameInput = name;
  }
</script>

<div class="flex w-80 items-center gap-3 rounded-full bg-input-bg pl-4">
  <IconSearch class="text-fg opacity-50" />
  <input
    class="flex-1 bg-transparent py-2"
    bind:value={name}
    placeholder="Enter a name..."
    onkeydown={submitIfEnter}
  />
  {#if loading}
    <div class="loader mr-3 h-5 w-5 bg-fg p-1 opacity-50"></div>
  {/if}
</div>
