import NextLink from "next/link"
import {
  Link as ChakraLink,
  LinkOverlay as ChakraLinkOverlay,
} from "@chakra-ui/react"

export function Link({ children, href, nextProps, ...chakraLinkProps }) {
  return (
    <NextLink {...nextProps} href={href} passHref>
      <ChakraLink {...chakraLinkProps}>{children}</ChakraLink>
    </NextLink>
  )
}

export function LinkOverlay({
  children, 
  href,
  nextProps,
  ...chakraLinkOverlayProps
}) {
  return (
    <NextLink {...nextProps} href={href} passHref>
      <ChakraLinkOverlay {...chakraLinkOverlayProps}>
        {children}
      </ChakraLinkOverlay>
    </NextLink>
  )
}

export { LinkBox } from "@chakra-ui/react"
