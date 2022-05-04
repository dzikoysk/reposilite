import {computed, markRaw, ref, toRaw} from 'vue'
import { useSession } from './session'
import {createToast} from 'mosha-vue-toastify'
import { createAjv } from '@jsonforms/core'

import { vanillaRenderers } from '@dzikoysk/vue-vanilla'

import { default as ObjectRenderer, tester as objectTester } from '../components/renderers/ObjectRenderer.vue'
import { default as AllOfRenderer, tester as allOfTester } from '../components/renderers/AllOfRenderer.vue'
import { default as OneOfRenderer, tester as oneOfTester } from '../components/renderers/OneOfRenderer.vue'
import { default as ConstantRenderer, tester as constantTester } from '../components/renderers/ConstantRenderer.vue'
import { default as OptionalRenderer, tester as optionalTester } from '../components/renderers/OptionalRenderer.vue'

const { client } = useSession()
const domains = ref([])
const schemas = ref({})
const configurations = ref({})
const selectedDomain = ref('')

const getSchema = async (name) =>
  (await client.value.schema.get(name)).data

const listConfigs = async () =>
  (await client.value.config.list()).data

const getConfig = async (name) =>
  (await client.value.config.get(name)).data

const fetchConfiguration = async () => {
  try {
    const fetchedConfiguration = {}
    const fetchedSchemas = {}
    domains.value = await listConfigs()
    for (const domain of domains.value) {
      fetchedConfiguration[domain] = await getConfig(domain)
      fetchedSchemas[domain] = await getSchema(domain)
    }
    configurations.value = fetchedConfiguration
    schemas.value = fetchedSchemas
    selectedDomain.value = domains.value[0]
    createToast('Configuration loaded', { type: 'success' })
  } catch (error) {
    createToast(`${error || ''}`, { type: 'danger' })
  }
}

const updateConfiguration = async () => {
  try {
    const fetchedConfigurations = []
    const errored = []
    for (const domain of domains.value) {
      await client.value.config.put(domain, toRaw(configurations.value[domain]))
      fetchedConfigurations[domain] = await getConfig(domain)
    }
    configurations.value = fetchedConfigurations
    if (errored.length > 0) {
      createToast(`Failed to update ${errored.join(', ')}`, { type: 'danger' })
    } else {
      createToast('Configuration updated', { type: 'success' })
    }
  } catch (error) {
    createToast(`${error || ''}`, { type: 'danger' })
  }
}

const renderers = markRaw([
  { tester: allOfTester, renderer: AllOfRenderer },
  { tester: oneOfTester, renderer: OneOfRenderer },
  { tester: constantTester, renderer: ConstantRenderer },
  { tester: optionalTester, renderer: OptionalRenderer },
  {
    tester: (uischema, schema) => {
      let x = objectTester(uischema, schema)
      return x === -1 || schema.title === 'Proxied Maven Repository' ? -1 : x  // needed because without it hangs TODO find out why
    },
    renderer: ObjectRenderer
  },
  ...(vanillaRenderers.filter(element => element.renderer.name != "label-renderer"))
])

const configurationValidator = computed(() => createAjv({
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in configurations.value['maven'].repositories
    }
  }
}))

export function useConfiguration() {
  return {
    fetchConfiguration,
    updateConfiguration,
    renderers,
    configurationValidator,
    domains,
    configurations,
    schemas,
    selectedDomain
  }
}