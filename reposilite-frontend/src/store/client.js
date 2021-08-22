import axios from 'axios'

export default function useClient(defaultAlias, defaultToken) {
  const baseUrl = () =>
    process.env.NODE_ENV === 'production'
      ? Vue.prototype.$reposilite.basePath
      : 'http://localhost:80'

  const defaultAuthorization = () =>
    (defaultAlias && defaultToken) ? authorization(defaultAlias, defaultToken) : {}

  const authorization = (alias, token) => ({
    auth: {
      username: alias,
      password: token
    }
  })
  
  const get = (endpoint, credentials) => {
    credentials = credentials || defaultAuthorization()
    return axios.get(baseUrl() + endpoint, { ...credentials })
  }

  const client = {
    auth: {
      me(alias, token) {
        return get("/auth/me", authorization(alias, token))
      }
    },
    maven: {
      details(gav) {
        return get("/api/maven/details/" + (gav || ''))
      }
    }
  }

  return {
    client
  }
}