import { computed } from 'vue'

export default function useRepository() {
  const createRepositories = (qualifier) => {
    const repository = computed(() => qualifier.path.split('/')[0])
    const domain = location.protocol + '//' + location.host + (qualifier.path ? `/${repository.value}` : '/{repository}')

    return [
      {
      name: 'Maven',
        lang: 'xml',
          snippet: `
<repository>
  <name>${window.REPOSILITE_TITLE}</name>
  <id>${window.REPOSILITE_ID}</id>
  <url>${domain}</url>
</repository>
        `.trim()
    },
    {
      name: 'Gradle Groovy',
        lang: 'groovy',
          snippet: `maven {\n    url "${domain}"\n }`.trim()
    },
    {
      name: 'Gradle Kotlin',
        lang: 'kotlin',
          snippet: `maven {\n    url = uri("${domain}")\n}`
    },
    {
      name: 'SBT',
        lang: 'scala',
          snippet: `resolvers += "${window.REPOSILITE_ID}" at "${domain}"`
    }
    ]
  }

  return {
    createRepositories
  }
}