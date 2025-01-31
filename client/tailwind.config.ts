import type { Config } from 'tailwindcss';

export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],

  theme: {
    fontFamily: {
      sans: ['Figtree', 'sans-serif']
    },
    colors: {
      white: '#fff',
      black: '#222',
      input: '#eee',
      node: '#fff',
      edge: '#222',
      red: '#f44',
      transparent: 'transparent'
    },
    extend: {
      strokeWidth: {
        line: '1'
      }
    }
  },

  plugins: []
} satisfies Config;
