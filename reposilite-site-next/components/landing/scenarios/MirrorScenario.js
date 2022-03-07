import { Text } from "@chakra-ui/react"
import { StyledNode } from "./Flow"

const ReposiliteInstance = () => (
  <StyledNode
    label={'Reposilite Instance Someday Soon Tm #TODO'}
    style={{ width: '330px', height: '90px' }}
  />
)

const mirrorScenario = {
  name: 'Mirror',
  flow: [
    { id: 'maven', data: { label: <Text>Maven <br/> Clients</Text> }, position: { x: 0, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'ci', data: { label: <Text>CI & CD <br/> Workers</Text> }, position: { x: 360, y: 5}, sourcePosition: 'bottom', targetPosition: 'bottom' },
    { id: 'cluster', data: { label: <ReposiliteInstance /> }, position: { x: 75, y: 100 } , sourcePosition: 'bottom', targetPosition: 'top'},
  ]
}

export {
  mirrorScenario
}