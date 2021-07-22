<template>
  <a-card :loading="!model.ready" :bordered="false" title="物流利润" :style="{ marginTop: '24px' }">
    汇率: {{ model.monthlyProfit.exchangeRate }}
    <a-row>
      <a-col :span="3">
        <a-statistic title="本月开票" :value="view.invoiced.orderNumber" suffix="单"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="应收 (EUR)" :value="view.invoiced.amountDue.EUR" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="应收 (CNY)" :value="view.invoiced.amountDue.CNY" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (EUR)" :value="view.invoiced.realCost.EUR" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (CNY)" :value="view.invoiced.realCost.CNY" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="盈利" :value="view.invoiced.profit.value" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="利润率" :value="view.invoiced.profit.rate" suffix="%"/>
      </a-col>
    </a-row>
    <a-divider/>
    <a-row>
      <a-col :span="3">
        <a-statistic title="未开票" :value="view.uninvoiced.orderNumber" suffix="单"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (EUR)" :value="view.uninvoiced.readCost.EUR" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (CNY)" :value="view.uninvoiced.readCost.CNY" suffix="¥"/>
      </a-col>
    </a-row>
    <a-divider/>
  </a-card>

</template>

<script>
import BarMultid from "@comp/chart/BarMultid";
import {getAction} from "@api/manage";

const url = {
  monthlyProfit: "/business/logisticExpenseDetail/monthlyLogisticProfit",
}

export default {
  name: "TextCard",
  components: {
    BarMultid
  },
  data() {
    return {
      view: {
        invoiced: {
          orderNumber: undefined,
          amountDue: {
            EUR: undefined,
            CNY: undefined
          },
          realCost: {
            EUR: undefined,
            CNY: undefined
          },
          profit: {
            value: undefined,
            rate: undefined
          }

        },
        uninvoiced: {
          orderNumber: undefined,
          readCost: {
            EUR: undefined,
            CNY: undefined
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
      let monthlyProfit = this.model.monthlyProfit

      let invoiced = {
        orderNumber: 0,
        amountDue: 0,
        realCost: 0,
      }

      let uninvoiced = {
        orderNumber: 0,
        realCost: 0,
      }

      for (const e of monthlyProfit.dailyData) {
        // invoiced
        invoiced.orderNumber += e['invoicedOrderNumber']
        invoiced.amountDue += e['amountReceivable']
        invoiced.realCost += e['actualCost']

        // uninvoiced
        uninvoiced.orderNumber += e['uninvoicedOrderNumber']
        uninvoiced.realCost += e['uninvoicedActualCost']
      }

      const viewInvoiced = this.view.invoiced
      viewInvoiced.orderNumber = invoiced.orderNumber
      viewInvoiced.amountDue.EUR = invoiced.amountDue
      viewInvoiced.amountDue.CNY = this.toCNY(invoiced.amountDue)
      viewInvoiced.realCost.EUR = invoiced.realCost
      viewInvoiced.realCost.CNY = this.toCNY(invoiced.realCost)
      viewInvoiced.profit.value = this.toCNY(invoiced.amountDue - invoiced.realCost)
      viewInvoiced.profit.rate = (viewInvoiced.profit.value / viewInvoiced.realCost.CNY * 100).toFixed(2)

      const viewUninvoiced = this.view.uninvoiced
      viewUninvoiced.orderNumber = uninvoiced.orderNumber
      viewUninvoiced.readCost.EUR = uninvoiced.realCost
      viewUninvoiced.readCost.CNY = this.toCNY(uninvoiced.realCost)
    },

    toCNY(EUR) {
      return (EUR * this.model.monthlyProfit.exchangeRate).toFixed(2)
    }
  }
}
</script>

<style scoped>

</style>