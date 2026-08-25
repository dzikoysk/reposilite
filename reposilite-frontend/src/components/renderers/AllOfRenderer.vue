<!--
  ~ Copyright (c) 2020-2026 dzikoysk
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<template>
  <div v-if="control.visible">
    <template v-if="delegateUISchema">
      <DispatchRenderer
          :schema="control.schema"
          :uischema="delegateUISchema"
          :path="control.path"
          :enabled="control.enabled"
          :renderers="control.renderers"
          :cells="control.cells"
      />
    </template>
    <template v-else-if="allOfRenderInfos">
      <DispatchRenderer
          v-for="(allOfRenderInfo, allOfIndex) in allOfRenderInfos"
          :key="`${control.path}-${allOfIndex}`"
          :schema="allOfRenderInfo.schema"
          :uischema="allOfRenderInfo.uischema"
          :path="control.path"
          :enabled="control.enabled"
          :renderers="control.renderers"
          :cells="control.cells"
      />
    </template>
  </div>
</template>

<script>
import {createCombinatorRenderInfos, findMatchingUISchema, isAllOfControl, rankWith} from '@jsonforms/core'
import {DispatchRenderer, rendererProps, useJsonFormsAllOfControl} from '@jsonforms/vue'
import {useVanillaControl} from '@jsonforms/vue-vanilla'

export const tester = rankWith(3, isAllOfControl)

export default {
  name: 'AllOfRenderer',
  components: {
    DispatchRenderer
  },
  props: rendererProps(),
  setup: props => useVanillaControl(useJsonFormsAllOfControl(props)),
  computed: {
    delegateUISchema() {
      return findMatchingUISchema(this.control.uischemas)(
          this.control.schema,
          this.control.uischema.scope,
          this.control.path
      )
    },
    allOfRenderInfos() {
      return createCombinatorRenderInfos(
          this.control.schema.allOf,
          this.control.rootSchema,
          'allOf',
          this.control.uischema,
          this.control.path,
          this.control.uischemas
      )
    }
  }
}
</script>