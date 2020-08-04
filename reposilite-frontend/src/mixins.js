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

const url = process.env.NODE_ENV === 'production' ? '{{REPOSILITE.BASE_PATH}}' : 'http://localhost:80/'
const apiUrl = url + 'api'

export default {
  methods: {
    api (uri, auth) {
      return this.$http.get(apiUrl + uri, {
        auth: {
          username: auth.alias,
          password: auth.token
        }
      })
    },
    normalize (uri) {
      if (uri === undefined) {
        return '{{REPOSILITE.BASE_PATH}}'
      }

      if (!uri.startsWith('/')) {
        uri = '/' + uri
      }

      if (!uri.endsWith('/')) {
        uri += '/'
      }

      return uri
    },
    parentPath () {
      const elements = this.splitQualifier()
      elements.pop()

      const path = this.normalize(elements.join('/'))
      return path.length === 0 ? '/' : path
    },
    splitQualifier () {
      const qualifier = this.getQualifier()
      const elements = qualifier.split('/')

      if (qualifier.endsWith('/')) {
        elements.pop()
      }

      return elements
    },
    getQualifier () {
      return this.normalize(this.$route.params.qualifier)
    },
    url () {
      return url
    }
  }
}
