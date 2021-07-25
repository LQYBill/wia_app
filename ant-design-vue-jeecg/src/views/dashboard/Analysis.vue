<template>
  <div>
    <index-chart></index-chart>
<!--    <div style="width: 100%;text-align: right;margin-top: 20px">-->
<!--      请选择首页样式：-->
<!--      <a-radio-group v-model="indexStyle">-->
<!--        <a-radio :value="1">统计图表To be translated</a-radio>-->
<!--        <a-radio :value="2">统计图表2To be translated</a-radio>-->
<!--        <a-radio :value="3">任务表格To be translated</a-radio>-->
<!--      </a-radio-group>-->
<!--    </div>-->
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