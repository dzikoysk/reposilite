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
      'general',
      'jar',
      'docker',
      'kubernetes',
    ]
  },
  {
    name: 'Infrastructure',
    directory: 'infrastructure',
    content: [
      'nginx',
      'caddy',
      'apache',
      'systemd',
      'archlinux',
      'nixos',
      'cloudflare',
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
      'sbt',
      'github',
      'ivy'
    ]
  },
  {
    name: 'Features',
    directory: 'features',
    content: [
      'dashboard',
      'repositories',
      's3',
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
