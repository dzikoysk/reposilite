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

import { watch, reactive } from 'vue'
import { createSharedComposable } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'

const useQualifier = createSharedComposable(() => {
  const route = useRoute()
  const router = useRouter()

  const qualifier = reactive({
    watchable: 0,
    path: ''
  })

  const refreshQualifier = () =>
    qualifier.watchable++

  const getParentPath = () =>
    `/${(qualifier.path.endsWith('/') ? qualifier.path.slice(0, -1) : qualifier.path)}`
      .split("/")
      .slice(0, -1)
      .join('/') || '/'

  const redirectTo = (path) =>
    router.push(path)

  watch(
    () => route.params.qualifier,
    newQualifier => {
      qualifier.path = newQualifier
      refreshQualifier()
    },
    { immediate: true }
  )

  return {
    qualifier,
    getParentPath,
    refreshQualifier,
    redirectTo
  }
})

export default useQualifier
