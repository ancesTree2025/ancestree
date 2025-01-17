<script>
	import Panzoom from '../components/Panzoom.svelte';
</script>

<div class="flex h-full flex-col">
	<nav>
		<h1>Ancestree</h1>
	</nav>
	<div class="flex-1">
		<Panzoom>
			<rect class="person" x="50" y="100" />
		</Panzoom>
	</div>
</div>

<style scoped>
	.person {
		width: 100px;
		height: 50px;
		fill: black;
		rx: 15px;
	}
</style>
