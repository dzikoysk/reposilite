<template>
  <div class="container mx-auto pt-10 px-6">
    <div v-for="entry in configurations" :key="entry.type">
      <h1 class="text-lg font-bold">{{entry.type}}</h1>
      <pre class="my-5 py-5 px-6 rounded-lg shadow-md bg-gray-50 dark:bg-gray-900">{{trim(entry.snippet)}}</pre>
    </div>
  </div>
</template>

<script>
const configurations = [
  {
    type: 'Maven',
    snippet: `
    <repositories>
        <repository>
            <name>${window.REPOSILITE_TITLE}</name>
            <id>${window.REPOSILITE_ID}</id>
            <url>${window.location}</url>
        </repository>
    </repositories>
    `
  },
  {
    type: 'Gradle Groovy',
    snippet: `
    maven {
        url "${window.location}"
    }
    `
  },
  {
    type: 'Gradle Kotlin',
    snippet: `
    maven {
        url = uri("${window.location}")
    }
    `
  },
  {
    type: 'Panda',
    snippet: `
    repositories: [
        ${window.location}
    ]
    `
  },
  {
    type: 'SBT',
    snippet: `
    resolvers += "${window.REPOSILITE_TITLE}" at "${window.location}"
    `
  }
]

export default {
  setup() {
    return {
      configurations
    }
  },
  methods: {
    trim(snippet) {
      const indentation = snippet.length - snippet.trimStart().length - 1
      
      return snippet.split('\n')
        .map(line => line.substring(indentation))
        .join('\n')
        .trim()
    }
  }
}
</script>
