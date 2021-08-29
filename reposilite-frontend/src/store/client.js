import axios from 'axios'

const production = () =>
  window.location.protocol + '//' + location.host + window.REPOSILITE_BASE_PATH

const baseUrl = () =>
  process.env.NODE_ENV === 'production'
    ? (production().endsWith('/') ? production().slice(0, -1) : production())
    : 'http://localhost'

const createURL = (endpoint) =>
  baseUrl() + endpoint

const useClient = (defaultName, defaultSecret) => {
  const defaultAuthorization = () =>
    (defaultName && defaultSecret) ? authorization(defaultName, defaultSecret) : {}

  const authorization = (name, secret) => ({
    auth: {
      username: name,
      password: secret
    }
  })
  
  const get = (endpoint, credentials) => {
    credentials = credentials || defaultAuthorization()
    return axios.get(createURL(endpoint), { ...credentials })
  }

  const client = {
    auth: {
      me(name, secret) {
        return get("/api/auth/me", authorization(name, secret))
      }
    },
    console: {
    },
    maven: {
      details(gav) {
        return get("/api/maven/details/" + (gav || ''))
      }
    }
  }

  return {
    createURL,
    client
  }
}

export {
  createURL,
  useClient
}