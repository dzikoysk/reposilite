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

import { Box, Text } from '@chakra-ui/react'
import { ColorModeStyles, useColorModeValue } from 'nextjs-color-mode'
import ReactFlow, { ReactFlowProvider } from 'react-flow-renderer'
import { chakraColor } from '../../../helpers/chakra-theme'

const LockedReactFlow = ({ nodes, edges, style }) => {
  const [flowColor, flowColorCss] = useColorModeValue('flow-color', 'black', 'white')
  const [flowBg, flowBgCss] = useColorModeValue('flow-bg', 'white', chakraColor('gray.800'))

  return (
    <ReactFlowProvider>
      <ColorModeStyles styles={[flowColorCss, flowBgCss]} />
      <ReactFlow
        className='nowheel'
        nodes={nodes}
        edges={edges}
        style={style}
        nodesDraggable={false}
        draggable={false}
        contentEditable={false}
        panOnDrag={false}
        panOnScroll={false}
        zoomOnScroll={false}
        zoomOnPinch={false}
        zoomOnDoubleClick={false}
        connectionMode={false}
        nodesConnectable={false}
        elementsSelectable={false}
      />
      <style jsx global>{`
        .react-flow__node-input, .react-flow__node-default {
          background: none !important;
          color: ${flowColor} !important;
          width: auto !important;
          border: none !important;
          box-shadow: none !important;
          cursor: default !important;
        }
        .react-flow__edge-textwrapper {}
        .react-flow__edge-textbg {
          fill: ${flowBg} !important;
        }
        .react-flow__edge-text {
          fill: ${flowColor} !important;
        }
        // Reposilite is an open-source project, so it's eligble to hide the attribution from React Flow 10.x+
        // https://reactflow.dev/docs/introduction/#attribution
        .react-flow__attribution {
          display: none;
        }
      `}</style>
    </ReactFlowProvider>
  )
}

const StyledNode = ({ label, style, nodes, edges }) => {
  const title = label === undefined
    ? <></>
    : <Text as="i">{label}</Text>
  
  const flowComponent = nodes === undefined
    ? <></>
    : <LockedReactFlow
      nodes={nodes}
      edges={edges}
      style={style}
    />
  
  const [flowBg, flowBgCss] = useColorModeValue('styled-flow-bg', chakraColor('gray.100'), chakraColor('gray.900'))
  
  return (
    <>
      <ColorModeStyles styles={[flowBgCss]} />
      <Box paddingY={3} paddingX={2} borderRadius={50} style={{ backgroundColor: flowBg }}>
        {title}
        {flowComponent}
      </Box>
    </>
  )
}

export {
  LockedReactFlow,
  StyledNode
}