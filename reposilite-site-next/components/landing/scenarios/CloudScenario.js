import { Box, Text } from "@chakra-ui/react"
import { LockedReactFlow, StyledNode } from "./Flow"

const clusterFlow = [
  { id: 'r1', data: { label: 'Reposilite 1' }, position: { x: 80, y: 0 }, sourcePosition: 'left', targetPosition: 'right' },
  { id: 'r2', data: { label: 'Reposilite 2' }, position: { x: 0, y: 50 }, sourcePosition: 'bottom', targetPosition: 'top' },
  { id: 'r3', data: { label: 'Reposilite 3' }, position: { x: 150, y: 50 }, sourcePosition: 'top', targetPosition: 'bottom' },
  { id: 'r4', data: { label: 'Reposilite 4' }, position: { x: 80, y: 100 }, sourcePosition: 'right', targetPosition: 'left' },
  { id: 'e1-2', source: 'r1', target: 'r2', animated: true },
  { id: 'e2-4', source: 'r2', target: 'r4', animated: true },
  { id: 'e4-3', source: 'r4', target: 'r3', animated: true },
  { id: 'e3-1', source: 'r3', target: 'r1', animated: true },
]

const ClusterGraph = () => (
  <StyledNode
    label={'K8S Cluster'}
    style={{ width: '230px', height: '130px' }}
    flow={clusterFlow}
  />
)

const integrationsFlow = [
  { id: 'shared', data: { label: 'Cloud integrations' }, position: { x: 115, y: -10 }, sourcePosition: 'bottom', targetPosition: 'bottom' },
  { id: 's3', data: { label: 'S3 Object Storages' }, position: { x: 5, y: 55 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'sql', data: { label: 'SQL Database' }, position: { x: 128, y: 70 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'proxy', data: { label: 'Proxied repositories' }, position: { x: 225, y: 55 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'shared-s3', source: 'shared', target: 's3', animated: true, type: 'smooth' },
  { id: 'shared-sql', source: 'shared', target: 'sql', animated: true, type: 'smooth' },
  { id: 'shared-proxy', source: 'shared', target: 'proxy', animated: true, type: 'smooth' },
]

const IntegrationsGraph = () => (
  <StyledNode
    style={{ width: '350px', height: '100px' }}
    flow={integrationsFlow}
  />
)

const cloudFlow = {
  name: 'Cloud',
  flow: [
    { id: 'maven', data: { label: <Text>Maven <br/> Clients</Text> }, position: { x: 0, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'ci', data: { label: <Text>CI & CD <br/> Workers</Text> }, position: { x: 360, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'cluster', data: { label: <ClusterGraph /> }, position: { x: 75, y: 100 } , sourcePosition: 'bottom', targetPosition: 'top'},
    { id: 'integrations', data: { label: <IntegrationsGraph /> }, position: { x: 15, y: 350 } , sourcePosition: 'top', targetPosition: 'top'},
    { id: 'maven-cluster', source: 'maven', target: 'cluster', animated: true, type: 'smooth', label: '[ Public ]' },
    { id: 'ci-cluster', source: 'ci', target: 'cluster', animated: true, type: 'smooth', label: '[ Private ]' },
    { id: 'cluster-integrations', source: 'cluster', target: 'integrations', animated: true, type: 'step', label: '[ Shared state ]' },
  ]
}

export {
  cloudFlow
}