<template>
  <div>
    <a-row>
      <a-col :span="24">
        <a-card :loading="!model.ready" :bordered="false" title="物流利润" :style="{ marginTop: '24px' }">
          <a-select :default-value="view.select.month" style="width: 120px" @change="onMonthChange">
            <a-select-option :value="i" v-for="i in view.select.months" :key="i">
              {{ i }}
            </a-select-option>
          </a-select>
          <a-select :default-value="null" style="width: 120px" @change="onCountryChange">
            <a-select-option value="e" v-for="e in view.select.countries" :key="e">
              {{ e }}
            </a-select-option>
          </a-select>
          <a-select :default-value="null" style="width: 120px" @change="onChannelChange">
            <a-select-option value="e" v-for="e in view.select.channels" :key="e">
              {{ e }}
            </a-select-option>
          </a-select>
          <a-divider/>
          <a-row>
            <a-col :span="3">
              <a-statistic title="本月开票" :value="view.text.invoiced.orderNumber" suffix="单"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="应收 (EUR)" :value="view.text.invoiced.amountDue.EUR" suffix="€"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="应收 (CNY)" :value="view.text.invoiced.amountDue.CNY" suffix="¥"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="实际开销 (EUR)" :value="view.text.invoiced.realCost.EUR" suffix="€"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="实际开销 (CNY)" :value="view.text.invoiced.realCost.CNY" suffix="¥"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="盈利" :value="view.text.invoiced.profit.value" suffix="¥"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="利润率" :value="view.text.invoiced.profit.rate" suffix="%"/>
            </a-col>
          </a-row>
          <a-divider/>
          <a-row>
            <a-col :span="3">
              <a-statistic title="未开票" :value="view.text.uninvoiced.orderNumber" suffix="单"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="实际开销 (EUR)" :value="view.text.uninvoiced.readCost.EUR" suffix="€"/>
            </a-col>
            <a-col :span="3">
              <a-statistic title="实际开销 (CNY)" :value="view.text.uninvoiced.readCost.CNY" suffix="¥"/>
            </a-col>
          </a-row>
          <a-divider/>
        </a-card>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="12">
        <a-card :loading="!model.ready" :bordered="false" :title="view.chart.invoiced.title"
                :style="{ marginTop: '24px' }">
          <BarMultid
            :title="view.chart.invoiced.title"
            :data-source="view.chart.invoiced.dataSource"
            :fields="view.chart.invoiced.fields"
            :height="400"
          >
          </BarMultid>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card :loading="!model.ready" :bordered="false" :title="view.chart.uninvoiced.title"
                :style="{ marginTop: '24px' }">
          <BarMultid
            :title="view.chart.uninvoiced.title"
            :data-source="view.chart.uninvoiced.dataSource"
            :fields="view.chart.uninvoiced.fields"
            :height="400"
          >
          </BarMultid>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import BarMultid from "@comp/chart/BarMultid";
import {getAction} from "@api/manage";
import {sum} from "xe-utils/methods";
import moment from "moment";

const url = {
  monthlyProfit: "/business/logisticExpenseDetail/monthlyLogisticProfit",
  allCountry: "/business/logisticExpenseDetail/allCountry",
  allChannel: "/business/logisticExpenseDetail/allChannel",
}

export default {
  name: "TextCard",
  components: {
    BarMultid
  },
  data() {
    return {
      view: {
        select: {
          months: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
          channels: [],
          countries: [],
          month: undefined
        },
        text: {
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
        chart: {
          invoiced: {
            title: "每日开销与应收物流费用",
            fields: [],
            dataSource: []
          },
          uninvoiced: {
            title: "未开票订单实际开销",
            fields: [],
            dataSource: []
          }
        },
        ready: false
      },
      model: {
        monthlyLogisticProfit: {
          invoicedOrderNumber: undefined,
          uninvoicedOrderNumber: undefined,
          monthOfYear: undefined,
          invoicedAmountDue: [],
          invoicedActualCosts: [],
          nonInvoicedActualCosts: [],
          exchangeRate: undefined,
        },
        select: {
          countries: [],
          channels: [],
        },
        ready: false
      },
      form: {
        month: moment().month() + 1,
        country: null,
        channel: null
      }
    }
  },
  created() {
    this.onMVChange()
  },
  computed: {
    loading() {
      return this.model.ready && this.view.ready
    }
  }
  ,
  methods: {
    loadModel() {
      return getAction(url.monthlyProfit, this.form)
        .then(
          res => {
            this.model.monthlyLogisticProfit = res['result']
          }
        )
        .then(() => {
            getAction(url.allCountry)
              .then(
                res => {
                  this.model.select.countries = res['result']
                }
              )
          }
        )
        .then(() => {
          getAction(url.allChannel)
            .then(
              res => {
                this.model.select.channels = res['result']
                this.model.ready = true
              }
            )
        })
    },

    prepareView() {
      let src = this.model.monthlyLogisticProfit
      let target = this.view.text.invoiced

      // invoiced part
      target.orderNumber = src.invoicedOrderNumber

      target.amountDue.EUR = sum(src.invoicedAmountDue)
      target.amountDue.CNY = this.toCNY(target.amountDue.EUR)

      target.realCost.EUR = sum(src.invoicedActualCosts)
      target.realCost.CNY = this.toCNY(target.realCost.EUR)

      target.profit.value = target.amountDue.CNY - target.realCost.CNY
      target.profit.rate = (target.profit.value / target.realCost.CNY * 100).toFixed(2)

      // uninvoiced part
      target = this.view.text.uninvoiced
      target.orderNumber = src.uninvoicedOrderNumber
      target.readCost.EUR = sum(src.nonInvoicedActualCosts)
      target.readCost.CNY = this.toCNY(target.readCost.EUR)

      let today = moment().date()

      // invoiced chart
      target = this.view.chart.invoiced
      target.fields = Array.from({length: today}, (v, i) => i + 1 + '')
      target.dataSource = [{type: '应收物流费用(CNY)'}, {type: '实际开销 (CNY)'}]
      for (let j = 0; j < today; j++) {
        target.dataSource[0][j + 1 + ''] = this.toCNY(src.invoicedAmountDue[j])
        target.dataSource[1][j + 1 + ''] = this.toCNY(src.invoicedActualCosts[j])
      }

      // uninvoiced chart
      target = this.view.chart.uninvoiced
      target.fields = Array.from({length: today}, (v, i) => i + 1 + '')
      target.dataSource = [{type: '实际开销 (CNY)'}]
      for (let j = 0; j < today; j++) {
        target.dataSource[0][j + 1 + ''] = this.toCNY(src.nonInvoicedActualCosts[j])
      }

      this.view.select.month = this.form.month

      this.view.ready = true
    },

    toCNY(EUR) {
      return (EUR * this.model.monthlyLogisticProfit.exchangeRate).toFixed(2)
    },

    onMonthChange(value) {
      this.form.month = value
      this.onMVChange()
    },

    onCountryChange(value) {
      this.form.country = value
      this.onMVChange()

    },
    onChannelChange(value) {
      this.form.channel = value
      this.onMVChange()

    },

    reset() {
      this.form = {
        month: moment().month(),
        country: null,
        channel: null
      }
      this.onMVChange()
    },

    onMVChange() {
      this.view.ready = false
      this.model.ready = false
      this.loadModel().then(this.prepareView)
    }
  }
}
</script>

<style scoped>

</style>