import type { Config } from 'tailwindcss';

export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],

  theme: {
    fontFamily: {
      sans: ['Figtree', 'sans-serif']
    },
    colors: {
      bg: '#ffffff',
      fg: '#222',
      'input-bg': '#eee',
      'node-bg': '#ddd',
      line: '#ddd',
      transparent: 'transparent'
    },
    extend: {
      strokeWidth: {
        line: '3'
      }
    }
  },

  plugins: []
} satisfies Config;
