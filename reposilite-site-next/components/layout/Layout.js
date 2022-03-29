import { Box, Flex } from "@chakra-ui/react"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { chakraColor } from "../../helpers/chakra-theme"
import Footer from "./Footer"
import Nav from "./Navbar"

export default function Layout({ children }) {
  const [layoutBg, layoutBgCss] = useColorModeValue('layout-bg', 'white', chakraColor('gray.800'))
  const [layoutColor, layoutColorCss] = useColorModeValue('layout-color', 'black', 'white')

  return (
    <>
      <ColorModeStyles styles={[layoutBgCss, layoutColorCss]} />
      <Flex
        style={{ background: layoutBg, color: layoutColor }}
        minH={'100vh'}
        flexDirection='column'
      >
        <Nav />
        <Box minH={'70vh'}>
          {children}
        </Box>
        <Footer />
      </Flex>
    </>
  )
}