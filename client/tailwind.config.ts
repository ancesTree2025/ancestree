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
      gray: '#f3f4f6',
      node: '#ddd',
      red: '#f44',
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
