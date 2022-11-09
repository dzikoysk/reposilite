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
