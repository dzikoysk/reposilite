import { Box, Button, Flex, Heading, Text, useColorModeValue } from "@chakra-ui/react"
import Scenarios from "./scenarios/Scenarios"

const HeroButton = ({ label, description, color, url }) => (
  <Button aria-label={description} width='48%' backgroundColor={color} href={url}>
    {label}
  </Button>
)

const HeroDescription = () => (
  <Box paddingX={{ base: '0', md: '7' }}>
    <Heading>
      Reposilite <Text as="u">3.x</Text>
    </Heading>
    <Text fontWeight={'bold'} paddingTop={6}>
      Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem.
    </Text>
    <Text paddingTop={2}>
      <Text as='i' color="purple.400">Simple, extensible, scalable and self-hosted</Text>
      &nbsp;alternative to managers like
      Nexus, Archiva or Artifactory, with reduced resources consumption written in Kotlin 📦
    </Text>
    <Flex paddingTop={7} justifyContent={'space-between'}>
      <HeroButton
        label='Get started'
        description='Get started - Learn about Reposilite'
        color={useColorModeValue('purple.100', 'purple.500')}
        url='/guide'
      />
      <HeroButton
        label='Download'
        description='Download Reposilite'
        url='https://github.com/dzikoysk/reposilite/releases'
      />
    </Flex>
  </Box>
)

const HeroScenarios = () => (
  <Box minWidth="515px" paddingX={7} paddingY={{ base: '5', md: '0'}}>
    <Scenarios />
  </Box>
)

export default function Hero() {
  return (
    <Flex direction={{ base: 'column', md: 'row' }} paddingTop={'20'} paddingBottom={'8'} justifyContent={'center'}>
      <HeroDescription />
      <HeroScenarios />
    </Flex>
  )
}