/*
 * Copyright (c) 2023 dzikoysk
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

import { computed } from 'vue'
import { i18n } from '../i18n'

export default function usePlaceholders() {
  const { t } = i18n.global
  const available = !'{{REPOSILITE.BASE_PATH}}'.includes('REPOSILITE.BASE_PATH')
  const basePath = available ? '{{REPOSILITE.BASE_PATH}}' : '/'
  const id = available ? '{{REPOSILITE.ID}}' : 'reposilite-repository'
  const title = computed(() => available ? '{{REPOSILITE.TITLE}}' : t('app.defaultTitle'))
  const description = computed(() => available ? '{{REPOSILITE.DESCRIPTION}}' : t('app.defaultDescription'))
  const organizationWebsite = available ? '{{REPOSILITE.ORGANIZATION_WEBSITE}}' : location.protocol + '//' + location.host + basePath
  const organizationLogo = available ? '{{REPOSILITE.ORGANIZATION_LOGO}}' : 'https://avatars.githubusercontent.com/u/75123628?s=200&v=4'
  const privacyPolicy = available ? '{{REPOSILITE.PRIVACY_POLICY}}' : 'https://example.com/privacy'
  const icpLicense = available ? '{{REPOSILITE.ICP_LICENSE}}' : '国ICP备000000000号'
  const javadocEnabled = available ? '{{REPOSILITE.JAVADOC_ENABLED}}' === 'true' : true

  const productionUrl =
    window.location.protocol + '//' + location.host + basePath

  const baseUrl =
    process.env.NODE_ENV === 'production'
      ? (productionUrl.endsWith('/') ? productionUrl.slice(0, -1) : productionUrl)
      : 'http://localhost:8080'
  
  return {
    available,
    basePath,
    id,
    title,
    description,
    organizationWebsite,
    organizationLogo,
    privacyPolicy,
    icpLicense,
    javadocEnabled,
    productionUrl,
    baseUrl
  }
}
