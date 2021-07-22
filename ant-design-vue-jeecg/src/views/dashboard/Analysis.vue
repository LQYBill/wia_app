<template>
  <div>
    <index-chart></index-chart>
  </div>
</template>

<script>
  import IndexChart from './IndexChart'

  export default {
    name: "Analysis",
    components: {
      IndexChart,
    },
    data() {
      return {
        indexStyle:1
      }
    },
    created() {

    },
    methods: {

    }
  }
</script>