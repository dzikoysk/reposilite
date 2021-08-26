import { reactive, ref } from "vue"
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

const tokenInfo = ref({
  id: defaultValue,
  name: defaultValue,
  createdAt: defaultValue,
  permissions: [],
  routes: []
})

const session = reactive({
  token,
  tokenInfo
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
  }

  const login = async (name, secret) => {
    try {
      const { client } = useClient()

      if (name == defaultValue) {
        throw new Error("Missing credentials")
      }

      const response = await client.auth.me(name, secret)
      updateToken(name, secret)
      tokenInfo.value = response.data
      return { token, tokenInfo }
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

  const isManager = () =>
    tokenInfo.value.permissions.includes(managerPermission)
  
  return {
    token,
    tokenInfo,
    session,
    login,
    logout,
    fetchSession,
    isLogged,
    isManager
  }
}