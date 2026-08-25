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

import { Box, Container, Flex, Heading, Spacer, VStack, Text } from "@chakra-ui/react"
import { Link } from '../../components/Link'
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { chakraColor } from "../../helpers/chakra-theme"

const link = (title, url) =>
  ({ title, url: url || "/" })

const guideLinks = [
  link('Getting Started', '/guide/about'),
  link('Installation', '/guide/general'),
  link('Plugins', '/plugin'),
  link('Developer API', '/guide/sources'),
]

const communityLinks = [
  link('Report Issue', 'https://github.com/dzikoysk/reposilite/issues/new/choose'),
  link('Join Discord', 'https://discord.gg/qGRqmGjUFX'),
  link('Visit Twitter', 'https://twitter.com/dzikoysk'),
  link('Go to GitHub', 'https://github.com/dzikoysk/reposilite'),
]

export default function Footer() {
  const [footerBg, footerBgCss] = useColorModeValue('footer-bg', chakraColor('gray.100'), chakraColor('gray.900'))

  return (
    <>
      <ColorModeStyles styles={[footerBgCss ]} />
      <Box style={{ backgroundColor: footerBg }} marginTop={'12'}>
        <Container>
          <Flex justifyContent={'center'} paddingY={10}>
            <VStack>
              <Heading fontSize={16}>Guide</Heading>
              {guideLinks.map(({ title, url }) => (
                <Link key={title} href={url} fontSize={'sm'}>{title}</Link>
              ))}
            </VStack>
            <Spacer />
            <VStack>
              <Heading fontSize={16}>Community</Heading>
              {communityLinks.map(({ title, url }) => (
                <Link key={title} href={url} fontSize={'sm'}>{title}</Link>
              ))}
            </VStack>
          </Flex>
          <Text textAlign={'center'} paddingY='4' fontSize={'sm'}>
            Copyright © 2020-2026 dzikoysk
          </Text>
        </Container>
      </Box>
    </>
  )
}
