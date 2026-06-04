import { computed, watch } from 'vue'
import { createI18n } from 'vue-i18n'
import en from './locales/en'
import zhCN from './locales/zh-CN'

const fallbackLocale = 'en'
const storageKey = 'reposilite.locale'

const messages = {
  en,
  'zh-CN': zhCN
}

const availableLocales = [
  { code: 'en', name: 'English', shortName: 'EN' },
  { code: 'zh-CN', name: '简体中文', shortName: '中' }
]

const isAvailableLocale = (locale) =>
  Object.prototype.hasOwnProperty.call(messages, locale)

const getBrowserLocale = () => {
  const browserLocale = navigator.language || fallbackLocale

  if (browserLocale.toLowerCase().startsWith('zh')) {
    return 'zh-CN'
  }

  return fallbackLocale
}

const getInitialLocale = () => {
  const storedLocale = localStorage.getItem(storageKey)

  if (isAvailableLocale(storedLocale)) {
    return storedLocale
  }

  return getBrowserLocale()
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: getInitialLocale(),
  fallbackLocale,
  messages
})

const locale = i18n.global.locale

const setLocale = (newLocale) => {
  if (!isAvailableLocale(newLocale)) {
    return
  }

  locale.value = newLocale
  localStorage.setItem(storageKey, newLocale)
  document.documentElement.lang = newLocale
}

const toggleLocale = () => {
  setLocale(locale.value === 'en' ? 'zh-CN' : 'en')
}

watch(locale, (newLocale) => {
  localStorage.setItem(storageKey, newLocale)
  document.documentElement.lang = newLocale
})

document.documentElement.lang = locale.value

const localeLabel = computed(() =>
  availableLocales.find(entry => entry.code === locale.value)?.shortName
)

export {
  availableLocales,
  i18n,
  locale,
  localeLabel,
  setLocale,
  toggleLocale
}
