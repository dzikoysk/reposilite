import {
  Box,
  Flex,
  Link,
  Button,
  useColorModeValue,
  Stack,
  HStack,
  useColorMode,
} from '@chakra-ui/react'
import { MoonIcon, SunIcon } from '@chakra-ui/icons'
import { FaGithub } from 'react-icons/fa'

const link = (label, url) =>
  ({ label, url })

const Links = [
  link('Guide', '/guide'),
  link('Plugins', '/plugins'),
  link('Support', '/support'),
  link('Contribute', 'https://github.com/dzikoysk/reposilite')
]

const NavLink = ({ link }) => (
  <Link
    px={2}
    py={1}
    rounded={'md'}
    _hover={{
      textDecoration: 'none',
      bg: useColorModeValue('gray.200', 'gray.700'),
    }}
    href={link.url}>
    {link.label}
  </Link>
)

const ThemeButton = () => {
  const { colorMode, toggleColorMode } = useColorMode()

  return (
    <Button aria-label='Switch color theme' onClick={toggleColorMode}>
      {colorMode === 'light' ? <MoonIcon /> : <SunIcon />}
    </Button>
  )
}

const GitHubButton = () => (
  <Link href='https://github.com/dzikoysk/reposilite'>
    <Button aria-label='Go to project on GitHub'>
      <FaGithub />
    </Button>
  </Link>
)
export default function Nav() {
  return (
    <Box bg={useColorModeValue('white', 'gray.900')} px={4}>
      <Flex h={16} alignItems={'center'} justifyContent={'space-between'}>
        <Link href='/'>
          <Box fontWeight={'bold'}>Reposilite</Box>
        </Link>

        <HStack as={'nav'} spacing={3} >
          {Links.map(link => (
            <NavLink key={link.label} link={link} />
          ))}
        </HStack>
  
        <Flex alignItems={'center'}>
          <Stack direction={'row'} spacing={4}>
            <ThemeButton />
            <GitHubButton />
          </Stack>
        </Flex>
      </Flex>
    </Box>
  )
}