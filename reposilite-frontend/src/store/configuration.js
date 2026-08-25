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

import {computed, markRaw, ref, toRaw} from 'vue'
import { useSession } from './session'
import { createToast } from 'mosha-vue-toastify'
import { vanillaRenderers } from '@jsonforms/vue-vanilla'
import Ajv2020 from 'ajv/dist/2020'
import addFormats from 'ajv-formats'
import { default as ObjectRenderer, tester as objectTester } from '../components/renderers/ObjectRenderer.vue'
import { default as AllOfRenderer, tester as allOfTester } from '../components/renderers/AllOfRenderer.vue'
import { default as ArrayListRenderer, tester as arrayListTester } from '../components/renderers/ArrayListRenderer.vue'
import { default as OneOfRenderer, tester as oneOfTester } from '../components/renderers/OneOfRenderer.vue'
import { default as ConstantRenderer, tester as constantTester } from '../components/renderers/ConstantRenderer.vue'
import { default as OptionalRenderer, tester as optionalTester } from '../components/renderers/OptionalRenderer.vue'

const { client } = useSession()
const domains = ref([])
const schemas = ref({})
const configurations = ref({})
const selectedDomain = ref('')
const isLoading = ref(false)

const fetchConfiguration = () => {
  isLoading.value = true
  return client.value.settings.domains()
    .then(domainsResponse =>
      Promise.all(domainsResponse.data.map(domain =>
        client.value.settings.schema(domain)
          .then(schemaResponse => schemas.value[domain] = schemaResponse.data)
          .then(() => client.value.settings.fetch(domain))
          .then(configurationResponse => configurations.value[domain] = configurationResponse.data)
      )).then(() => domains.value = domainsResponse.data)
    )
    .then(() => selectedDomain.value = domains.value[0])
    .then(() => createToast('Configuration loaded', { type: 'success' }))
    .catch(error => createToast(`${error || ''}`, { type: 'danger' }))
    .finally(() => isLoading.value = false)
}

const updateConfiguration = () =>
  Promise.all(domains.value.map(domain =>
    client.value.settings.update(domain, toRaw(configurations.value[domain]))
      .then(() => client.value.settings.fetch(domain))
      .then(response => configurations.value[domain] = response.data)
  ))
    .then(() => createToast('Configuration updated', { type: 'success' }))
    .catch(error => createToast(`${error || ''}`, { type: 'danger' }))

const renderers = markRaw([
  { tester: arrayListTester, renderer: ArrayListRenderer },
  { tester: allOfTester, renderer: AllOfRenderer },
  { tester: oneOfTester, renderer: OneOfRenderer },
  { tester: constantTester, renderer: ConstantRenderer },
  { tester: optionalTester, renderer: OptionalRenderer },
  {
    // needed because without it hangs TODO find out why
    tester: (uischema, schema) => {
      let rank = objectTester(uischema, schema)
      return rank === -1 || schema.title === 'Proxied Maven Repository' ? -1 : rank 
    },
    renderer: ObjectRenderer
  },
  ...vanillaRenderers,
])

const configurationValidator = computed(() => {
  const ajv = new Ajv2020({
    allErrors: true,
    verbose: true,
    strict: false,
    addUsedSchema: false,
    useDefaults: true,
    removeAdditional: false,
    formats: {
      'repositories.storageProvider.quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
      'repositories.id': {
        type: 'string',
        validate: (name) => name in configurations.value['maven'].repositories || name.startsWith(' ') || name.endsWith(' ')
      },
      'repositories.proxied.allowedGroups': /^(\w+\.)*\w+$/,
    }
  })
  addFormats(ajv)
  ajv.addFormat("repositories.id", {
    type: "string",
    validate: (value) => !value.endsWith(' ')
  })
  return ajv
})

export function useConfiguration() {
  return {
    fetchConfiguration,
    updateConfiguration,
    renderers,
    configurationValidator,
    domains,
    configurations,
    schemas,
    selectedDomain,
    isLoading
  }
}
