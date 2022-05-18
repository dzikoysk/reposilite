import { Text, Code } from "@chakra-ui/react"
import { StyledNode } from "./Flow"

const localReposiliteFlow = {
  nodes: [
    { id: 'local', data: { label: <Text as='i' fontWeight={'bold'}>Local Reposilite Instance</Text> }, position: { x: 95, y: -10 }, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'fs', data: { label: <Text>Local FileSystem</Text> }, position: { x: 0, y: 50 }, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'sql', data: { label: 'Embedded SQL' }, position: { x: 60, y: 100 }, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'memory', data: { label: 'Low consumption' }, position: { x: 240, y: 50 }, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'cache', data: { label: 'Cloned artifacts' }, position: { x: 180, y: 100 }, sourcePosition: 'top', targetPosition: 'top' },
  ],
  edges: [
    { id: 'local-fs', source: 'local', target: 'fs', animated: true, type: 'smooth' },
    { id: 'local-sql', source: 'local', target: 'sql', animated: true, type: 'smooth' },
    { id: 'local-memory', source: 'local', target: 'memory', animated: true, type: 'smooth' },
    { id: 'local-cache', source: 'local', target: 'cache', animated: true, type: 'smooth' },
  ]
}

const LocalReposiliteGraph = () => (
  <StyledNode
    style={{ width: '350px', height: '130px' }}
    nodes={localReposiliteFlow.nodes}
    edges={localReposiliteFlow.edges}
  />
)

const directScenario = {
  name: 'Direct',
  nodes: [
    { id: 'maven', data: { label: <Text>Maven <br /> Clients</Text> }, position: { x: 0, y: 5 }, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'ci', data: { label: <Text>CI & CD <br /> Workers</Text> }, position: { x: 360, y: 5 }, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'reposilite', data: { label: <LocalReposiliteGraph /> }, position: { x: 20, y: 130 }, sourcePosition: 'bottom', targetPosition: 'top' },
    { id: 'api', data: { label: <Text>Reposilite API</Text> }, position: { x: 167, y: 340 }, sourcePosition: 'bottom', targetPosition: 'top' },
    { id: 'rest', data: { label: <Text>Public API</Text> }, position: { x: 55, y: 420 }, sourcePosition: 'top', targetPosition: 'top' },
    { id: 'plugins', data: { label: <Text>Plugins</Text> }, position: { x: 300, y: 420 }, sourcePosition: 'top', targetPosition: 'top' },
  ],
  edges: [
    { id: 'maven-reposilite', source: 'maven', target: 'reposilite', animated: true, type: 'smooth', label: '[ Public ]' },
    { id: 'ci-reposilite', source: 'ci', target: 'reposilite', animated: true, type: 'smooth', label: '[ Private ]' },
    { id: 'rest-api', source: 'api', target: 'rest', animated: true, markerEnd: { type: 'arrowclosed' }, type: 'smooth' },
    { id: 'plugins-api', source: 'api', target: 'plugins', animated: true, markerEnd: { type: 'arrowclosed' }, type: 'smooth' },
    // React Flow does not respect styles.height, so the this label won't be visible:
    { id: 'reposilite-api', source: 'reposilite', target: 'api', markerEnd: { type: 'arrowclosed' }, animated: true, type: 'step', label: '~ Possibilities ~', style: { stroke: '#9f7aea' } },
  ]
}

export {
  directScenario
}