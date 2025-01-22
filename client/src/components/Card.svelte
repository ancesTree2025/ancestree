<!-- Card component to overlay other components -->
<div class="h-full w-full fixed flex items-center justify-center">
  <div class="card">
    <slot></slot>
  </div>
</div>

<style>
  .card {
    background-color: black;
    color: white;
    border-radius: 15px;
    padding: 40px 50px;
    margin: 20px 0;
  }
</style>