<template>
  <Hero />
  <Browser ref=""/>
</template>

<script>
import Hero from './Hero.vue'
import Browser from '../Browser.vue'

export default {
  components: {
    Hero,
    Browser
  }
}
</script>