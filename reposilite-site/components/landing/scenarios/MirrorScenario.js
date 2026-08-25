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

import { Text } from "@chakra-ui/react"
import { StyledNode } from "./Flow"

const LocalRepository = () => (
  <StyledNode
    label={<Text>Repository in <br />Reposilite instance</Text>}
    style={{ width: '100px', height: '40px' }}
  />
)

const ReposiliteInstance = () => (
  <StyledNode
    label={'Reposilite instance as proxy'}
    style={{ width: '330px', height: '90px' }}
  />
)

const mirrorScenario = {
  name: 'Mirror',
  nodes: [
    { id: 'maven', data: { label: <Text>Maven <br/> Clients</Text> }, position: { x: 0, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'ci', data: { label: <Text>CI & CD <br/> Workers</Text> }, position: { x: 360, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'reposilite', data: { label: <ReposiliteInstance /> }, position: { x: 115, y: 100 }, sourcePosition: 'bottom', targetPosition: 'top' },
    { id: 'central', data: { label: <Text>Maven Central</Text> }, position: { x: 155, y: 400}, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'private', data: { label: <Text>Private<br/>Repository</Text> }, position: { x: 10, y: 300}, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'local', data: { label: <LocalRepository /> }, position: { x: 290, y: 300 }, sourcePosition: 'top', targetPosition: 'top' },
  ],
  edges: [
    { id: 'maven-reposilite', source: 'maven', target: 'reposilite', animated: true, type: 'smooth', label: '[ Public ]' },
    { id: 'ci-reposilite', source: 'ci', target: 'reposilite', animated: true, type: 'smooth', label: '[ Private ]' },
    { id: 'central-reposilite', source: 'reposilite', target: 'central', animated: true, type: 'smooth', label: '~ Proxied ~' },
    { id: 'private-reposilite', source: 'reposilite', target: 'private', animated: true, type: 'smooth', label: '~ Proxied ~' },
    { id: 'local-reposilite', source: 'reposilite', target: 'local', type: 'smooth', label: '~ Local link  ~' },
  ]
}

export {
  mirrorScenario
}