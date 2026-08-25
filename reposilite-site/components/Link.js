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

import NextLink from "next/link"
import {
  Box,
  // Link as ChakraLink,
  LinkOverlay as ChakraLinkOverlay,
} from "@chakra-ui/react"

export function Link({ children, href, nextProps, ...chakraLinkProps }) {
  return (
    <Box {...chakraLinkProps} style={{ display: 'inline'}}>
      <NextLink {...nextProps} href={href} passHref>
        {children}
      </NextLink>
    </Box>
  )
}

export function LinkOverlay({
  children, 
  href,
  nextProps,
  ...chakraLinkOverlayProps
}) {
  return (
    <Box {...chakraLinkOverlayProps} style={{ display: 'inline'}}>
      <NextLink {...nextProps} href={href} passHref>
        {children}
      </NextLink>
    </Box>
    
  )
}

export { LinkBox } from "@chakra-ui/react"
