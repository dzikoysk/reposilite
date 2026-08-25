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

import { Container, Flex } from "@chakra-ui/react"
import Features from "./Features"
import Hero from "./Hero"

export default function Landing() {
  return (
    <Container maxW='container.lg' marginInlineStart={{ base: '0', sm: 'auto' }}>
      <Flex direction={'column'} justifyContent={'center'} >
        <Hero />
        <Features />
      </Flex>
    </Container>
  )
}