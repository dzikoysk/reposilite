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

import { Box, Flex, Heading, Text } from "@chakra-ui/react"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { Link } from '../../components/Link'
import { chakra } from "../../helpers/chakra-theme"

const feature = (title, description) =>
  ({ title, description })

const features = [
  feature('Open Source', 'Personal access-token based authorization with configurable routes and permissions'),
  feature('Docker 🐋', <Box>Available docker images <Link color={'purple.400'} href="https://hub.docker.com/r/dzikoysk/reposilite">hub.docker.com/r/dzikoysk/reposilite</Link></Box>),
  feature('Authorization', 'Personal access-token based authorization with configurable routes and permissions'),
  feature('Dashboard', 'Simple dashboard with repository browser and management panel'),
  feature('API Endpoints', 'Exposes dedicated REST API to communicate with repository using external tools'),
  feature('Plugin system', 'Extend your instance with personalized extensions in Java, Kotlin or Groovy'),
  feature('Storage providers', 'Store artifacts locally or integrate your instance with cloud object storage like AWS S3'),
  feature('Proxy', 'Link other Maven repositories and redirect all traffic through your instance'),
  feature('And...', 'Much more, take a look at Guide section to learn more!')
]

// TODO - Better layout or sth idk
export default function Features() {
  const [featuresBg, featuresBgCss] = useColorModeValue('features-bg', chakra('colors-gray-50'), chakra('colors-gray-900'))
  
  return (
    <>
      <ColorModeStyles styles={[featuresBgCss]} />
      <Flex
        justifyContent={'space-between'}
        flexWrap='wrap'
        borderRadius='2xl'
        padding={'7'}
        style={{ backgroundColor: featuresBg }}
      >
        {features.map(({ title, description }) => (
          <Box key={title} width={'33%'} textAlign='center'>
            <Heading as='h4' size='sm' paddingTop='5'>{title}</Heading>
            <Box paddingY='3' paddingX='6' fontSize={'sm'}>
              {description}
            </Box>
          </Box>
        ))}
      </Flex>
    </>
  )
}
