import { Box, Text, useColorModeValue } from '@chakra-ui/react'
import ReactFlow, { ReactFlowProvider } from 'react-flow-renderer'

const LockedReactFlow = ({ elements, style }) => (
  <ReactFlowProvider>
    <ReactFlow
      className='nowheel'
      elements={elements}
      style={style}
      nodesDraggable={false}
      draggable={false}
      contentEditable={false}
      paneMoveable={false}
      panOnScroll={false}
      zoomOnScroll={false}
      zoomOnPinch={false}
      zoomOnDoubleClick={false}
      connectionMode={false}
      nodesConnectable={false}
      nodesDraggable={false}
      elementsSelectable={false}
    />
    <style jsx global>{`
      .react-flow__node-input, .react-flow__node-default {
        background: none !important;
        color: ${useColorModeValue('black', 'white')} !important;
        width: auto !important;
        border: none !important;
        box-shadow: none !important;
        cursor: default !important;
      }
      .react-flow__edge-textwrapper {}
      .react-flow__edge-textbg {
        fill: var(${useColorModeValue('--chakra-colors-white', '--chakra-colors-gray-800')}) !important;
      }
      .react-flow__edge-text {
        fill: ${useColorModeValue('black', 'white')} !important;
      }
    `}</style>
  </ReactFlowProvider>
)

const StyledNode = ({ label, style, flow }) => {
  const title = label === undefined
    ? <></>
    : <Text as="i">{label}</Text>
  
  const flowComponent = flow === undefined
    ? <></>
    : <LockedReactFlow
      elements={flow}
      style={style}
    />
  
  return (
    <Box paddingY={3} paddingX={2} borderRadius={50} backgroundColor={useColorModeValue('gray.100', 'gray.900')}>
      {title}
      {flowComponent}
    </Box>
  )
}

export {
  LockedReactFlow,
  StyledNode
}