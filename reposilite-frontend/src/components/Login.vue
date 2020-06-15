<template lang="pug">
    form#login(v-if="!auth.verified").p-8.text-center(method="post").border-dashed.border-black.border-1.rounded
        h1.font-bold.py-4.text-xl Login
        div.py-1
            input(placeholder="Alias" name="alias" v-model="auth.alias").w-96.p-1
        div.py-1
            input(placeholder="Token" name="token" v-model="auth.token" autocomple="on").w-96.p-1
        div.py-3
            button.bg-gray-300.px-6.py-1.mt-1(v-on:click="login") Login
        div.py-1
            p(v-if="error") {{ this.error }}
</template>

<script>
export default {
    
}
</script>