<!-- Component to accept a name input from the user -->

<script lang="ts">
  import type { Marriages, People, Person, PersonID, Tree } from '$lib/familytree/models';
  import Card from './Card.svelte';
  // Name input variable (might be useful to set as $state for autocomplete box)
  let { treeData = $bindable() }: { treeData?: Tree } = $props<{ treeData?: Tree }>();

  type PersonData = {
    data: {
      id: PersonID;
      name: string;
      gender: 'male' | 'female';
    };
    id: PersonID;
    depth: number;
  };
  type ApiResponse = {
    root: PersonData;
    nodes: PersonData[];
    edges: {
      node1: PersonID;
      node2: PersonID;
    }[];
  };

  const people: People = new Map();

  let name = $state('');
  const submitAction = () => {
    fetch(`http://localhost:8080/${name}`).then((response) => {
      const people: Map<PersonID, Person & { depth: number }> = new Map();
      const marriages: Marriages = [];
      // Create map of Person to list of parents
      // Iterate through this map, creating a new map of parents to family

      // Create map of Person to list of children
      // Create list of marriages
      // For each marriage m
      response.json().then((s: ApiResponse) => {
        for (const person of s.nodes.concat(s.root) ) {
          people.set(person.id, { name: person.data.name, x: 0, y: 0, depth: person.depth });
        }
        const children = new Map<PersonID, PersonID[]>();
        for (const edge of s.edges) {
          // node 1: parent, node 2: child
          if (people.get(edge.node1)!.depth < people.get(edge.node2)!.depth) {
            if (!children.has(edge.node1)) {
              children.set(edge.node2, []);
            }
            children.get(edge.node1)?.push(edge.node2);
          } else {
            marriages.push({ parents: [edge.node1, edge.node2], children: [] });
          }
        }
        for (const marriage of marriages) {
          marriage.children = children
            .get(marriage.parents[0])!
            .filter((child) => children.get(marriage.parents[1])!.includes(child));
        }
        treeData = {
          people,
          marriages
        };
      });
    });
  };
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
