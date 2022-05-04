import {computed, markRaw, ref} from 'vue'
import { useSession } from './session'
import {createToast} from 'mosha-vue-toastify'
import { createAjv } from '@jsonforms/core'

import { vanillaRenderers } from '@dzikoysk/vue-vanilla'

import { default as LabelRenderer, tester as labelTester } from '../components/renderers/LabelRenderer.vue'
import { default as ObjectRenderer, tester as objectTester } from '../components/renderers/ObjectRenderer.vue'
import { default as AllOfRenderer, tester as allOfTester } from '../components/renderers/AllOfRenderer.vue'
import { default as OneOfRenderer, tester as oneOfTester } from '../components/renderers/OneOfRenderer.vue'
import { default as ConstantRenderer, tester as constantTester } from '../components/renderers/ConstantRenderer.vue'
import { default as OptionalRenderer, tester as optionalTester } from '../components/renderers/OptionalRenderer.vue'

const { client } = useSession()
const domains = ref([])
const schemas = ref({})
const selectedDomain = ref('')
const selectedConfiguration = ref({})

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
    domains.value = await listConfigs()
    for (const conf of domains.value) {
      fetchedConfiguration[conf] = await getConfig(conf)
      fetchedSchemas[conf] = await getSchema(conf)
    }
    selectedConfiguration.value = fetchedConfiguration
    schemas.value = fetchedSchemas
    selectedDomain.value = domains.value[0]
    createToast('Configuration loaded', { type: 'success' })
  } catch (error) {
    createToast(`${error || ''}`, { type: 'danger' })
  }
}

const updateConfiguration = async () => {
  try {
    const fetchedConfiguration = {}
    const errored = []
    for (const conf of domains.value) {
      const newValue = await updateConfig(conf, selectedConfiguration.value[conf])
      if (newValue) {
        fetchedConfiguration[conf] = newValue
      } else {
        errored.push(conf)
      }
    }
    selectedConfiguration.value = fetchedConfiguration
    if (errored.length > 0) {
      createToast(`Failed to update ${errored.join(', ')}`, { type: 'danger' })
    } else {
      createToast('Configuration updated', { type: 'success' })
    }
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

console.log(vanillaRenderers.filter(element => element.renderer.name != "label-renderer"))

const renderers = markRaw([
  { tester: allOfTester, renderer: AllOfRenderer },
  { tester: oneOfTester, renderer: OneOfRenderer },
  { tester: constantTester, renderer: ConstantRenderer },
  { tester: optionalTester, renderer: OptionalRenderer },
  { tester: labelTester, renderer: LabelRenderer },
  {
    tester: (uischema, schema) => {
      let x = objectTester(uischema, schema)
      return x === -1 || schema.title === 'Proxied Maven Repository' ? -1 : x  // needed because without it hangs TODO find out why
    },
    renderer: ObjectRenderer
  },
  ...(vanillaRenderers.filter(element => element.renderer.name != "label-renderer"))
])

console.log(renderers)

const configurationValidator = computed(() => createAjv({
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in selectedConfiguration.value['maven'].repositories
    }
  }
}))

export function useConfiguration() {
  return {
    fetchConfiguration,
    updateConfiguration,
    renderers,
    configurationValidator,
    configurations: domains,
    configuration: selectedConfiguration,
    schema: schemas,
    selectedConfiguration: selectedDomain
  }
}