<!-- Component to accept a name input from the user -->

<script lang="ts">
  import { apiResponseToTree } from '$lib/familytree/fetchTree';
  import type { Tree } from '$lib/familytree/models';
  import Card from './Card.svelte';

  let { nameInput = $bindable() }: { nameInput?: string } = $props<{ nameInput?: string }>();

  let name = $state('');
  const submitAction = () => {
    nameInput = name
  };
    // fetch(`http://localhost:8080/${name}`).then((response) => {
    //   response.json().then((s) => {
    //     treeData = apiResponseToTree(s);
    //   });
    // });
  // let { submitAction } = $props<{submitAction: (name: string) => void}>()
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
