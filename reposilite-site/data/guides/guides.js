const categories = [
  {
    name: 'Introduction',
    directory: 'introduction',
    content: [
      'about',
      // 'comparison',
      'support'
    ]
  },
  {
    name: 'Installation',
    directory: 'installation',
    content: [
      'settings',
      'standalone',
      'docker',
      'kubernetes',
    ]
  },
  {
    name: 'Authentication',
    directory: 'authentication',
    content: [
      'tokens',
      'routes',
      'ldap'
    ]
  },
  {
    name: 'Deployment',
    directory: 'deployment',
    content: [
      'gradle',
      'maven',
      'sbt'
    ]
  },
  {
    name: 'Features',
    directory: 'features',
    content: [
      'dashboard',
      'repositories',
      'mirrors',
      'static-files',
      'badges',
      'plugins',
      'ssl'
    ]
  },
  {
    name: 'Integrations',
    directory: 'integrations',
    content: [
      'nginx',
      'caddy',
      'apache',
      'cloudflare',
      'systemd',
      'github'
    ]
  },
  {
    name: 'Migration',
    directory: 'migration',
    content: [
      'reposilite-2.x',
      'artifactory'
    ]
  },
  {
    name: 'Developers',
    directory: 'developers',
    content: [
      'sources',
      'kotlin',
      'plugin-api',
      'endpoints',
    ]
  }
]

export {
  categories
}
