<!-- Component to accept a name input from the user -->

<script lang="ts">
  import Card from './Card.svelte';

  let { nameInput = $bindable(), loading }: { nameInput?: string; loading: boolean } = $props<{
    nameInput?: string;
    loading?: boolean;
  }>();

  let name = $state('');
  function submitAction() {
    nameInput = name;
  }
</script>

<Card>
  <h1>Enter a name</h1>
  <input bind:value={name} class="NameTextBox" />
  <div>
    <button class="SubmitBox" onclick={submitAction}>Submit</button>
    {#if loading}
      <p>Loading...</p>
    {/if}
  </div>
</Card>

<style>
  .NameTextBox {
    color: black;
  }

  .SubmitBox {
    background-color: white;
    color: black;
    margin-top: 5px;
    padding: 0px 5px;
  }
</style>
