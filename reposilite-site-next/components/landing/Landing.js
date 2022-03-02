import { Container } from "@chakra-ui/react"
import Features from "./Features"
import Hero from "./Hero"
import Preview from "./scenarios/Preview"

export default function Landing() {
  return (
    <Container maxW='container.lg' marginInlineStart={{ base: '0', sm: 'auto' }}>
      <Hero />
      <Preview />
      <Features />
    </Container>
  )
}