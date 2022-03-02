import { Flex, Box } from "@chakra-ui/react"
import Footer from "../components/layout/Footer"
import Landing from "../components/landing/Landing"
import Nav from "../components/layout/Navbar"
import { useEffect } from "react"

export default function Home() {
  useEffect(() => {
    // ReactFlow always registers wheel event, so we have to disable this globally
    // ~ https://github.com/wbkd/react-flow/blob/4b043bceccf41ae1edbd52e2919e1bf5a65aef99/src/container/FlowRenderer/index.tsx#L119
    window.addEventListener('wheel', event => event.stopImmediatePropagation(), true)
  })

  return (
    <Flex minH={'100vh'} flexDirection='column' flexWrap={'wrap'} >
      <Nav />
      <Landing />
      <Footer />
    </Flex>
  )
}
