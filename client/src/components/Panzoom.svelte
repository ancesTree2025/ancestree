<script lang="ts">
  let { children } = $props();

  let parent = $state<HTMLDivElement | null>(null);
  let child = $state<SVGSVGElement | null>(null);
  let width = $state(0);
  let height = $state(0);

  let pan = $state([0, 0]);
  let zoom = $state(1);

  $effect(() => {
    if (child) {
      const transformations = child.transform.baseVal;
      while (transformations.length < 2) {
        transformations.appendItem(child.createSVGTransform());
      }

      child.transform.baseVal.getItem(0).setTranslate(pan[0], pan[1]);
      child.transform.baseVal.getItem(1).setScale(zoom, zoom);
    }
  });

  let mouseOffset: [number, number] = [0, 0];

  function getMouseOffset(e: MouseEvent): [number, number] {
    const rect = parent?.getBoundingClientRect();
    return [e.clientX - (rect?.left ?? 0), e.clientY - (rect?.top ?? 0)];
  }

  function toLocal([x, y]: [number, number]): [number, number] {
    return [(x - pan[0]) / zoom, (y - pan[1]) / zoom];
  }

  let wasMoved = true;
  let mouseDown = false;

  let panStart = [0, 0];

  let moveStart = [0, 0];

  function onMouseMove(e: MouseEvent) {
    mouseOffset = getMouseOffset(e);
    wasMoved = true;
    if (mouseDown) {
      pan[0] = panStart[0] + (mouseOffset[0] - moveStart[0]);
      pan[1] = panStart[1] + (mouseOffset[1] - moveStart[1]);
    }
    e.stopPropagation();
  }

  function onMouseDown(e: MouseEvent) {
    mouseDown = true;
    panStart = [pan[0], pan[1]];
    moveStart = getMouseOffset(e);
    e.stopPropagation();
  }

  function onMouseUp(e: MouseEvent) {
    mouseDown = false;
    e.stopPropagation();
  }

  let zoomStart = 0;

  let scrollDelta = 0;
  let scrollPivot = [0, 0];

  function onScroll(e: WheelEvent) {
    if (wasMoved) {
      scrollPivot = toLocal(mouseOffset);
      panStart = [pan[0], pan[1]];
      zoomStart = zoom;
      scrollDelta = 0;
    }

    scrollDelta += e.deltaY;

    const x = scrollPivot[0];
    const y = scrollPivot[1];
    const d = Math.pow(1.1, scrollDelta / 100);

    pan[0] = panStart[0] + x * zoomStart * (1 - d);
    pan[1] = panStart[1] + y * zoomStart * (1 - d);
    zoom = zoomStart * d;

    wasMoved = false;
    e.stopPropagation();
  }
</script>

<div
  class="panzoom-container"
  bind:clientWidth={width}
  bind:clientHeight={height}
  bind:this={parent}
  onwheel={onScroll}
  onmousemove={onMouseMove}
  onmousedown={onMouseDown}
  onmouseup={onMouseUp}
  role="presentation"
>
  <svg class="panzoom" bind:this={child}>
    {@render children()}
  </svg>
</div>

<style scoped>
  .panzoom-container {
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  .panzoom {
    transform-origin: 0 0;
  }
</style>
