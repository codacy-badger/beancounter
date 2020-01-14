module.exports = {
    parser: "@typescript-eslint/parser",  // Specifies the ESLint parser
    "extends": [
        "plugin:react/recommended", // Uses the recommended rules from @eslint-plugin-react
        "plugin:@typescript-eslint/recommended", // Uses the recommended rules from the @typescript-eslint/eslint-plugin
        "prettier/@typescript-eslint", // Uses eslint-config-prettier to disable ESLint rules from @typescript-eslint/eslint-plugin that would conflict with prettier
        "plugin:prettier/recommended" // Enables eslint-plugin-prettier and eslint-config-prettier. This will display prettier errors as ESLint errors. Make sure this is always the last configuration in the extends array.
    ],
    "plugins": ["@typescript-eslint", "react-hooks"],
    settings: {
        "react": {
            "version": "detect"
        }
    },

    parserOptions: {
        ecmaFeatures: {
            jsx: true // Allows for the parsing of JSX
        },
        ecmaVersion: 2018,  // Allows for the parsing of modern ECMAScript features
        sourceType: "module"  // Allows for the use of imports
    },

    rules: {
        "strict": "error",
        "@typescript-eslint/explicit-member-accessibility": [1, { accessibility: 'no-public' }],
        "react/jsx-sort-default-props": ["error", { "ignoreCase": false }],
        "max-len": [
            "error",
            {
                "code": 100
            }
        ],
        "require-await": "error",
        "no-func-assign": "error",
        "object-shorthand": [
            "error",
            "methods",
            { "avoidExplicitReturnArrows": false }
        ],
        "prefer-const": ["error", {
            "destructuring": "any",
            "ignoreReadBeforeAssign": false
        }],
        "no-useless-return": "error",
        "no-else-return": "error",
        "no-return-await": "error",
        "no-var": "error",
        '@typescript-eslint/explicit-function-return-type': ['warn', {
            allowExpressions: true,
            allowTypedFunctionExpressions: true
        }],
        "@typescript-eslint/no-explicit-any": "off",
        "react-hooks/rules-of-hooks": "error",
        "react-hooks/exhaustive-deps": "warn"
    },
    env: {
        "es6": true,
        "browser": true,
        "node": true,
        "jest": true
    }
};