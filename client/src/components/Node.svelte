<script lang="ts">
  export let name: string;
  export let x: number;
  export let y: number;
</script>

<g>
  <rect class="bg" {x} {y}></rect>
  <text class="person_name" x={x + 10} y={y + 30}>{name}</text>
</g>

<style scoped>
  .person_name {
    width: 100px;
    height: 50px;
    fill: black;
    stroke: b;
    rx: 15px;
    /* font-size: 10px; */
  }

  .bg {
    width: 100px;
    height: 50px;
    fill: white;
    stroke: black;
  }
</style>
