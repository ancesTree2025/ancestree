<!-- Component to accept a name input from the user -->

<script lang="ts">
  import type { Marriages, People, PersonID, Tree } from "$lib/familytree/models";
  import Card from "./Card.svelte"
  // Name input variable (might be useful to set as $state for autocomplete box)
  let { treeData = $bindable() }: { treeData?: Tree } = $props<{treeData?: Tree}>();

  type PersonData = {
    data: {
        id: PersonID;
        name: string;
        gender: 'male' | 'female';
    },
    id: PersonID;
    depth: number;
  }
  type ApiResponse = {
    root: PersonData;
    nodes: PersonData[];
    edges: {
      node1: PersonID;
      node2: PersonID;
    }[];
  }

  const people: People = new Map();

  let name = $state("");
  const submitAction = () => {
    let people: People = new Map();
    let marriages: Marriages = [];
    fetch(`http://localhost:8080/${name}`).then((response) => {
      response.json().then((s: ApiResponse) => {
        people.set(s.root.id, { name: s.root.data.name, x: 0, y: 0 })
        for (const person of s.nodes) {
          people.set(person.id, { name: person.data.name, x: 0, y: 0 })
        }
        for (const marriage of s.edges) {
          marriages.push({
            parents: [marriage.node1, marriage.node2],
            children: []
          })
        }
        treeData = {
          people,
          marriages
        };
      })
    })
  }
  // let { submitAction } = $props<{submitAction: (name: string) => void}>()
</script>

<Card>
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