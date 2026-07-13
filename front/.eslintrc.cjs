module.exports = {
  ignorePatterns: ['dist/**', 'node_modules/**'],
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  globals: {
    afterEach: 'readonly',
    beforeEach: 'readonly',
    describe: 'readonly',
    expect: 'readonly',
    it: 'readonly',
    vi: 'readonly',
  },
  extends: ['eslint:recommended', 'plugin:react/recommended', 'plugin:react-hooks/recommended'],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  settings: {
    react: {
      version: 'detect',
    },
  },
  rules: {
    'react/react-in-jsx-scope': 'off',
    'react/prop-types': 'off',
  },
};
