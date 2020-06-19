<template lang="pug">
    svg.w-full.h-264.lg_h-168(viewBox="0 0 1080 640" preserveAspectRatio="none")
        path(
            :fill="this.accentColor"
            fill-opacity="1"
            d=" \
                M 0,260 \
                C 640,330 650,0 1080,170 \
                L 1440 0 \
                L 0,0 \
                Z \
        ")
        path(
            fill="#212121"
            fill-opacity="1"
            d=" \
                M 0,230 \
                C 620,310 650,50 1080,150 \
                L 1440 0 \
                L 0,0 \
                Z \
        ")
</template>

<script>
export default {
  props: ['accentColor']
}
</script>
