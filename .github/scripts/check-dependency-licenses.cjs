/*
 * Copyright (c) 2020-2026 dzikoysk
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

const changes = JSON.parse(process.env.LICENSE_CHANGES)

for (const category of ['forbidden', 'unresolved', 'unlicensed']) {
  if (!Array.isArray(changes[category])) {
    throw new Error(`Missing dependency review results: ${category}`)
  }
}

const failures = [...changes.forbidden, ...changes.unresolved].filter(change => {
  if (typeof change.manifest !== 'string') {
    throw new Error('Dependency review result has no manifest')
  }
  return !change.manifest.startsWith('reposilite-site/')
})

for (const change of failures) {
  console.error(`${change.manifest}: ${change.name}@${change.version} (${change.license})`)
}
if (failures.length) {
  process.exitCode = 1
}
