import {computed, markRaw, ref} from 'vue'
import { useSession } from './session'
import {createToast} from 'mosha-vue-toastify'
import { createAjv } from '@jsonforms/core'

import { vanillaRenderers } from '@jsonforms/vue-vanilla'

import { default as ObjectRenderer, tester as objectTester } from '../components/renderers/ObjectRenderer.vue'
import { default as AllOfRenderer, tester as allOfTester } from '../components/renderers/AllOfRenderer.vue'
import { default as OneOfRenderer, tester as oneOfTester } from '../components/renderers/OneOfRenderer.vue'
import { default as ConstantRenderer, tester as constantTester } from '../components/renderers/ConstantRenderer.vue'
import { default as OptionalRenderer, tester as optionalTester } from '../components/renderers/OptionalRenderer.vue'

const { client } = useSession()
const schema = ref({})
const configuration = ref({})
const configurations = ref([])
const selectedConfiguration = ref('')

const getSchema = async (name) =>
  (await client.value.schema.get(name)).data

const listConfigs = async () =>
  (await client.value.config.list()).data

const getConfig = async (name) =>
  (await client.value.config.get(name)).data

const updateConfig = async (name, value) =>
  (await client.value.config.put(name, value)).data

const fetchConfiguration = async () => {
  try {
    const fetchedConfiguration = {}
    const fetchedSchemas = {}
    configurations.value = await listConfigs()
    for (const conf of configurations.value) {
      fetchedConfiguration[conf] = await getConfig(conf)
      fetchedSchemas[conf] = await getSchema(conf)
    }
    configuration.value = fetchedConfiguration
    schema.value = fetchedSchemas
    selectedConfiguration.value = configurations.value[0]
    createToast('Configuration loaded', { type: 'success' })
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

const updateConfiguration = async () => {
  try {
    const confs = {}
    const errored = []
    for (const conf of configurations.value) {
      const newValue = await updateConfig(conf, configuration.value[conf])
      if (newValue) {
        confs[conf] = newValue
      } else {
        errored.push(conf)
      }
    }
    configuration.value = confs
    if (errored.length > 0) {
      createToast(`Failed to update ${errored.join(', ')}`, { type: 'danger' })
    } else {
      createToast('Configuration updated', { type: 'success' })
    }
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

const renderers = markRaw([
  ...vanillaRenderers,
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
])

const configurationValidator = computed(() => createAjv({
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in configuration.value['maven'].repositories
    }
  }
}))

export function useConfiguration() {
  return {
    fetchConfiguration,
    updateConfiguration,
    renderers,
    configurationValidator,
    configurations,
    schema,
    selectedConfiguration
  }
}