/*
 * Copyright (c) 2022 dzikoysk
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

import { computed } from "vue"
import usePlaceholders from "../../store/placeholders"

export default function useRepository() {
  const { basePath, id, title } = usePlaceholders()

  const createRepositories = (qualifier) => {
    const repository = computed(() => qualifier.path.split("/")[0])
    const repoId = id + (qualifier.path ? `-${repository.value}` : "")
    const domain =
      location.protocol +
      "//" +
      location.host +
      basePath +
      (basePath.endsWith("/") ? "" : "/") +
      (qualifier.path ? `${repository.value}` : "{repository}")

    return [
      {
        name: "Maven",
        lang: "xml",
        snippet: `
<repository>
  <id>${repoId}</id>
  <name>${title}</name>
  <url>${domain}</url>
</repository>`.trim(),
      },
      {
        name: "Gradle Groovy",
        lang: "groovy",
        snippet: `maven {\n    url "${domain}"\n}`.trim(),
      },
      {
        name: "Gradle Kotlin",
        lang: "kotlin",
        snippet: `maven(url = "${domain}") {\n    name = "${title}"\n}`,
      },
      {
        name: "SBT",
        lang: "scala",
        snippet: `resolvers +=\n  "${repoId}" \n     at "${domain}"`,
      },
    ]
  }

  return {
    createRepositories,
  }
}
