<template>
  <div>
    <BaseCheckbox v-model="checked" label="good"></BaseCheckbox>
    <BaseCheckbox v-model="toggle" label="bad"></BaseCheckbox>
  </div>
</template>

<script lang="ts">
    import Vue from 'vue';
    import BaseCheckbox from "~/components/BaseCheckbox.vue";

    const exampleValue = {
        "property": "P31",
        "value": "Q5",
        "rank": "best-rank",
    };

    export default Vue.extend({
        data() {
            return { exampleValue, checked: false, toggle: false };
        },
        meta: {
            "title": "New Dump"
        },
        components: {
            BaseCheckbox,
        }
    })
</script>

<style>
</style>
