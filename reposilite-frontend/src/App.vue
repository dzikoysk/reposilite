<template lang="pug">
  router-view
</template>

<script>
export default {
  name: 'App',
  mounted () {
    console.log(window.REPOSILITE_MESSAGE)
  }
}
</script>
