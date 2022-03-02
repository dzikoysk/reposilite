import { Container } from "@chakra-ui/react"
import Features from "./Features"
import Hero from "./Hero"

export default function Landing() {
  return (
    <Container maxW='container.lg'>
      <Hero />
      <Features />
    </Container>
  )
}