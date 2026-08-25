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

const createSemverComparator = (reversed, filter, toSemver) => {
  // Semver sorting
  // ~ https://github.com/substack/semver-compare/issues/1#issuecomment-594765531
  const compare = (rawA, rawB) => {
    const a = rawA.split('-')
    const b = rawB.split('-')
    const pa = a[0].split('.')
    const pb = b[0].split('.')
    
    for (let idx = 0; idx < 3; idx++) {
        const na = Number(pa[idx])
        const nb = Number(pb[idx])
        if (na > nb) return 1
        if (nb > na) return -1
        if (!isNaN(na) && isNaN(nb)) return 1
        if (isNaN(na) && !isNaN(nb)) return -1
    }
    if (a[1] && b[1]) {
        return a[1] > b[1] ? 1 : (a[1] < b[1] ? -1 : 0)
    }
    return !a[1] && b[1] ? 1 : (a[1] && !b[1] ? -1 : 0)
  }

  return (a, b) => {
    if (!filter(a, b)) return 0
    const result = compare(toSemver(a), toSemver(b))
    return reversed ? -result : result
  }
}

export {
  createSemverComparator
}