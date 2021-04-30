<template>
  <div>
    <Header/>
    <div>
      <div class="container mx-auto">
        <tabs v-model="selectedMenuTab" >
          <tab
            v-for="(tab, i) in menuTabs"
            class="item font-normal"
            :key="`menu${i}`"
            :val="tab"
            :label="tab"
            :indicator="true"
          />
        </tabs>
      </div>
      <hr class="dark:border-gray-700">
      <div>
        <tab-panels v-model="selectedMenuTab" :animate="true">
          <tab-panel :val="'Overview'">
            <Overview/>
          </tab-panel>
          <tab-panel :val="'Usage'">
            <Usage/>
          </tab-panel>
          <tab-panel :val="'Endpoints'">
            <Endpoints/>
          </tab-panel>
        </tab-panels>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, toRefs } from 'vue'
import Header from '../components/Header.vue'
import Overview from '../components/index/Overview.vue'
import Usage from '../components/index/Usage.vue'
import Endpoints from '../components/index/Endpoints.vue'

const menuTabs = [ 'Overview', 'Usage', 'Endpoints' ]

export default {
  name: 'Index',
  components: {
    Header,
    Overview,
    Usage,
    Endpoints
  },
  setup() {
    const state = reactive({
      selectedMenuTab: menuTabs[0]
    })

    return {
      menuTabs,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.item {
  @apply px-1;
  @apply pb-1;
  @apply cursor-pointer;
  @apply text-gray-600 dark:text-gray-300;
}

.selected {
  @apply border-b-2;
  @apply border-black dark:border-white;
  @apply text-black dark:text-white;
}
</style>