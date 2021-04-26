const colors = require('windicss/colors')

module.exports = {
  darkMode: "class",
  theme: {
    screens: {
      sm: '640px',
      md: '768px',
      lg: '1024px',
      xl: '1280px',
    },
    extend: {
      colors: {
        gray: colors.trueGray
      },
      fontSize: {
        xm: ['0.625rem', { lineHeight: '0.75rem' }],
      }
    }
  }
}