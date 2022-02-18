<script setup>
import {  } from 'vue'
import download from 'downloadjs'
import { createToast } from 'mosha-vue-toastify'
import { createClient } from '../../helpers/client'
import Entry from './ListEntry.vue'

const props = defineProps({
  files: {
    type: Object,
    required: true
  }
})

const downloadHandler = (path, name) => {
  const { client } = createClient(props.token.name, props.token.secret)
  client.maven.download(path.substring(1) + '/' + name)
    .then(response => download(response.data, name, response.headers['content-type']))
    .catch(error => createToast(`Cannot download file - ${error.response.status}: ${error.response.data.message}`, {
      type: 'danger'
    }))
}
</script>

<template>
  <div id="browser-list" class="pt-3">
    <div v-for="file in files.list" v-bind:key="file">
      <router-link v-if="file.type === 'DIRECTORY'" :to="append($route.path, file.name)">
        <Entry :file="file"/>
      </router-link>
      <a v-else @click.left.prevent="downloadHandler($route.path, file.name)" :href="$route.path + '/' + file.name" target="_blank">
        <Entry :file="file"/>
      </a>
    </div>
    <div v-if="files.isEmpty" class="pl-2">
      <p>Directory is empty</p>
    </div>
    <div v-if="files.error" class="pl-2">
      <p>Directory not found</p>
    </div>
  </div>
</template>
