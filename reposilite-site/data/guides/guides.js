/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
