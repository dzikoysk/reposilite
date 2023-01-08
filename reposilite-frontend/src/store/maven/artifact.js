/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default function useArtifact() {
  const createArtifactSnippet = (lang, { groupId, artifactId, version }) => {
    switch (lang) {
      case "Maven": return `
<dependency>
  <groupId>${groupId}</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${version}</version>
</dependency>`.trim()
      case "Gradle Groovy": return `implementation "${groupId}:${artifactId}:${version}"`
      case "Gradle Kotlin": return `implementation("${groupId}:${artifactId}:${version}")`
      case "SBT": return `"${groupId}" %% "${artifactId}" %% "${version}"`
      default: return ""
    }
  }

  return {
    createArtifactSnippet
  }
}