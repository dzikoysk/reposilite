import axios from 'axios'

const baseUrl = () => {
  return process.env.NODE_ENV === 'production'
    ? Vue.prototype.$reposilite.basePath
    : 'http://localhost:80'
}

const client = {
  maven: {
    details(gav) {
      return axios.get(baseUrl() + "/api/maven/details/" + (gav || ""))
    }
  }
}

export default client