import { Box, Container, Flex, Heading, Spacer, VStack, Text } from "@chakra-ui/react";
import { Link } from '../../components/Link'
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode";
import { chakra } from "../../helpers/chakra-theme";

const link = (title, url) =>
  ({ title, url: url || "/" })

const guideLinks = [
  link('Getting Started'),
  link('Installation'),
  link('Developer API'),
  link('FAQ'),
]

const communityLinks = [
  link('Report Issue'),
  link('Star Reposilite'),
  link('Join Discord'),
  link('Follow @dzikoysk'),
]

export default function Footer() {
  const [footerBg, footerBgCss] = useColorModeValue('footer-bg', chakra('colors-gray-100'), chakra('colors-gray-900'))

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
            Copyright © 2022 dzikoysk with ❤ panda-lang
          </Text>
        </Container>
      </Box>
    </>
  )
}
