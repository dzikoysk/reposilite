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