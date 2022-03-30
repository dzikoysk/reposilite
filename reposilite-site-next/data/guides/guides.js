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
      'manual',
      'setup',
      'docker',
      //'k8s'
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
    name: 'Features',
    directory: 'features',
    content: [
      'dashboard',
      'repositories',
      'deployment',
      'proxy',
      'static-files',
      'plugins',
      'ssl'
    ]
  },
  {
    name: 'Integrations',
    directory: 'integrations',
    content: [
      'nginx',
      'apache',
      'cloudflare',
      'systemd',
      'github'
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
      'swagger',
    ]
  }
]

export {
  categories
}