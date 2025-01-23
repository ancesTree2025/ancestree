<!-- Component to accept a name input from the user -->

<script lang="ts">
  import Card from "./Card.svelte"
  // Name input variable (might be useful to set as $state for autocomplete box)
  let name = $state("");
  let result = $state("");
  const submitAction = () => {
    fetch(`http://localhost:8080/${name}`).then((response) => {
      response.json().then((s) => {
        result = s?.root?.data?.name;
      })
    })
  }
  // let { submitAction } = $props<{submitAction: (name: string) => void}>()
</script>

<Card>
  <h1>{result}</h1>
  <h1> Enter a name </h1>
  <input bind:value={name} class="NameTextBox"/>
  <div>
    <button class = "SubmitBox" onclick={submitAction}>Submit</button>
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