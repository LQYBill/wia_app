<template>
  <div>
    <a-row>
      <a-col :span='24'>
        <a-card :loading='!model.ready' :bordered='false' title='物流利润' :style="{ marginTop: '24px' }">
          <a-space>
            <a-range-picker :format='dateFormat' @change='onDateChange' :defaultValue='defaultRange()'
                            :value='view.select.range' />
            <a-select placeholder='选择国家' :default-value='view.select.country' style='width: 240px'
                      @change='onCountryChange' mode='multiple'>
              <a-select-option :value='e' v-for='e in view.select.countries' :key='e'>
                {{ e }}
              </a-select-option>
            </a-select>
            <a-select placeholder='选择渠道' :default-value='view.select.channel' style='width: 300px'
                      @change='onChannelChange' mode='multiple'>
              <a-select-option :value='e' v-for='e in view.select.channels' :key='e'>
                {{ e }}
              </a-select-option>
            </a-select>
            <a-button type='primary' @click='apply'>Apply</a-button>
            <a-button type='danger' @click='reset'>Reset</a-button>
          </a-space>
          <a-divider />
          <a-row>
            <a-col :span='3'>
              <a-statistic title='本月开票' :value='view.text.invoiced.orderNumber' suffix='单' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='应收 (EUR)' :value='view.text.invoiced.amountDue.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='应收 (CNY)' :value='view.text.invoiced.amountDue.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销 (EUR)' :value='view.text.invoiced.realCost.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销 (CNY)' :value='view.text.invoiced.realCost.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='盈利' :value='view.text.invoiced.profit.value' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='利润率' :value='view.text.invoiced.profit.rate' suffix='%' />
            </a-col>
          </a-row>
          <a-divider />
          <a-row>
            <a-col :span='3'>
              <a-statistic title='未开票' :value='view.text.uninvoiced.orderNumber' suffix='单' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销 (EUR)' :value='view.text.uninvoiced.readCost.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销 (CNY)' :value='view.text.uninvoiced.readCost.CNY' suffix='¥' />
            </a-col>
          </a-row>
          <a-divider />
        </a-card>
      </a-col>
    </a-row>
    <a-row :gutter='24'>
      <a-col :span='12'>
        <a-card :loading='!model.ready' :bordered='false' :title='view.chart.invoiced.title'
                :style="{ marginTop: '24px' }">
          <BarMultid
            :title='view.chart.invoiced.title'
            :data-source='view.chart.invoiced.dataSource'
            :fields='view.chart.invoiced.fields'
            :height='400'
          >
          </BarMultid>
        </a-card>
      </a-col>
      <a-col :span='12'>
        <a-card :loading='!model.ready' :bordered='false' :title='view.chart.uninvoiced.title'
                :style="{ marginTop: '24px' }">
          <BarMultid
            :title='view.chart.uninvoiced.title'
            :data-source='view.chart.uninvoiced.dataSource'
            :fields='view.chart.uninvoiced.fields'
            :height='400'
          >
          </BarMultid>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import BarMultid from '@comp/chart/BarMultid'
import { getAction } from '@api/manage'
import { sum } from 'xe-utils/methods'
import moment from 'moment'

const url = {
  monthlyProfit: '/business/logisticExpenseDetail/monthlyLogisticProfit',
  allCountry: '/business/logisticExpenseDetail/allCountry',
  allChannel: '/business/logisticExpenseDetail/allChannel'
}

export default {
  name: 'TextCard',
  components: {
    BarMultid
  },
  data() {
    return {
      dateFormat: 'YYYY-MM-DD',
      view: {
        select: {
          range: null,
          channels: [],
          countries: [],
          channel: undefined,
          country: undefined
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
            title: '每日开销与应收物流费用',
            fields: [],
            dataSource: []
          },
          uninvoiced: {
            title: '未开票订单实际开销',
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
          exchangeRate: undefined
        },
        select: {
          countries: [],
          channels: []
        },
        ready: false
      },
      form: {
        startDate: moment().startOf('month').format('YYYY-MM-DD'),
        endDate: moment().endOf('day').format('YYYY-MM-DD'),
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
      let res1 = getAction(url.monthlyProfit, this.form)
        .then(
          res => {
            this.model.monthlyLogisticProfit = res['result']
          }
        )

      let res2 = getAction(url.allCountry)
        .then(
          res => {
            this.model.select.countries = res['result']
          }
        )

      let res3 = getAction(url.allChannel)
        .then(
          res => {
            this.model.select.channels = res['result']
          }
        )

      return Promise.all([res1, res2, res3]).then(() => {
        this.model.ready = true
      })
    },
    defaultRange() {
      return [this.form.startDate, this.form.endDate]
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

      target.profit.value = (target.amountDue.CNY - target.realCost.CNY).toFixed(2)
      target.profit.rate = (target.profit.value / target.realCost.CNY * 100).toFixed(2)

      // uninvoiced part
      target = this.view.text.uninvoiced
      target.orderNumber = src.uninvoicedOrderNumber
      target.readCost.EUR = sum(src.nonInvoicedActualCosts)
      target.readCost.CNY = this.toCNY(target.readCost.EUR)

      let numberOfDays = moment(this.form.endDate).diff(moment(this.form.startDate), 'days') + 1

      // invoiced chart
      target = this.view.chart.invoiced
      target.fields = Array.from({ length: numberOfDays }, (v, i) => i + 1 + '')
      target.dataSource = [{ type: '应收物流费用(CNY)' }, { type: '实际开销 (CNY)' }]
      for (let j = 0; j < numberOfDays; j++) {
        let date = moment(this.form.startDate).add(j, 'd').format(this.dateFormat)
        target.dataSource[0][j + 1 + ''] = this.toCNY(src.invoicedAmountDue[date] || 0)
        target.dataSource[1][j + 1 + ''] = this.toCNY(src.invoicedActualCosts[date] || 0)
      }

      // uninvoiced chart
      target = this.view.chart.uninvoiced
      target.fields = Array.from({ length: numberOfDays }, (v, i) => i + 1 + '')
      target.dataSource = [{ type: '实际开销 (CNY)' }]
      for (let j = 0; j < numberOfDays; j++) {
        let date = moment(this.form.startDate).add(j, 'd').format(this.dateFormat)
        target.dataSource[0][j + 1 + ''] = this.toCNY(src.nonInvoicedActualCosts[date] || 0)
      }

      // select
      this.view.select.range = [this.form.startDate, this.form.endDate]
      this.view.select.countries = this.model.select.countries
      this.view.select.channels = this.model.select.channels

      this.view.ready = true
    },

    toCNY(EUR) {
      return Number((EUR * this.model.monthlyLogisticProfit.exchangeRate).toFixed(2))
    },
    onDateChange(dates, dateStrings) {
      this.form.startDate = dateStrings[0]
      this.form.endDate = dateStrings[1]
      this.view.select.range = dates
    },
    onCountryChange(value) {
      this.form.country = value
      this.view.select.country = value
    },
    onChannelChange(value) {
      this.form.channel = value
      this.view.select.channel = value
    },
    apply() {
      this.onMVChange()
    },
    reset() {
      this.form = {
        startDate: moment().startOf('month').format('YYYY-MM-DD'),
        endDate: moment().endOf('day').format('YYYY-MM-DD'),
        country: null,
        channel: null
      }
      this.view.select.country = []
      this.view.select.channel = []
      this.onMVChange()
    },

    onMVChange() {
      this.view.ready = false
      this.model.ready = false
      this.$emit('range', [this.form.startDate, this.form.endDate])
      this.$emit('country', this.form.country)
      this.$emit('channel', this.form.channel)
      this.loadModel().then(this.prepareView)
    }
  }
}
</script>

<style scoped>

</style>