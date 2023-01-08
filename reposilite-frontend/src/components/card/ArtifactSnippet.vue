<!--
  - Copyright (c) 2023 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup>
import XmlTag from './XmlTag.vue'
import CodeString from './CodeString.vue'
import CodeBrackets from "./CodeBrackets.vue"

defineProps({
  configuration: {
    type: Object,
    required: true
  },
  data: {
    type: Object,
    required: true
  }
})
</script>

<template>
<pre v-if="configuration.lang === 'xml'">
<XmlTag name="dependency">
  <XmlTag name="groupId">{{ data.groupId }}</XmlTag>
  <XmlTag name="artifactId">{{ data.artifactId }}</XmlTag>
  <XmlTag name="version">{{ data.version }}</XmlTag>
</XmlTag>
</pre>
<pre v-else-if="configuration.lang === 'groovy'">
implementation <CodeString>{{ `${data.groupId}:${data.artifactId}:${data.version}` }}</CodeString>
</pre>
<pre v-else-if="configuration.lang === 'kotlin'">
implementation<CodeBrackets start="(" end=")"><CodeString>{{ `${data.groupId}:${data.artifactId}:${data.version}` }}</CodeString></CodeBrackets>
</pre>
<pre v-else-if="configuration.lang === 'scala'">
<CodeString>{{data.groupId}}</CodeString> %% <CodeString>{{data.artifactId}}</CodeString> %% <CodeString>{{data.version}}</CodeString>
</pre>
</template>