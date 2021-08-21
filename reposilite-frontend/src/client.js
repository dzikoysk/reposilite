import axios from 'axios'

const baseUrl = () => {
  return process.env.NODE_ENV === 'production'
    ? Vue.prototype.$reposilite.basePath
    : 'http://localhost:80'
}

const client = {
  auth: {
    me(alias, token) {
      return axios.get(baseUrl() + "/auth/me", {
        auth: {
          username: alias,
          password: token
        }
      })
    }
  },
  maven: {
    details(gav) {
      return axios.get(baseUrl() + "/api/maven/details/" + (gav || ""))
    }
  }
}

export default client