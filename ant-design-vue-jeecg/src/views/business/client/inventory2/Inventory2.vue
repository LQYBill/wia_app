<template>
  <DataTableAndAction
    cancel-text="cancel"
    ok-text="OK!"
    :ok-handler="handleOK"
    :columns="columns"
    :url="url"
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
      <a-tooltip title="purchasing" style="color: #00DB00">
        {{ dealNull(tuple.record['greenQuantity']) }}
      </a-tooltip>
      |
      <a-tooltip title="Pending" style="color: #cc0000">
        {{ dealNull(tuple.record['redQuantity']) }}
      </a-tooltip>
    </template>

    <template slot="imgSlot" slot-scope="tuple">
      <span v-if="!tuple.record['imageSource']">No picture available</span>
      <img v-else
           :src="getImgView(tuple.record['imageSource'])"
           :preview="getImgView(tuple.record['imageSource'])"
           alt="Sku picture"
           style="min-width:50px;max-width:80px;height:50px;"
      />
    </template>

    <template slot="popup" slot-scope="tuple">
      <popup-confirmation ref="popup" :ok-callback="modalFormOk" :sku-identifiers="skuToBuy"/>
    </template>

  </DataTableAndAction>

</template>

<script>

import DataTableAndAction from "@comp/dataDisplay/DataTableAndAction";
import SkuPriceSubTable from './subTables/SkuPriceSubTable'
import ShippingDiscountSubTable from './subTables/ShippingDiscountSubTable'
import PopupConfirmation from './modules/ConfirmationContainer'
import {getFileAccessHttpUrl} from "@api/manage";

const rootURL = '/business/inventory/client/'

export default {
  name: 'new inventory',
  components: {
    DataTableAndAction,
    SkuPriceSubTable,
    ShippingDiscountSubTable,
    PopupConfirmation
  },
  data() {
    return {
      // table head
      columns: [
        {
          title: '#',
          key: 'rowIndex',
          width: 60,
          align: 'center',
          customRender: (t, r, index) => parseInt(index) + 1
        },
        {
          title: 'Picture',
          align: 'center',
          dataIndex: 'imageSource',
          scopedSlots: {customRender: 'imgSlot'}
        },
        {
          title: 'ERP Code',
          align: 'center',
          dataIndex: 'erpCode',
        },
        {
          title: 'Name',
          align: 'center',
          dataIndex: 'productId_dictText',
        },
        {
          title: '库存数量',
          align: 'center',
          dataIndex: 'availableAmount',
        },
        {
          title: '在途数量',
          dataIndex: 'redQuantity',
          align: 'center',
          scopedSlots: {customRender: 'redAndGreenCell'}
        },
        {
          title: '平台单数量',
          align: 'center',
          dataIndex: 'platformOrderQuantity',
          customRender: (t, r, ibdex) => {
            if (t === null) return 0; else return t
          }
        }
      ],
      url: {
        list: rootURL + 'list',
        exportXlsUrl: rootURL + 'exportXls',
      },
      skuToBuy:[]
    }
  },
  computed:{
  }
  ,
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
    handleOK(keys, records) {
      this.skusToBuy = records.map(r => (r['id']))
      console.log(this.skusToBuy)
      this.$refs.popup.display(this.skusToBuy)
    },
    modalFormOk() {
      // 新增/修改 成功时，重载列表
      this.$refs.table.loadData();
      //清空列表选中
      this.$refs.table.clearSelected();
    },
  }
}
</script>
<style scoped>
</style>