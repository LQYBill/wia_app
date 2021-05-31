<template>
  <div>
    <dynamic-user-table
      :data-source-url="url.list"
      :user-config="userConfig"
      :host="this"
      ref="table"
    >
      <template slot="sub-table" slot-scope="record">
        <a-tab-pane tab="SKU价格表" key="skuPrice" forceRender>
          <sku-price-sub-table :record="record.record"/>
        </a-tab-pane>
        <a-tab-pane tab="SKU物流折扣" key="shippingDiscount" forceRender>
          <shipping-discount-sub-table :record="record.record"/>
        </a-tab-pane>
      </template>

      <template slot="redAndGreenCell" slot-scope="tuple">
        <a-tooltip title="Purchasing" style="color: #00DB00">
          {{ dealNull(tuple.record['greenQuantity']) }}
        </a-tooltip>
        |
        <a-tooltip title="Pending" style="color: #cc0000">
          {{ dealNull(tuple.record['redQuantity']) }}
        </a-tooltip>
      </template>

      <template slot="salesQuantitySlot" slot-scope="tuple">
        <a-tooltip title="Sales from last 7 days">
          {{ dealNull(tuple.record['sales7']) }}
        </a-tooltip>
        |
        <a-tooltip title="Sales from last 14 days">
          {{ dealNull(tuple.record['sales14']) }}
        </a-tooltip>
        |
        <a-tooltip title="Sales from last 28 days">
          {{ dealNull(tuple.record['sales28']) }}
        </a-tooltip>
      </template>

      <template slot="imgSlot" slot-scope="tuple">
        <span v-if="!tuple.record['imageSource']">No picture available</span>
        <img v-else
             :src="getImgView(tuple.record['imageSource'])"
             :preview="getImgView(tuple.record['imageSource'])"
             alt="SKU picture"
             style="min-width:50px;max-width:80px;height:50px;"
        />
      </template>

    </dynamic-user-table>
    <popup-confirmation ref="popup" :ok-callback="modalFormOk" :data-for-child="skuToBuy"/>
  </div>

</template>

<script>

import DynamicUserTable from "@comp/dynamicTablePage/DynamicUserTable";
import SkuPriceSubTable from './subTables/SkuPriceSubTable'
import ShippingDiscountSubTable from './subTables/ShippingDiscountSubTable'
import PopupConfirmation from './modules/ConfirmationContainer'
import {getFileAccessHttpUrl} from "@api/manage";
import role_config from "./RoleConfig"



const rootURL = '/business/inventory/client/'

export default {
  name: 'Inventory2',
  components: {
    DynamicUserTable,
    SkuPriceSubTable,
    ShippingDiscountSubTable,
    PopupConfirmation
  },
  data() {
    return {
      // table head
      columns: [],
      url: {
        list: rootURL + 'list',
        exportXlsUrl: rootURL + 'exportXls',
      },
      skuToBuy: [],
      currentUser: Object,
      userConfig:role_config,
    }
  },
  computed: {}
  ,
  created() {
  },
  methods: {
    clear() {
    },
    dealNull(value) {
      if (value === null) {
        return 0
      }
      return value
    },
    getImgView(text) {
      if (text && text.indexOf(",") > 0) {
        text = text.substring(0, text.indexOf(","))
      }
      return getFileAccessHttpUrl(text)
    },
    modalFormOk() {
      // reload table data
      this.$refs.table.loadData();
      // clear selected rows
      this.$refs.table.clearSelected();
    }
  }
}
</script>
<style scoped>
</style>