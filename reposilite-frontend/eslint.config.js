/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import js from "@eslint/js";
import pluginVue from 'eslint-plugin-vue'

export default [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    languageOptions: {
      parserOptions: {
        "ecmaVersion": 2020,
        "ecmaFeatures": {
          "jsx": true
        }
      },
    },
    rules: {
      'vue/script-setup-uses-vars': 'error',
      'vue/no-setup-props-destructure': 'error',
      'vue/no-unused-vars': 'error',
      'vue/multi-word-component-names': 'error',
      'vue/jsx-uses-vars': 'error',
      'semi': ['error', 'never'],
    }
  }
]
