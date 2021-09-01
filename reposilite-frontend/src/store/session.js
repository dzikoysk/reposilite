import { isRef, reactive, ref } from "vue"
import { useClient } from './client'

const defaultValue = ''
const nameKey = 'session-token-name'
const secretKey = 'session-token-secret'
const managerPermission = 'access-token:manager'
const readPermission = 'route:read'
const writePermission = 'route:write'

const token = reactive({
  name: defaultValue,
  secret: defaultValue
})

const defaultTokenInfo = {
  id: defaultValue,
  name: defaultValue,
  createdAt: defaultValue,
  permissions: [],
  routes: []
}

const session = reactive({
  tokenInfo: defaultTokenInfo
})

export default function useSession() {
  const updateToken = (name, secret) => {
    localStorage.setItem(nameKey, name)
    token.name = name
    localStorage.setItem(secretKey, secret)
    token.secret = secret
  }

  const logout = () => {
    updateToken(defaultValue, defaultValue)
    session.tokenInfo = defaultTokenInfo
  }

  const login = async (name, secret) => {
    try {
      const { client } = useClient()

      if (name == defaultValue) {
        throw new Error("Missing credentials")
      }

      const response = await client.auth.me(name, secret)
      updateToken(name, secret)
      session.tokenInfo = response.data
      return { token, session }
    } catch (error) {
      logout()
      throw error
    }
  }

  const fetchSession = () => {
    return login(
      localStorage.getItem(nameKey),
      localStorage.getItem(secretKey)
    )
  }

  const isLogged = () =>
    token.name != defaultValue

  const isManager = (tokenInfo) =>
    tokenInfo?.permissions?.find(entry => entry.identifier == managerPermission)
  
  return {
    token,
    session,
    login,
    logout,
    fetchSession,
    isLogged,
    isManager
  }
}