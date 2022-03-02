import React from 'react'
import { Tabs, TabList, TabPanels, Tab, TabPanel, useColorModeValue, Box } from '@chakra-ui/react'
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
    <div>
      <Tabs
        align="center"
        variant='soft-rounded'
        colorScheme='purple'
        isLazy
        // backgroundColor={'black'}
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
        <TabPanels padding={0}>
          {scenarios.map(scenario => (
            <TabPanel key={scenario.name} paddingY={0}>
              <LockedReactFlow
                elements={scenario.flow}
                style={{ width: '100%', height: '490px' }}
              />
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </div>
  )
}