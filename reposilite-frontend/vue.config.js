/*
 * Copyright (c) 2020 Dzikoysk
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

module.exports = {
  publicPath:
    process.env.NODE_ENV === 'production'
      ? '{{REPOSILITE.VUE_BASE_PATH}}'
      : '/',
  outputDir: '../reposilite-backend/src/main/resources/frontend/',
  filenameHashing: false,
  productionSourceMap: false,
  css: {
    extract: false
  },
  chainWebpack: config => {
    config.optimization.delete('splitChunks')

    // Vue removes quotes from attributes which causes bugs in placeholders with spaces
    // ~ https://github.com/dzikoysk/reposilite/issues/209
    config.plugin('html')
      .tap(args => {
        if (args[0].minify) {
          args[0].minify.removeAttributeQuotes = false;
        }
        return args;
    })
  }
}
