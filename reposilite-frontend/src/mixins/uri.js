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

import Vue from 'vue'

export default {
  methods: {
    /**
     * Make authorized get request to api
     * @param {string} uri the api endpoint qualifier
     * @param {object} auth object with 'alias' and 'token' properties
     */
    api (uri, auth) {
      return this.$http.get(this.baseApiUrl() + uri, {
        auth: {
          username: auth.alias,
          password: auth.token
        }
      })
    },
    /**
     * Normalize uri:
     * * returns base path if uri is undefined
     * * adds / at the beginning of uri if not defined
     * * adds / at the end of uri if not defined
     * @param {string} uri to normalize
     */
    normalize (uri) {
      if (uri === undefined) {
        return Vue.prototype.$reposilite.basePath
      }

      if (!uri.startsWith('/')) {
        uri = '/' + uri
      }

      if (!uri.endsWith('/')) {
        uri += '/'
      }

      return uri
    },
    /**
     * Get parent uri
     */
    parentPath () {
      const elements = this.splitQualifier()
      elements.pop()

      const path = this.normalize(elements.join('/'))
      return path.length === 0 ? '/' : path
    },
    /**
     * Split normalized uri
     */
    splitQualifier () {
      const qualifier = this.getQualifier()
      const elements = qualifier.split('/')

      if (qualifier.endsWith('/')) {
        elements.pop()
      }

      return elements
    },
    /**
     * Get normalized uri as qualifier
     */
    getQualifier () {
      return this.normalize(this.$route.params.qualifier)
    },
    /**
     * Get Reposilite base API url
     */
    baseApiUrl () {
      return this.baseUrl() + 'api'
    },
    /**
     * Get Reposilite base url
     */
    baseUrl () {
      return process.env.NODE_ENV === 'production'
        ? Vue.prototype.$reposilite.basePath
        : 'http://localhost:80/'
    }
  }
}
