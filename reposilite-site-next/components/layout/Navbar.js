import { ReactNode } from 'react';
import {
  Box,
  Flex,
  Avatar,
  Link,
  Button,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuDivider,
  useDisclosure,
  useColorModeValue,
  Stack,
  HStack,
  useColorMode,
  Center,
} from '@chakra-ui/react';
import { MoonIcon, SunIcon } from '@chakra-ui/icons';
import { FaGithub, FaTwitter } from 'react-icons/fa'

const Links = ['Guide', 'Plugins', 'Support', 'Contribute'];

const NavLink = ({ children }) => (
  <Link
    px={2}
    py={1}
    rounded={'md'}
    _hover={{
      textDecoration: 'none',
      bg: useColorModeValue('gray.200', 'gray.700'),
    }}
    href={'#'}>
    {children}
  </Link>
);

export default function Nav() {
  const { colorMode, toggleColorMode } = useColorMode()
  const { isOpen, onOpen, onClose } = useDisclosure()

  return (
    <Box bg={useColorModeValue('gray.50', 'gray.900')} px={4}>
      <Flex h={16} alignItems={'center'} justifyContent={'space-between'}>
        <Box fontWeight={'bold'}>Reposilite</Box>

        <HStack as={'nav'} spacing={3} >
          { Links.map((link) => (
            <NavLink key={link}>{link}</NavLink>
          )) }
        </HStack>
        
        <Flex alignItems={'center'}>
          <Stack direction={'row'} spacing={4}>
            <Button onClick={toggleColorMode}>
              {colorMode === 'light' ? <MoonIcon /> : <SunIcon />}
            </Button>
            <Button>
              <FaGithub />
            </Button>
          </Stack>
        </Flex>
      </Flex>
    </Box>
  )
}
