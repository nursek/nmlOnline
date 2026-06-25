// @ts-check
import angularEslint from 'angular-eslint';
import tseslint from 'typescript-eslint';
import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import eslintPluginPrettier from 'eslint-plugin-prettier';

/**
 * Flat ESLint config for Angular 22.
 *
 * - `angular-eslint` provides the TS + template parser/plugin and recommended rule sets.
 * - `typescript-eslint` drives type-aware TS linting.
 * - Prettier is run via `eslint-plugin-prettier` (formatting as a lint warning),
 *   with `eslint-config-prettier` disabling conflicting stylistic rules.
 */
export default tseslint.config(
  {
    ignores: ['dist/', 'node_modules/', 'coverage/', '**/*.spec.ts'],
  },

  // TypeScript project files (components, directives, services, guards, ...)
  {
    files: ['src/**/*.ts'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      ...angularEslint.configs.tsRecommended,
    ],
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        project: './tsconfig.app.json',
      },
    },
    plugins: {
      '@typescript-eslint': tseslint.plugin,
      prettier: eslintPluginPrettier,
    },
    rules: {
      'prettier/prettier': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],

      // Angular 22 best-practices enforcement (signal-first, control-flow-first)
      '@angular-eslint/prefer-signals': 'warn',
      '@angular-eslint/prefer-host-metadata-property': 'warn',
      '@angular-eslint/prefer-inject': 'warn',
      '@angular-eslint/prefer-output-readonly': 'warn',
      '@angular-eslint/no-uncalled-signals': 'warn',
      '@angular-eslint/no-inputs-metadata-property': 'warn',
      '@angular-eslint/no-outputs-metadata-property': 'warn',
      '@angular-eslint/no-empty-lifecycle-method': 'warn',
      '@angular-eslint/no-async-lifecycle-method': 'warn',
      '@angular-eslint/no-lifecycle-call': 'warn',
      '@angular-eslint/contextual-lifecycle': 'warn',
      '@angular-eslint/use-lifecycle-interface': 'warn',
      '@angular-eslint/use-injectable-provided-in': 'warn',
    },
  },

  // Component / directive / pipe HTML templates
  {
    files: ['src/**/*.html'],
    extends: [
      ...angularEslint.configs.templateRecommended,
      ...angularEslint.configs.templateAccessibility,
    ],
    languageOptions: {
      parserOptions: {
        project: './tsconfig.app.json',
      },
    },
    rules: {
      '@angular-eslint/template/prefer-control-flow': 'warn',
      '@angular-eslint/template/no-empty-control-flow': 'warn',
      '@angular-eslint/template/use-track-by-function': 'warn',
      '@angular-eslint/template/no-call-expression': 'warn',
      '@angular-eslint/template/prefer-class-binding': 'warn',
    },
  },

  prettier,
);