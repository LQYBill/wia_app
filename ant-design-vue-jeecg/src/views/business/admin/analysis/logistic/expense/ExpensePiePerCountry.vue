<template>
  <a-card :loading="!view.ready" :bordered="false" :title="view.title" :style="{ marginTop: '24px' }">
    <pie title="饼图" :height="view.height" :data-source="view.chart.dataSource"/>
  </a-card>
</template>

<script>
import {getAction} from "@api/manage";
import Pie from '@comp/chart/Pie'


export default {
  name: "ExpensePie",
  components: {
    Pie
  },
  data: function () {
    return {
      model: {
        url: {
          logisticExpenseProportion: "/business/logisticExpenseDetail/expenseProportionByCountry"
        },
        data: []
      },
      view: {
        ready: false,
        title: "物流开销占比（国家）",
        height:300,
        chart: {
          dataSource: []
        },

      }
    }
  },

  created() {
    this.loadModel().then(this.prepareView)
  },

  methods: {
    loadModel() {
      const param = {}
      return getAction(this.model.url.logisticExpenseProportion, param)
        .then(
          res => {
            this.model.data = res['result']
          }
        )
    },

    prepareView() {
      this.view.ready = true
      this.view.chart.dataSource = this.model.data.map(
        element => ({
          item: element["name"],
          count: element["expense"]
        })
      )
    }
  }

}
</script>

<style scoped>

</style>