@raw
<script>
    const themeKey = 'color-theme'
    let darkThemeEnabled = localStorage.getItem(themeKey) === 'dark'

    if (!darkThemeEnabled && !(themeKey in localStorage)) {
        darkThemeEnabled = window.matchMedia('(prefers-color-scheme: dark)').matches
    }

    if (darkThemeEnabled) {
        document.documentElement.classList.add('dark')
        document.documentElement.classList.remove('light')
    } else {
        document.documentElement.classList.remove('dark')
        document.documentElement.classList.add('light')
    }
</script>
@endraw