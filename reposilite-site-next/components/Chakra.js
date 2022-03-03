// ~ https://chakra-ui.com/docs/features/color-mode#add-colormodemanager-optional-for-ssr
import { ChakraProvider, cookieStorageManager, localStorageManager } from '@chakra-ui/react'

export function Chakra({ cookies, children }) {
  const colorModeManager =
    typeof cookies === 'string'
      ? cookieStorageManager(cookies)
      : localStorageManager

  return (
    <ChakraProvider colorModeManager={colorModeManager}>
      {children}
    </ChakraProvider>
  )
}

export function getServerSideProps({ req }) {
  return {
    props: {
      cookies: req.headers.cookie ?? '',
    }
  }
}