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

import { Box, Button, Flex, Heading, Text } from "@chakra-ui/react"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { Link } from '../../components/Link'
import { chakraColor } from "../../helpers/chakra-theme"
import Scenarios from "./scenarios/Scenarios"

const HeroButton = ({ label, description, url, style }) => (
  <Link href={url} width='48%'>
    <Button aria-label={description} width='full' style={style}>
      {label}
    </Button>
  </Link>
)

const HeroDescription = ({ style }) => {
  const [ startBg, startBgCss ] = useColorModeValue('get-started-bg', chakraColor('purple.100'), chakraColor('purple.500'))
  const [ color, colorCss ] = useColorModeValue('hero-color', chakraColor('black'), chakraColor('gray.200'))
  const [ downloadBg, downloadBgCss ] = useColorModeValue('download-bg', chakraColor('gray.100'), chakraColor('gray.500'))
  
  return (
    <Box style={style}>
      <ColorModeStyles styles={[startBgCss, colorCss, downloadBgCss]} />
      <Box paddingX={{ base: '0', md: '7' }}>
        <Heading>
          Reposilite <Text as="u">3.x</Text>
        </Heading>
        <Text fontWeight={'bold'} paddingTop={6}>
          Lightweight and easy-to-use repository manager for Maven-based artifacts in the JVM ecosystem.
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
            url='/guide/about'
            style={{
              color,
              backgroundColor: startBg
            }}
          />
          <HeroButton
            label='Download'
            description='Download Reposilite'
            url='https://github.com/dzikoysk/reposilite/releases'
            style={{
              color,
              backgroundColor: downloadBg
            }}
          />
        </Flex>
      </Box>
    </Box>
  )
}

const HeroScenarios = () => (
  <Box paddingTop={{ base: 8, lg: 0}}>
    <Scenarios />
  </Box>
)

export default function Hero() {
  return (
    <Flex
      direction={{ base: 'column', lg: 'row' }}
      paddingTop={{ base: 6, lg: 20 }}
      paddingBottom={8}
      justifyContent={'center'}
    >
      <HeroDescription />
      <HeroScenarios />
    </Flex>
  )
}
