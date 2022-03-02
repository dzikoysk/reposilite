import { Flex, Box } from "@chakra-ui/react"
import Footer from "../components/layout/Footer"
import Landing from "../components/landing/Landing"
import Nav from "../components/layout/Navbar"

export default function Home() {
  return (
    <Flex minH={'100vh'} flexDirection='column' flexWrap={'wrap'} >
      <Nav />
      <Landing />
      <Footer />
    </Flex>
  )
}
