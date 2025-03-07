<script lang="ts">
  import type { Snippet } from 'svelte';

  type Vertical = 't' | 'm' | 'b';
  type Horizontal = 'l' | 'm' | 'r';
  type Position = `${Vertical}${Horizontal}`;

  type Props = {
    title: string;
    children: Snippet<[]>;
    position?: Position;
  };

  const { title, children, position = 'mm' }: Props = $props();

  function positionClass(): string {
    let clazz = [];
    const vertical = position.charAt(0) as Vertical;
    const horizontal = position.charAt(1) as Horizontal;

    switch (vertical) {
      case 't':
        clazz.push('bottom-full');
        break;

      case 'm':
        clazz.push('top-1/2 -translate-y-1/2');
        break;

      case 'b':
        clazz.push('top-full');
        break;
    }

    switch (horizontal) {
      case 'l':
        clazz.push('right-1/2');
        break;

      case 'm':
        clazz.push('left-1/2 -translate-x-1/2');
        break;

      case 'r':
        clazz.push('left-1/2');
        break;
    }

    return clazz.join(" ");
  }
</script>

<div class="group relative">
  {@render children()}

  <div
    class={`${positionClass()} absolute w-full min-w-max rounded-md bg-black/90 p-2 text-sm text-white opacity-0 transition-opacity group-hover:opacity-100`}
  >
    {title}
  </div>
</div>
