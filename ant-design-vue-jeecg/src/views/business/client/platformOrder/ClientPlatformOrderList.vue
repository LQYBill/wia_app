<template>
  <a-tabs default-active-key="1" @change="callback">
    <a-tab-pane key="1">
      <span slot="tab">
        Pending orders <a-badge show-zero :count="orderQuantity.pending" :number-style="{ backgroundColor: '#fa541c' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      <pending-order-list/>
    </a-tab-pane>
    <a-tab-pane key="2">
      <span slot="tab">
        Purchasing orders <a-badge show-zero :count="orderQuantity.purchasing" :number-style="{ backgroundColor: '#faad14' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      <purchasing-order-list/>
    </a-tab-pane>
    <a-tab-pane key="3">
      <span slot="tab">
        Processed orders <a-badge show-zero :count="orderQuantity.processed" :number-style="{ backgroundColor: '#a0d911' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      <processed-order-list/>
    </a-tab-pane>
  </a-tabs>
</template>

<script>

import PendingOrderList from './PendingOrderList'
import PurchasingOrderList from './PurchasingOrderList'
import ProcessedOrderList from './ProcessedOrderList'
import { getAction } from '@api/manage'
import '@assets/less/TableExpand.less'

export default {
  name: 'PlatformOrderList',
  components: {
    PendingOrderList,
    PurchasingOrderList,
    ProcessedOrderList
  },
  data() {
    return {
      description: 'Client platform order page',
      url: {
        getData: '/business/clientPlatformOrder/queryQuantities'
      },
      orderQuantity: {
        processed: 0,
        pending: 0,
        purchasing: 0
      }
    }
  },
  created() {
    this.loadData()
  },
  computed: {},
  methods: {
    loadData() {
      this.loading = true
      getAction(this.url.getData).then(res => {
        if (res.success) {
          this.orderQuantity = res.result
        } else {
          this.$error({title: 'Request failed', content: res.message})
        }
      }).finally(() => {
        this.loading = false
      })
    },
    callback(key) {
      console.log(key);
      this.ipagination = {};
    }
  }
}
</script>