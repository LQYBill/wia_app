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
            <a-select show-search placeholder='输入客户进行搜索' option-filter-prop='children' allowClear=true
                      :filter-option='clientFilterOption' @change='handleClientChange' style='width: 240px'>
              <a-select-option v-for='(item, index) in clientList' :key='index' :value='index'>
                {{ item.text }}
              </a-select-option>
            </a-select>

            <a-select mode='multiple' style='width: 300px' placeholder='不选默认所有店铺' @change='handleShopChange'
              :allowClear=true v-model='shopIDs' :disabled='shopDisable'>
              <a-select-option v-for='(item, index) in shopList' :value='item.value' :key='index'>
                {{ item.text }}
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
              <a-statistic title='应收(EUR)(含VAT)' :value='view.text.invoiced.amountDue.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(EUR)(含VAT)' :value='view.text.invoiced.realCost.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='盈利(EUR)(含VAT)' :value='view.text.invoiced.profit.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='应收(CNY)(含VAT)' :value='view.text.invoiced.amountDue.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(CNY)(含VAT)' :value='view.text.invoiced.realCost.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='盈利(CNY)(含VAT)' :value='view.text.invoiced.profit.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='利润率(含VAT)' :value='view.text.invoiced.profit.rate' suffix='%' />
            </a-col>
          </a-row>
          <a-divider />
          <a-row>
            <a-col :span='3'>
            </a-col>
            <a-col :span='3'>
              <a-statistic title='应收 (EUR)(不含VAT)' :value='view.text.invoiced.amountDueWithoutVat.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(EUR)(不含VAT)' :value='view.text.invoiced.realCostWithoutVat.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='盈利(EUR)(不含VAT)' :value='view.text.invoiced.profitWithoutVat.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='应收 (CNY)(不含VAT)' :value='view.text.invoiced.amountDueWithoutVat.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(CNY)(不含VAT)' :value='view.text.invoiced.realCostWithoutVat.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='盈利(CNY)(不含VAT)' :value='view.text.invoiced.profitWithoutVat.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='利润率(不含VAT)' :value='view.text.invoiced.profitWithoutVat.rate' suffix='%' />
            </a-col>
          </a-row>
          <a-divider />
          <a-row>
            <a-col :span='3'>
              <a-statistic title='未开票' :value='view.text.uninvoiced.orderNumber' suffix='单' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(EUR)(含VAT)' :value='view.text.uninvoiced.realCost.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(EUR)(不含VAT)' :value='view.text.uninvoiced.realCostWithoutVat.EUR' suffix='€' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(CNY)(含VAT)' :value='view.text.uninvoiced.realCost.CNY' suffix='¥' />
            </a-col>
            <a-col :span='3'>
              <a-statistic title='实际开销(CNY)(不含VAT)' :value='view.text.uninvoiced.realCostWithoutVat.CNY' suffix='¥' />
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
  getClientList: "/client/client/all",
  getShopsByCustomerId: "/shippingInvoice/shopsByClient",
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
      clientList: [],
      shopList: [],
      shopIDs: [],
      customerId: "",
      selectedClient: null,
      shopDisable: true,
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
            amountDueWithoutVat: {
              EUR: undefined,
              CNY: undefined
            },
            realCost: {
              EUR: undefined,
              CNY: undefined
            },
            realCostWithoutVat: {
              EUR: undefined,
              CNY: undefined
            },
            profit: {
              EUR: undefined,
              CNY: undefined,
              rate: undefined
            },
            profitWithoutVat: {
              EUR: undefined,
              CNY: undefined,
              rate: undefined
            }
          },
          uninvoiced: {
            orderNumber: undefined,
            realCost: {
              EUR: undefined,
              CNY: undefined
            },
            realCostWithoutVat: {
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
          invoicedAmountDueWithoutVat: [],
          invoicedActualCosts: [],
          invoicedActualCostsWithoutVat: [],
          nonInvoicedActualCosts: [],
          nonInvoicedActualCostsWithoutVat: [],
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
    this.loadClientList();
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

      target.amountDueWithoutVat.EUR = sum(src.invoicedAmountDueWithoutVat)
      target.amountDueWithoutVat.CNY = this.toCNY(target.amountDueWithoutVat.EUR)

      target.realCost.CNY = sum(src.invoicedActualCosts)
      target.realCost.EUR = this.toEUR(target.realCost.CNY)

      target.realCostWithoutVat.CNY = sum(src.invoicedActualCostsWithoutVat)
      target.realCostWithoutVat.EUR = this.toEUR(target.realCostWithoutVat.CNY)

      target.profit.EUR = (target.amountDue.EUR - target.realCost.EUR).toFixed(2)
      target.profit.CNY = (target.amountDue.CNY - target.realCost.CNY).toFixed(2)
      target.profit.rate = (target.profit.CNY / target.realCost.CNY * 100).toFixed(2)

      target.profitWithoutVat.EUR = (target.amountDueWithoutVat.EUR - target.realCost.EUR).toFixed(2)
      target.profitWithoutVat.CNY = (target.amountDueWithoutVat.CNY - target.realCost.CNY).toFixed(2)
      target.profitWithoutVat.rate = (target.profitWithoutVat.CNY / target.realCost.CNY * 100).toFixed(2)

      // uninvoiced part
      target = this.view.text.uninvoiced
      target.orderNumber = src.uninvoicedOrderNumber
      target.realCost.CNY = sum(src.nonInvoicedActualCosts)
      target.realCost.EUR = this.toEUR(target.realCost.CNY)
      target.realCostWithoutVat.CNY = sum(src.nonInvoicedActualCostsWithoutVat)
      target.realCostWithoutVat.EUR = this.toEUR(target.realCostWithoutVat.CNY)

      let numberOfDays = moment(this.form.endDate).diff(moment(this.form.startDate), 'days') + 1

      // invoiced chart
      target = this.view.chart.invoiced
      target.fields = Array.from({ length: numberOfDays }, (v, i) => i + 1 + '')
      target.dataSource = [{ type: '应收物流费用(CNY)' }, { type: '实际开销 (CNY)' }]
      for (let j = 0; j < numberOfDays; j++) {
        let date = moment(this.form.startDate).add(j, 'd').format(this.dateFormat)
        target.dataSource[0][j + 1 + ''] = this.toCNY(src.invoicedAmountDue[date] || 0)
        target.dataSource[1][j + 1 + ''] = src.invoicedActualCosts[date] || 0
      }

      // uninvoiced chart
      target = this.view.chart.uninvoiced
      target.fields = Array.from({ length: numberOfDays }, (v, i) => i + 1 + '')
      target.dataSource = [{ type: '实际开销 (CNY)' }]
      for (let j = 0; j < numberOfDays; j++) {
        let date = moment(this.form.startDate).add(j, 'd').format(this.dateFormat)
        target.dataSource[0][j + 1 + ''] = src.nonInvoicedActualCosts[date] || 0
      }

      // select
      this.view.select.range = [this.form.startDate, this.form.endDate]
      this.view.select.countries = this.model.select.countries
      this.view.select.channels = this.model.select.channels

      this.view.ready = true
    },
    toEUR(CNY) {
      return Number((CNY / this.model.monthlyLogisticProfit.exchangeRate).toFixed(2))
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
    },
    loadClientList() {
      let self = this
      getAction(url.getClientList)
        .then(res => {
          if (res.success) {
            console.log(res)
            self.clientList = res.result.map(customer => ({
              text: `${customer.firstName} ${customer.surname}`,
              value: customer.id,
              client: customer
            }));
          }
        })
    },
    handleClientChange(index) {
      console.log(`selected ${index}`);
      this.customerId = this.clientList[index].client.id;
      this.client = this.clientList[index].client
      this.loadShopList(this.customerId)
        .then(
          () =>
            this.shopDisable = false
        );
      // clear selected shop IDs
      this.shopIDs = []
    },
    clientFilterOption(input, option) {
      return (
        option.componentOptions.children[0]
          .text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
    },
    loadShopList(clientID) {
      let self = this
      const param = {clientID: clientID}
      return getAction(url.getShopsByCustomerId, param)
        .then(res => {
          if (res.success) {
            if (res.result.length === 0) {
              self.$message.warning("没有找到当前客户的相关店铺信息");
            }
            console.log(res.result)
            self.shopList = res.result.map(
              shop => ({
                text: shop.erpCode,
                value: shop.id,
              })
            );
          } else {
            self.$message.warning("Internal server error. Try later.");
          }
        })
    },
    handleShopChange(value) {
      // value returned is array of shop
      this.shopIDs = value
      console.log(this.shopIDs)
      if (this.shopIDs.length !== 0) {
        this.loadAvailableDate()
      } else {
        this.dataDisable = true
      }
    },
  }
}
</script>

<style scoped>

</style>