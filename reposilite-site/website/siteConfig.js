const siteConfig = {
  title: 'Reposilite',
  tagline: 'Lightweight repository manager for Maven artifacts',
  url: 'https://reposilite.com',
  baseUrl: '/',
  copyright: `Copyright © ${new Date().getFullYear()} dzikoysk and ❤ panda-lang organization`,

  projectName: 'reposilite-site',
  organizationName: 'panda-lang',

  headerLinks: [
    { doc: 'about', label: 'Guide' },
    { page: 'help', label: 'Help' },
    { href: 'https://panda-lang.org/support', label: 'Support' },
    { href: 'https://github.com/dzikoysk/reposilite', label: 'GitHub' }
  ],

  headerIcon: '',
  footerIcon: 'img/favicon.png',
  favicon: 'img/favicon.png',

  colors: {
    primaryColor: '#101357',
    secondaryColor: '#fff'
  },

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks.
    theme: 'default'
  },

  scripts: ['https://buttons.github.io/buttons.js'],
  onPageNav: 'separate',
  cleanUrl: true,

  ogImage: 'img/undraw_online.svg',
  twitterImage: 'img/undraw_tweetstorm.svg',
  twitterUsername: 'dzikoysk'
}

module.exports = siteConfig
