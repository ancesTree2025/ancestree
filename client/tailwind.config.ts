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
      pink: '#ffada9',
      red: '#f44',
      blue: '#b5e2ff',
      highlight: '#e8bf8b',
      orange: 'oklch(0.705 0.213 47.604)',
      cream: '#f5ebda',
      'dark-gray': '#423d3d',
      highlight_border: '#2D2D2D',
      transparent: 'transparent'
    },
    extend: {
      strokeWidth: {
        line: '4',
        'line-border': '12'
      }
    }
  },

  plugins: []
} satisfies Config;
