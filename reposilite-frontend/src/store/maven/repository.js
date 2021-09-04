export default function useRepository(qualifier) {
  const trim = (snippet) => {
    const indentation = snippet.length - snippet.trimStart().length - 1
    
    return snippet.split('\n')
      .map(line => line.substring(indentation))
      .join('\n')
      .replace('#/', '')
      .trim()
  }

  const domain = location.protocol + '//' + location.host

  const configurations = [
    {
      type: 'Maven',
      snippet: `
      <repository>
          <name>${window.REPOSILITE_TITLE}</name>
          <id>${window.REPOSILITE_ID}</id>
          <url>${domain}</url>
      </repository>
      `
    },
    {
      type: 'Gradle Groovy',
      snippet: `
      maven {
          url "${domain}"
      }
      `
    },
    {
      type: 'Gradle Kotlin',
      snippet: `
      maven {
          url = uri("${domain}")
      }
      `
    },
    {
      type: 'SBT',
      snippet: `
      resolvers += "${window.REPOSILITE_TITLE}" at "${domain}"
      `
    }
  ]

  return {
    configurations,
    trim
  }
}