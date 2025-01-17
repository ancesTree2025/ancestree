import globals from 'globals';
import pluginJs from '@eslint/js';
import tseslint from 'typescript-eslint';
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
			}
		}
	},
	pluginJs.configs.recommended,
	...tseslint.configs.recommended,
	...eslintPluginSvelte.configs['flat/recommended']
];
