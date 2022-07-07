import {computed, markRaw, ref, toRaw} from 'vue'
import { useSession } from './session'
import {createToast} from 'mosha-vue-toastify'
import { createAjv } from '@jsonforms/core'
import { vanillaRenderers } from '@dzikoysk/vue-vanilla'
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
    
const fetchConfiguration = () => {
  return client.value.settings.domains()
    .then(domainsResponse => domains.value = domainsResponse.data)
    .then(() => Promise.all(domains.value.map(domain =>
      client.value.settings.schema(domain)
        .then(schemaResponse => schemas.value[domain] = schemaResponse.data)
        .then(() => client.value.settings.fetch(domain))
        .then(configurationResponse => configurations.value[domain] = configurationResponse.data)))
    )
    .then(() => selectedDomain.value = domains.value[0])
    .then(() => createToast('Configuration loaded', { type: 'success' }))
    .catch(error => createToast(`${error || ''}`, { type: 'danger' }))
}

const updateConfiguration = () => {
  const updates = domains.value.map(domain => {
    client.value.settings.update(domain, toRaw(configurations.value[domain]))
      .then(() => client.value.settings.fetch(domain))
      .then(response => { configurations[domain] = response.data })
  })

  return Promise.all(updates)
    .then(() => createToast('Configuration updated', { type: 'success' }))
    .catch(error => createToast(`Failed to update ${error.join(', ')}`, { type: 'danger' }))
}

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

const configurationValidator = computed(() => createAjv({
  useDefaults: true,
  removeAdditional: false,
  formats: {
    'repositories.storageProvider.quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'repositories.id': {
      type: 'string',
      validate: (name) => name in configurations.value['maven'].repositories
    },
    'repositories.proxied.allowedGroups': /^(\w+\.)*\w+$/,
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