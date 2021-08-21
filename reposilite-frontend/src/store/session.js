import { reactive, toRef, toRefs } from "vue"

const defaultValue = ''

const session = reactive({
  alias: defaultValue,
  token: defaultValue
})

const aliasKey = 'alias'
const tokenKey = 'token'

export default function useSession() {
  const login = (alias, token) => {
    localStorage.setItem(aliasKey, alias)
    session.alias = alias
    localStorage.setItem(tokenKey, token)
    session.token = token
  }

  const logout = () => {
    login(defaultValue, defaultValue)
  }

  const fetchSession = () => {
    login(
      localStorage.getItem(aliasKey),
      localStorage.getItem(tokenKey)
    )
  }

  const isLogged = () =>
    session.alias != defaultValue

  return {
    session,
    login,
    logout,
    fetchSession,
    isLogged
  }
}