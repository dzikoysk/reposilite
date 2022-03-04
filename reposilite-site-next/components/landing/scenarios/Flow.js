import { Box, Text } from '@chakra-ui/react'
import { ColorModeStyles, useColorModeValue } from 'nextjs-color-mode'
import ReactFlow, { ReactFlowProvider } from 'react-flow-renderer'
import { chakraColor } from '../../../helpers/chakra-theme'

const LockedReactFlow = ({ elements, style }) => {
  const [flowColor, flowColorCss] = useColorModeValue('flow-color', 'black', 'white')
  const [flowBg, flowBgCss] = useColorModeValue('flow-bg', 'white', chakraColor('gray.800'))

  return (
    <ReactFlowProvider>
      <ColorModeStyles styles={[flowColorCss, flowBgCss]} />
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
      `}</style>
    </ReactFlowProvider>
  )
}

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