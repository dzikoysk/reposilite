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
    name: 'Integrations',
    directory: 'integrations',
    content: [
      'nginx',
      'apache',
      'caddy',
      'systemd',
      'archlinux',
      'nixos',
      'cloudflare',
      'github',
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
      'javadocs',
      'static-files',
      'badges',
      'plugins',
      'ssl'
    ]
  },
  {
    name: 'Migration',
    directory: 'migration',
    content: [
      'reposilite-2.x',
      'artifactory',
      'nexus-3'
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
