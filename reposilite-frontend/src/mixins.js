const url = process.env.NODE_ENV === 'production' ? '/' : 'http://localhost:80/'
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
        return '/'
      }

      if (!uri.startsWith('/')) {
        uri = '/' + uri
      }

      if (uri.length > 1 && uri.endsWith('/')) {
        uri = uri.substr(0, uri.length - 1)
      }

      return uri
    },
    parentPath () {
      const elements = ('/' + this.getQualifier()).split('/')
      elements.pop()
      const path = this.normalize(elements.join('/'))
      return path.length === 0 ? '/' : path
    },
    getQualifier () {
      return this.normalize(this.$route.params.qualifier)
    },
    url () {
      return url
    }
  }
}
