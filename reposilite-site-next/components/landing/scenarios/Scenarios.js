import React from 'react'
import { Tabs, TabList, TabPanels, Tab, TabPanel, useColorModeValue, Box } from '@chakra-ui/react'
import { LockedReactFlow } from './Flow'
import { cloudFlow } from './CloudScenario'

const scenarios = [
  {
    name: 'Direct',
    flow: [
      { id: '1', type: 'input', data: { label: 'Node 1' }, position: { x: 250, y: 5 } },
    ]
  },
  {
    name: 'Mirror',
    flow: [
      { id: '1', type: 'input', data: { label: 'Node 1' }, position: { x: 250, y: 5 } },
    ]
  },
  cloudFlow
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
                style={{ width: '100%', height: '500px' }}
              />
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </div>
  )
}