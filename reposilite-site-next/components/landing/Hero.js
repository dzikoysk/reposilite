import { Box, Button, Container, Flex, Heading, Text, useColorModeValue } from "@chakra-ui/react"
import Features from "./Features"
import Scenarios from "./scenarios/Scenarios"

export default function Hero() {
  return (
    <Box paddingY={'24'}>
      <Flex direction={'row'}>
        <Box paddingX={7}>
          <Heading>
            Reposilite <Text as="u">3.x</Text>
          </Heading>
          <Text fontWeight={'bold'} paddingTop={6}>
            Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem.
          </Text>
          <Text paddingTop={2}>
            <Text color="purple.200" fontStyle={'italic'}>
              Simple, extensible, scalable and self-hosted
            </Text>
            alternative to managers like
            Nexus, Archiva or Artifactory, with reduced resources consumption. 📦
          </Text>
          <Flex paddingTop={7} justifyContent={'space-between'}>
            <Button w='48%'>Get started</Button>
            <Button w='48%'>Download</Button>
          </Flex>
        </Box>
        <Box minWidth="515px" paddingX={7}>
          <Scenarios />
        </Box>
      </Flex>
    </Box>
  )
}