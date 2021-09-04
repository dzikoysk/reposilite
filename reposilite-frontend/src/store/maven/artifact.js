export default function useArtifact(qualifier) {
  const configurations = [
    { 
      name: 'Maven', 
      value: `
<dependency>
  <groupId>{groupId}</groupId>
  <artifactId>{artifactId}</artifactId>
  <version>{version}</version>
</dependency>`.trim()
    },
    { name: 'Gradle Groovy', value: `implementation "{groupId}:{artifactId}:{version}"` }, 
    { name: 'Gradle Kotlin', value: `implementation("{groupId}:{artifactId}:{version}")` },
    { name: 'SBT', value: `"{groupId}" %% "{artifactId}" %% "{version}"` }
  ]

  return {
    configurations
  }
}