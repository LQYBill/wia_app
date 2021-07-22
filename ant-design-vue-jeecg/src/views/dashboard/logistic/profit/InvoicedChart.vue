<template>
  <a-card :loading="!model.ready" :bordered="false" :title="view.title" :style="{ marginTop: '24px' }">
    汇率: {{ model.monthlyProfit.exchangeRate }}
    <BarMultid
      :title="view.chart.chart.title"
      :data-source="view.chart.chart.dataSource"
      :fields="view.chart.chart.fields"
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
  name: "InvoicedChart",
  components: {
    BarMultid
  },
  data() {
    return {
      view: {
        title:"已开票",
        chart: {
          chart: {
            title: "每日开销与应收物流费用",
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

      let chart1 = this.view.chart.chart
      chart1.fields = Array.from({length: moment().date()}, (v, i) => i + 1 + '')
      let dataSource1 = [{type: '实际开销 (CNY)'}, {type: '应收物流费用(CNY)'}]

      let i = 1
      for (const e of monthlyProfit.dailyData) {

        // data source
        dataSource1[0][i + ''] = (e['actualCost'] * exchange_rate).toFixed(2)
        dataSource1[1][i + ''] = (e['amountReceivable'] * exchange_rate).toFixed(2)
        i++
      }

      this.view.chart.chart.dataSource = dataSource1
    }
  }
}
</script>

<style scoped>

</style>