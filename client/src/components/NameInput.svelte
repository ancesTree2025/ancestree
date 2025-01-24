<!-- Component to accept a name input from the user -->

<script lang="ts">
  import Card from './Card.svelte';

  let { nameInput = $bindable() }: { nameInput?: string } = $props<{ nameInput?: string }>();

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
