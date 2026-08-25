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

import React from 'react'
import { Tabs, TabList, TabPanels, Tab, TabPanel, Box, } from '@chakra-ui/react'
import { LockedReactFlow } from './Flow'
import { directScenario } from './DirectScenario'
import { cloudScenario } from './CloudScenario'
import { mirrorScenario } from './MirrorScenario'

const scenarios = [
  directScenario,
  mirrorScenario,
  cloudScenario
]

export default function Scenarios() {
  return (
    <Box>
      <Tabs
        isLazy
        variant='soft-rounded'
        colorScheme='purple'
        // defaultIndex={scenarios.length - 1}
        align="center"
        padding={0}
        margin={0}
      >
        <TabList>
          {scenarios.map(scenario => (
            <Tab
              key={scenario.name}
              paddingTop={'0'}
              paddingBottom={'0'}
            >
              {scenario.name}
            </Tab>
          ))}
        </TabList>
        <TabPanels padding={0} margin={0}>
          {scenarios.map(scenario => (
            <TabPanel
              key={scenario.name}
              padding={0}
              margin={0}
              paddingInlineStart={0}
            >
              <LockedReactFlow
                nodes={scenario.nodes}
                edges={scenario.edges}
                style={{ width: '420px', height: '490px' }}
              />
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </Box>
  )
}