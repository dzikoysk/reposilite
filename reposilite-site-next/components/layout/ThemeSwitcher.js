import { MoonIcon, SunIcon } from '@chakra-ui/icons'
import { Button } from '@chakra-ui/react'
import { useColorSwitcher } from 'nextjs-color-mode'

export default function ThemeButton({ style }) {
  const { toggleTheme, colorMode } = useColorSwitcher()

  localStorage.removeItem('chakra-ui-color-mode')

  return (
    <Button aria-label='Switch color theme' onClick={toggleTheme} style={style}>
      {colorMode === 'light' ? <MoonIcon /> : <SunIcon />}
    </Button>
  )
}