<template>
  <a-card>
    <a-space>
      <a-select
        show-search
        placeholder="Select a Sku"
        option-filter-prop="children"
        style="width: 200px"
        :filter-option="filterOption"
        @change="handleSelectChange"
        :loading="!res.sku.ready"
      >
        <a-select-option v-for="item in res.sku.data" :value="item.id" :key="item.id">
          {{ `${item.erpCode}` }}
        </a-select-option>
      </a-select>

      <a-button @click="sendReqForChannelHistory">Search</a-button>
    </a-space>
    <a-divider></a-divider>

    <a-table
      :columns="table.column"
      :data-source="table.data"
      :loading="table.loading"
      rowKey="rowIndex"
    >

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
        skuList: "/business/sku/all",
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
            key: 'rowIndex',
            width: 60,
            align: 'center',
            customRender: (t, r, index) => parseInt(index) + 1
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
          }, {
            title: 'Percentage',
            dataIndex: 'diff_per',
            width: 60,
            align: 'center',
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
    },

    sendReqForChannelHistory() {
      const param = {
        skuId: "123"
      }
      this.table.loading = true
      getAction(this.url.skuHistory, param)
        .then(res => {
          this.table.loading = false
          let remote_data = res.result
          this.table.data = remote_data.map(this.transformRow)

        })
    },

    transformRow(row) {
      let now = row['now']
      let old = row['previous']

      let channel = `${row['englishChannelName']}-${row["chineseChannelName"]}`
      let country = `${row['englishCountryName']}-${row['chineseCountryName']}`

      let diff = now['shippingFee'] - old['shippingFee']
      let diff_per = diff / old['shippingFee']
      return {
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
</script>

<style scoped>

</style>