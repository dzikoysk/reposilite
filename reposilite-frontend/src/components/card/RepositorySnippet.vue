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
<XmlTag name="repository">
  <XmlTag name="id">{{ data.repoId }}</XmlTag>
  <XmlTag name="name">{{ data.title }}</XmlTag>
  <XmlTag name="url">{{ data.domain }}</XmlTag>
</XmlTag>
</pre>
<pre v-else-if="configuration.lang === 'groovy'">
maven <CodeBrackets start="{" end="}">
    url <CodeString>{{ data.domain }}</CodeString>
</CodeBrackets>
</pre>
<pre v-else-if="configuration.lang === 'kotlin'">
maven <CodeBrackets start="{" end="}">
    url = uri<CodeBrackets start="(" end=")"><CodeString>{{ data.domain }}</CodeString></CodeBrackets>
</CodeBrackets>
</pre>
<pre v-else-if="configuration.lang === 'scala'">
resolvers +=
  <CodeString>{{data.repoId}}</CodeString>
     at <CodeString>{{data.domain}}</CodeString>
</pre>
</template>