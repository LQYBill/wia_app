<template>
  <a-card>
    <a-space>
      <a-select
        show-search
        placeholder="Select a Sku"
        option-filter-prop="children"
        style="width: 800px"
        :filter-option="filterOption"
        @change="handleSelectChange"
        :loading="!res.sku.ready"
      >
        <a-select-option v-for="item in res.sku.data" :value="item.skuId" :key="item.skuId">
          {{ `${item.erpCode} - ${item.enName} - ${item.zhName}` }}
        </a-select-option>
      </a-select>

      <a-button @click="sendReqForChannelHistory" :loading="table.loading">Search</a-button>
    </a-space>
    <a-divider></a-divider>

    <a-table
      :columns="table.column"
      :data-source="table.data"
      :loading="table.loading"
      rowKey="i"
    >
      <div slot="rich_number" slot-scope="value, record">
        <span v-if="value > 0" style="color: red">+{{ value }}</span>
        <span v-else-if="value < 0" style="color: green">{{ value }}</span>
        <span v-else style="color: grey">0</span>
      </div>

      <div slot="rich_percentage" slot-scope="value, record">
        <span v-if="value > 0" style="color: red">+{{ value }}%</span>
        <span v-else-if="value < 0" style="color: green">{{ value }}%</span>
        <span v-else style="color: grey">0</span>
      </div>

    </a-table>


  </a-card>

</template>

<script>
import {getAction} from "@api/manage";

export default {
  name: "SkuChannelHistory",
  data: function () {
    return {
      url: {
        skuList: "/business/sku/client/userSku",
        skuHistory: "/business/sku/client/channelHistory"
      },
      selectedSkuId: undefined,
      res: {
        sku: {
          data: [],
          ready: false
        }
      },
      table: {
        column: [
          {
            title: '#',
            dataIndex: 'i',
            width: 60,
            align: 'center',
          }, {
            title: 'Channel',
            dataIndex: 'channel',
            width: 60,
            align: 'center',
          }, {
            title: 'Country',
            dataIndex: 'country',
            width: 60,
            align: 'center',
          }, {
            title: 'Effective Date',
            dataIndex: 'date',
            width: 60,
            align: 'center',
          }, {
            title: 'Registration Cost',
            dataIndex: 'r_cost',
            width: 60,
            align: 'center',
          }, {
            title: 'Shipping Cost',
            dataIndex: 's_cost',
            width: 60,
            align: 'center',
          }, {
            title: 'Previous Effective Date',
            dataIndex: 'previous_date',
            width: 60,
            align: 'center',
          }, {
            title: 'Previous Registration Cost',
            dataIndex: 'p_r_cost',
            width: 60,
            align: 'center',
          }, {
            title: 'Previous Shipping Cost',
            dataIndex: 'p_s_cost',
            width: 60,
            align: 'center',
          }, {
            title: 'Difference',
            dataIndex: 'diff',
            width: 60,
            align: 'center',
            scopedSlots: {customRender: 'rich_number'},
          }, {
            title: 'Percentage',
            dataIndex: 'diff_per',
            width: 60,
            align: 'center',
            scopedSlots: {customRender: 'rich_percentage'},
          },
        ],
        data: [],
        loading: false
      }


    }
  },

  created() {
    // Retrieve sku list from remote API
    getAction(this.url.skuList)
      .then(
        res => {
          this.res.sku.data = res.result
          console.log(this.res.sku.data)
          this.res.sku.ready = true
        })
  },

  methods: {
    /**
     * search option in the select
     */
    filterOption(input, option) {
      return (
        option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
    },

    handleSelectChange(value) {
      this.selectedSkuId = value
      console.log(this.selectedSkuId)
    },

    sendReqForChannelHistory() {
      if (this.selectedSkuId === undefined) {
        this.$message.warn("Please select a sku !")
        return
      }
      const param = {
        skuId: this.selectedSkuId
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
    },

    transformRow(row, i) {
      let now = row['now']
      let old = row['previous']

      let channel = `${row['englishChannelName']}-${row["chineseChannelName"]}`
      let country = `${row['englishCountryName']}-${row['chineseCountryName']}`

      if (old == null) {
        return {
          i: i,
          channel: channel,
          country: country,
          date: now['effectiveDate'],
          r_cost: now['registrationFee'],
          s_cost: now['shippingFee'],
          previous_date: "N/A",
          p_r_cost: "N/A",
          p_s_cost: "N/A",
          diff: "N/A",
          diff_per: "N/A"
        }
      } else {
        let diff = (now['shippingFee'] - old['shippingFee']).toFixed(2)
        let diff_per = (diff / old['shippingFee']).toFixed(2) * 100
        return {
          i: i,
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