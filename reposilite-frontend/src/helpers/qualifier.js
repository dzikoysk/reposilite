/*
 * Copyright (c) 2021 dzikoysk
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
import { useRoute } from 'vue-router'
import { useSession } from '../store/session'

const qualifier = reactive({
  watchable: 0,
  path: ''
})

const { details } = useSession()

watch(
  () => details.value,
  () => qualifier.watchable++
)

export default function useQualifier() {
  const route = useRoute()

  watch(
    () => route.params.qualifier,
    newQualifier => {
      qualifier.path = newQualifier
      qualifier.watchable++
    },
    { immediate: true }
  )

  return {
    qualifier
  }
}