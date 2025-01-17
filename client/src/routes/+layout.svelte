<script lang="ts">
  import '../app.css';
  let { children } = $props();
</script>

{@render children()}

<style>
	:global(:root, body, body > div) {
		height: 100%;
	}
</style>
