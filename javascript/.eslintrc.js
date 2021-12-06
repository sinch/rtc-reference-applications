module.exports = {
  env: {
    browser: true,
    serviceworker: true,
  },
  extends: ["airbnb-base", "prettier"],
  globals: {
    Sinch: true,
  },
  parserOptions: {
    ecmaVersion: 13,
    sourceType: "module",
  },
  rules: {
    "class-methods-use-this": "off",
    "no-console": "off",
    "import/extensions": "off",
    "no-new": "off",
  },
};
