import { reactive } from "vue"

const defaultValue = ''

const token = reactive({
  name: defaultValue,
  secret: defaultValue
})

const nameKey = 'session-token-name'
const secretKey = 'session-token-secret'

export default function useSession() {
  const login = (name, secret) => {
    localStorage.setItem(nameKey, name)
    token.name = name
    localStorage.setItem(secretKey, secret)
    token.secret = secret
  }

  const logout = () => {
    login(defaultValue, defaultValue)
  }

  const fetchSession = () => {
    login(
      localStorage.getItem(nameKey),
      localStorage.getItem(secretKey)
    )
  }

  const isLogged = () =>
    token.name != defaultValue

  return {
    token,
    login,
    logout,
    fetchSession,
    isLogged
  }
}