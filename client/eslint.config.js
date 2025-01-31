import pluginJs from '@eslint/js';
import tsEslint from 'typescript-eslint';
import * as svelteParser from 'svelte-eslint-parser';
import * as typescriptParser from '@typescript-eslint/parser';
import eslintPluginSvelte from 'eslint-plugin-svelte';

/** @type {import('eslint').Linter.Config[]} */
export default [
  {
    languageOptions: {
      parser: svelteParser,
      parserOptions: {
        parser: typescriptParser,
        project: './tsconfig.json',
        extraFileExtensions: ['.svelte']
      },
      globals: {
        fetch: 'readonly'
      }
    },
    rules: {
      // note you must disable the base rule
      // as it can report incorrect errors
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': [
        'error', // or "warn"
        {
          argsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_'
        }
      ],
      eqeqeq: ['error', 'smart'],
      'no-unneeded-ternary': ['error'],
      camelcase: ['error'],
      'func-style': ['error', 'declaration'],
      'prefer-const': ['error', {
        'destructuring': 'all'
      }]
    }
  },
  pluginJs.configs.recommended,
  ...tsEslint.configs.recommended,
  ...eslintPluginSvelte.configs['flat/recommended']
];
