export default function useArtifact() {
  const createSnippets = (groupId, artifactId, version) => [
    { 
      name: 'Maven',
      lang: 'xml',
      snippet: `
<dependency>
  <groupId>${groupId}</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${version}</version>
</dependency>`.trim()
    },
    {
      name: 'Gradle Groovy',
      lang: 'xml',
      snippet: `implementation "${groupId}:${artifactId}:${version}"`
    },
    {
      name: 'Gradle Kotlin',
      lang: 'kotlin',
      snippet: `implementation("${groupId}:${artifactId}:${version}")`
    },
    {
      name: 'SBT',
      lang: 'scala',
      snippet: `"${groupId}" %% "${artifactId}" %% "${version}"`
    }
  ]

  return {
    createSnippets
  }
}