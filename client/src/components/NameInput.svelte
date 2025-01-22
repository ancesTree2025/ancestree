<!-- Component to accept a name input from the user -->

<script lang="ts">
  import Card from "./Card.svelte"
  // Name input variable (might be useful to set as $state for autocomplete box)
  let name = ""
  let { submitAction } = $props<{submitAction: (name: string) => void}>()
</script>

<Card>
  <h1> Enter a name </h1>
  <input value={name} class="NameTextBox"/>
  <div>
    <button class = "SubmitBox" onclick={submitAction(name)}>Submit</button>
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