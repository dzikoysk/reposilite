module.exports = {
  env: {
    node: true,
    'vue/setup-compiler-macros': true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
  ],
  rules: {
    // override/add rules settings here, such as:
    'vue/script-setup-uses-vars': 'error',
    'vue/no-setup-props-destructure': 'error'
  }
}