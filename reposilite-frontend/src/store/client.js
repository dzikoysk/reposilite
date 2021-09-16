import axios from 'axios'
import usePlaceholders from './placeholders'

const { basePath } = usePlaceholders()

const production = () =>
  window.location.protocol + '//' + location.host + basePath

const baseUrl = () =>
  process.env.NODE_ENV === 'production'
    ? (production().endsWith('/') ? production().slice(0, -1) : production())
    : 'http://localhost'

const createURL = (endpoint) =>
  baseUrl() + endpoint

const createClient = (defaultName, defaultSecret) => {
  const defaultAuthorization = () =>
    (defaultName && defaultSecret) ? authorization(defaultName, defaultSecret) : {}

  const authorization = (name, secret) => ({
    headers: {
      Authorization: `xBasic ${btoa(`${name}:${secret}`)}`
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
      content(gav) {
        return get(`/${gav}`)
      },
      details(gav) {
        return get(`/api/maven/details/${gav || ''}`)
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
  createClient
}