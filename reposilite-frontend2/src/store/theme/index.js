import { reactive, toRef, toRefs } from "vue"

const themeKey = 'dark-theme'
const theme = reactive({
  isDark: false
})

export default function useTheme() {
  const fetchTheme = () => {
    theme.isDark = (localStorage.getItem(themeKey) === 'true')
  }

  const toggleTheme = () => {
    theme.isDark = !theme.isDark
    localStorage.setItem(themeKey, theme.isDark)
  }

  return {
    theme,
    fetchTheme,
    toggleTheme
  }
}