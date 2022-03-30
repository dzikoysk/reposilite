import { Text } from "@chakra-ui/react"
import { StyledNode } from "./Flow"

const ReposiliteInstance = ({ number, status }) => (
  <Text>
    Reposilite {number} <br />
    {status === 'offline'
      ? <Text as='sup' color={'red.500'}>Offline</Text>
      : <Text as='sup' color={'green.500'}>Online</Text>
    }
  </Text>
)

const clusterFlow = [
  { id: 'r1', data: { label: <ReposiliteInstance number={1} /> }, position: { x: 80, y: 0 }, sourcePosition: 'left', targetPosition: 'right' },
  { id: 'r2', data: { label: <ReposiliteInstance number={2} /> }, position: { x: 0, y: 50 }, sourcePosition: 'bottom', targetPosition: 'top' },
  { id: 'r3', data: { label: <ReposiliteInstance number={3} status='offline' /> }, position: { x: 150, y: 50 }, sourcePosition: 'top', targetPosition: 'bottom' },
  { id: 'r4', data: { label: <ReposiliteInstance number={4} /> }, position: { x: 80, y: 100 }, sourcePosition: 'right', targetPosition: 'left' },
  { id: 'e1-2', source: 'r1', target: 'r2', animated: true },
  { id: 'e2-4', source: 'r2', target: 'r4', animated: true },
  { id: 'e4-3', source: 'r4', target: 'r3', animated: true },
  { id: 'e3-1', source: 'r3', target: 'r1', animated: true },
]

const ClusterGraph = () => (
  <StyledNode
    label={'K8S Cluster'}
    style={{ width: '230px', height: '145px' }}
    flow={clusterFlow}
  />
)

const integrationsFlow = [
  { id: 'shared', data: { label: <Text as='i'>Cloud integrations</Text> }, position: { x: 115, y: -10 }, sourcePosition: 'bottom', targetPosition: 'bottom' },
  { id: 's3', data: { label: 'S3 Object Storages' }, position: { x: 5, y: 50 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'sql', data: { label: 'SQL Database' }, position: { x: 128, y: 60 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'proxy', data: { label: 'Proxied hosts' }, position: { x: 230, y: 50 }, sourcePosition: 'top', targetPosition: 'top' },
  { id: 'shared-s3', source: 'shared', target: 's3', animated: true, type: 'smooth' },
  { id: 'shared-sql', source: 'shared', target: 'sql', animated: true, type: 'smooth' },
  { id: 'shared-proxy', source: 'shared', target: 'proxy', animated: true, type: 'smooth' },
]

const IntegrationsGraph = () => (
  <StyledNode
    style={{ width: '330px', height: '90px' }}
    flow={integrationsFlow}
  />
)

const cloudScenario = {
  name: 'Cloud',
  flow: [
    { id: 'maven', data: { label: <Text>Maven <br/> Clients</Text> }, position: { x: 0, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'ci', data: { label: <Text>CI & CD <br/> Workers</Text> }, position: { x: 360, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'cluster', data: { label: <ClusterGraph /> }, position: { x: 75, y: 100 } , sourcePosition: 'bottom', targetPosition: 'top'},
    { id: 'integrations', data: { label: <IntegrationsGraph /> }, position: { x: 25, y: 360 } , sourcePosition: 'top', targetPosition: 'top'},
    { id: 'maven-cluster', source: 'maven', target: 'cluster', animated: true, type: 'smooth', label: '[ Public ]' },
    { id: 'ci-cluster', source: 'ci', target: 'cluster', animated: true, type: 'smooth', label: '[ Private ]' },
    { id: 'cluster-integrations', source: 'cluster', target: 'integrations', animated: true, type: 'step', label: '[ Shared state ]' },
  ]
}

export {
  cloudScenario
}