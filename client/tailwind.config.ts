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
      red: '#ffada9',
      blue: '#b5e2ff',
      highlight: '#e8bf8b',
      highlight_border: '#2D2D2D',
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
