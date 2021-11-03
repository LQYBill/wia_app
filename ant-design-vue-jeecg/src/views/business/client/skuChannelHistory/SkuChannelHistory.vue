<template>
  <a-card :bordered='false'>
    <!-- 查询区域 -->
    <div class='table-page-search-wrapper'>
      <a-row :gutter='24'>
        <a-form-model ref='searchForm' layout='inline' :model='form' :rules='rules'>
          <a-col :md='6' :sm='8'>
            <a-form-model-item
              label='客户'
              :labelCol='{span: 5}'
              :wrapperCol='{span: 18}'
              prop='clientId'
            >
              <a-select
                show-search
                placeholder='输入客户进行搜索'
                :allowClear=true
                option-filter-prop='children'
                :filter-option='clientFilterOption'
                v-model='form.clientId'
              >
                <a-select-option
                  v-for='(item, index) in customerList'
                  :key='index'
                  :value='item.value'
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :md='6' :sm='8'>
            <a-form-model-item label='目的地' prop='countryCode'>
              <a-select
                show-search
                placeholder='输入国家选择'
                style='width: 100%'
                :allowClear=true
                v-model='form.countryCode'
              >
                <a-select-option :value='item.code' v-for='item in countryList' :key='item.code'>
                  {{ item.code + '-' + item.nameZh + '-' + item.nameEn }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <span style='float: left;overflow: hidden;' class='table-page-search-submitButtons'>
          <a-col :md='6' :sm='24'>
            <a-form-model-item>
              <a-button type='primary' htmlType='submit' @click='sendReqForPriceHistory'
                        :loading='table.loading'>搜索</a-button>
            </a-form-model-item>
          </a-col>
        </span>
        </a-form-model>
      </a-row>
    </div>
    <a-divider></a-divider>

    <a-table
      :columns='table.column'
      :data-source='table.data'
      :loading='table.loading'
      :pagination='false'
      rowKey='i'
    >
      <div slot='rich_number' slot-scope='value, record'>
        <span v-if='value > 0' style='color: red'>+{{ value }}</span>
        <span v-else-if='value < 0' style='color: green'>{{ value }}</span>
        <span v-else style='color: grey'>0</span>
      </div>

      <div slot='rich_percentage' slot-scope='value, record'>
        <span v-if='value > 0' style='color: red'>+{{ value }}%</span>
        <span v-else-if='value < 0' style='color: green'>{{ value }}%</span>
        <span v-else style='color: grey'>0</span>
      </div>

    </a-table>


  </a-card>

</template>

<script>
import { getAction } from '@api/manage'

export default {
  name: 'SkuChannelHistory',
  data: function() {
    return {
      customerList: [],
      form: {
        countryCode: undefined,
        clientId: undefined
      },
      countryList: [],
      url: {
        getClientList: '/client/client/all',
        countries: '/business/logisticChannel/countries',
        skuList: '/business/sku/client/userSku',
        skuHistory: '/business/sku/client/channelCountryHistory'
      },
      res: {
        sku: {
          data: [],
          ready: false
        }
      },
      rules: {
        countryCode: [{ required: true, message: '请选择国家', trigger: 'blur' }],
        clientId: [{ required: true, message: '请选择客户', trigger: 'blur' }]
      },
      table: {
        column: [
          {
            title: '#',
            dataIndex: 'i',
            width: 60,
            align: 'center'
          },
          {
            title: 'SKU',
            dataIndex: 'sku',
            width: 60,
            align: 'center'
          },
          /*{
            title: 'Channel',
            dataIndex: 'channel',
            width: 60,
            align: 'center'
          },*/
          {
            title: 'Country',
            dataIndex: 'country',
            width: 60,
            align: 'center'
          },
          {
            title: 'Effective Date',
            dataIndex: 'date',
            width: 60,
            align: 'center'
          },
          {
            title: 'Registration Cost',
            dataIndex: 'r_cost',
            width: 60,
            align: 'center'
          },
          {
            title: 'Shipping Cost',
            dataIndex: 's_cost',
            width: 60,
            align: 'center'
          },
          {
            title: 'Previous Effective Date',
            dataIndex: 'previous_date',
            width: 60,
            align: 'center'
          },
          {
            title: 'Previous Registration Cost',
            dataIndex: 'p_r_cost',
            width: 60,
            align: 'center'
          },
          {
            title: 'Previous Shipping Cost',
            dataIndex: 'p_s_cost',
            width: 60,
            align: 'center'
          },
          {
            title: 'Difference',
            dataIndex: 'diff',
            width: 60,
            align: 'center',
            scopedSlots: { customRender: 'rich_number' }
          },
          {
            title: 'Percentage',
            dataIndex: 'diff_per',
            width: 60,
            align: 'center',
            scopedSlots: { customRender: 'rich_percentage' }
          }
        ],
        data: [],
        loading: false
      }


    }
  },

  created() {
    // load country list
    getAction(this.url.countries)
      .then(res => {
        this.countryList = res.result
      })
    this.loadClientList()
  },

  methods: {
    /**
     * Load client list from API
     */
    loadClientList() {
      let self = this
      getAction(this.url.getClientList)
        .then(res => {
          if (res.success) {
            self.customerList = res.result.map(customer => ({
              text: `${customer.firstName} ${customer.surname}`,
              value: customer.id,
              client: customer
            }))
          }
        })
    },
    clientFilterOption(input, option) {
      return (
        option.componentOptions.children[0]
          .text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      )
    },
    sendReqForPriceHistory() {
      let self = this
      this.$refs.searchForm.validate(
        (valid) => {
          if (valid) {
            let param = {
              clientId: self.form.clientId,
              countryCode: self.form.countryCode
            }
            this.table.loading = true
            getAction(this.url.skuHistory, param)
              .then(res => {
                this.table.loading = false
                this.selectedSkuId = undefined
                if (res.success) {
                  let remote_data = res.result
                  let data = []
                  for (let i = 0; i < remote_data.length; i++) {
                    data.push(this.transformRow(remote_data[i], i))
                  }
                  this.table.data = data
                } else {
                  this.$message.warning(res.message)
                }
              })
          }
        }
      )
    },
    transformRow(row, i) {
      let now = row['now']
      let old = row['previous']

      let channel = `${row['englishChannelName']}`
      let country = `${row['englishCountryName']}`
      let sku = `${row['sku']}`

      if (old == null) {
        return {
          i: i,
          sku: sku,
          channel: channel,
          country: country,
          date: now['effectiveDate'],
          r_cost: now['registrationFee'],
          s_cost: now['shippingFee'],
          previous_date: 'N/A',
          p_r_cost: 'N/A',
          p_s_cost: 'N/A',
          diff: 'N/A',
          diff_per: 'N/A'
        }
      } else {
        let diff = (now['shippingFee'] - old['shippingFee']).toFixed(2)
        let diff_per = ((diff / (old['shippingFee'] + old['registrationFee'])) * 100).toFixed(2)
        return {
          i: i,
          sku: sku,
          channel: channel,
          country: country,
          date: now['effectiveDate'],
          r_cost: now['registrationFee'],
          s_cost: now['shippingFee'],
          previous_date: old['effectiveDate'],
          p_r_cost: old['registrationFee'],
          p_s_cost: old['shippingFee'],
          diff: diff,
          diff_per: diff_per
        }
      }
    }

  }
}
</script>

<style scoped>

</style>