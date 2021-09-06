import { computed } from 'vue'
import usePlaceholders from '../placeholders'

export default function useRepository() {
  const { id, title } = usePlaceholders()

  const createRepositories = (qualifier) => {
    const repository = computed(() => qualifier.path.split('/')[0])
    const domain = location.protocol + '//' + location.host + (qualifier.path ? `/${repository.value}` : '/{repository}')

    return [
      {
      name: 'Maven',
        lang: 'xml',
          snippet: `
<repository>
  <id>${id}</id>
  <name>${title}</name>
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
          snippet: `resolvers += "${id}" at "${domain}"`
    }
    ]
  }

  return {
    createRepositories
  }
}