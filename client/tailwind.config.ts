import type { Config } from 'tailwindcss';

export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],

  theme: {
    fontFamily: {
      sans: ['Figtree', 'sans-serif']
    },
    colors: {
      bg: '#ffffff',
      fg: '#000000',
      'node-bg': '#e0e0e0'
    }
  },

  plugins: []
} satisfies Config;
