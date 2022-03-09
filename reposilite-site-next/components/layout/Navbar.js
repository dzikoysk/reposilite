import { ColorModeStyles, useColorModeValue } from 'nextjs-color-mode'
import { Box, Flex, Link, Button, Stack, HStack } from '@chakra-ui/react'
import { FaGithub } from 'react-icons/fa'
import dynamic from 'next/dynamic'
import { chakraColor } from '../../helpers/chakra-theme'

const link = (label, url) =>
  ({ label, url })

const Links = [
  link('Guide', '/guide/about'),
  link('Plugins', '/plugins'),
  link('Support', '/support'),
  link('Contribute', 'https://github.com/dzikoysk/reposilite')
]

const NavLink = ({ link }) => {
  const [linkBg, linkBgCss] = useColorModeValue('nav-link-bg', 'white', chakraColor('gray.900'))
  const [hoverBg, hoverBgCss] = useColorModeValue('nav-link-hover-bg', chakraColor('gray.200'), chakraColor('gray.700'))

  return (
    <>
      <ColorModeStyles styles={[linkBgCss, hoverBgCss]}/>
      <Link
        href={link.url}
        px={2}
        py={1}
        rounded={'md'}
        backgroundColor={linkBg}
        _hover={{ textDecoration: 'none', bg: hoverBg }}
      >
        {link.label}
      </Link>
    </>
  )
}

const ThemeSwitcher = dynamic(() =>
  import('./ThemeSwitcher'), { ssr: false })

const GitHubButton = ({ style }) => (
  <Link href='https://github.com/dzikoysk/reposilite'>
    <Button aria-label='Go to project on GitHub' style={style}>
      <FaGithub />
    </Button>
  </Link>
)
export default function Nav() {
  const [navbarBg, navbarBgCss] = useColorModeValue('navbar-bg', 'white', chakraColor('gray.900'))
  const [buttonBg, buttonBgCss] = useColorModeValue('navbar-button-bg', chakraColor('gray.100'), chakraColor('gray.800'))

  return (
    <>
      <ColorModeStyles styles={[navbarBgCss, buttonBgCss]} />
      <Box style={{ backgroundColor: navbarBg }} px={10}>
        <Flex
          alignItems={'center'}
          justifyContent={'space-between'}
          direction={{ base: 'column', md: 'row' }}
        >
          <Link href='/' py={4}>
            <Box fontWeight={'bold'}>Reposilite</Box>
          </Link>
          <HStack as={'nav'} spacing={3} >
            {Links.map(link => (
              <NavLink key={link.label} link={link} />
            ))}
          </HStack>
          <Flex alignItems={'center'} py={4}>
            <Stack direction={'row'} spacing={4}>
              <ThemeSwitcher style={{ backgroundColor: buttonBg }} />
              <GitHubButton style={{ backgroundColor: buttonBg }} />
            </Stack>
          </Flex>
        </Flex>
      </Box>
    </>
  )
}