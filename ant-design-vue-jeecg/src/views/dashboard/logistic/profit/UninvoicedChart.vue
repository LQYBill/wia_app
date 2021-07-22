<template>
  <a-card :loading="!model.ready" :bordered="false" :title="view.title" :style="{ marginTop: '24px' }">
    汇率: {{ model.monthlyProfit.exchangeRate }}
    <BarMultid
      :title="view.chart.chart2.title"
      :data-source="view.chart.chart2.dataSource"
      :fields="view.chart.chart2.fields"
      :height="400"
    >
    </BarMultid>
  </a-card>

</template>

<script>
import BarMultid from "@comp/chart/BarMultid";
import {getAction} from "@api/manage";
import moment from "moment";

const url = {
  monthlyProfit: "/business/logisticExpenseDetail/monthlyLogisticProfit",
}

export default {
  name: "UninvoicedChart.vue",
  components: {
    BarMultid
  },
  data() {
    return {
      view: {
        title:"未开票",
        chart: {
          chart2: {
            title: "未开票订单实际开销",
            fields: [],
            dataSource: []
          }

        }
      },
      model: {
        monthlyProfit: {
          month: undefined,
          dailyData: [],
          exchangeRate: undefined
        },
        ready: false
      }
    }
  },
  created() {
    this.loadModel().then(() => this.modelToView()).then(
      () => {
        this.model.ready = true
      }
    )

  }
  ,
  methods: {
    loadModel() {
      const param = {}
      return getAction(url.monthlyProfit, param)
        .then(
          res => {
            this.model.monthlyProfit.month = res['result']['monthOfYear']
            this.model.monthlyProfit.dailyData = res['result']['dailyData']
            this.model.monthlyProfit.exchangeRate = res['result']['exchangeRate']
          }
        )
    },
    modelToView() {
      const exchange_rate = this.model.monthlyProfit.exchangeRate
      let monthlyProfit = this.model.monthlyProfit

      let chart2 = this.view.chart.chart2
      chart2.fields = Array.from({length: moment().date()}, (v, i) => i + 1 + '')
      let dataSource2 = [{type: '实际开销 (CNY)'}]

      let i = 1
      for (const e of monthlyProfit.dailyData) {
        dataSource2[0][i + ''] = (e['uninvoicedActualCost'] * exchange_rate).toFixed(2)
        i++
      }
      this.view.chart.chart2.dataSource = dataSource2
    }
  }
}
</script>

<style scoped>

</style>