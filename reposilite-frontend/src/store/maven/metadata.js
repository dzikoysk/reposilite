/*
 * Copyright (c) 2021 dzikoysk
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

const parser = new DOMParser()

export default function useMetadata() {
  const parseMetadata = (source) =>
    parser.parseFromString(source, 'text/xml')
  
  const groupId = (metadata) =>
    metadata
      ?.getElementsByTagName('groupId')[0]
      ?.firstChild
      ?.nodeValue
  
  const artifactId = (metadata) =>
    metadata
      ?.getElementsByTagName('artifactId')[0]
      ?.firstChild
      ?.nodeValue
  
  const versions = (metadata) =>
    Array.from(
      metadata
        ?.getElementsByTagName('versioning')[0]
        ?.children)
      ?.map(node => node.firstChild.nodeValue)
      ?? ['{unknown}']

  return {
    parseMetadata,
    groupId,
    artifactId,
    versions
  }
}