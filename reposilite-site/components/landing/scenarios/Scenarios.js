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
                elements={scenario.flow}
                style={{ width: '420px', height: '490px' }}
              />
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </Box>
  )
}