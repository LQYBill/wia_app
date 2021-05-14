<template>
  <a-card class="j-inner-table-wrapper" :bordered="false">

    <!-- 查询区域 begin -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="24">
        </a-row>
      </a-form>
    </div>
    <!-- 查询区域 end -->

    <!-- table区域 begin -->
    <div>

      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>Selected</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>Items</span>
          <a style="margin-left: 24px" @click="onClearSelected">Clear</a>
        </template>
      </a-alert>

      <a-table
        ref="table"
        size="middle"
        bordered
        rowKey="id"
        class="j-table-force-nowrap"
        :scroll="{x:true}"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :expandedRowKeys="expandedRowKeys"
        :rowSelection="{selectedRowKeys, onChange: onSelectChange}"
        @expand="handleExpand"
        @change="handleTableChange"
      >

        <!-- 内嵌table区域 begin -->
        <template slot="expandedRowRender" slot-scope="record">
          <a-tabs tabPosition="top">
            <a-tab-pane tab="SKU价格表" key="skuPrice" forceRender>
              <sku-price-sub-table :record="record"/>
            </a-tab-pane>
            <a-tab-pane tab="SKU物流折扣" key="shippingDiscount" forceRender>
              <shipping-discount-sub-table :record="record"/>
            </a-tab-pane>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

        <template slot="imgSlot" slot-scope="text">
          <div style="font-size: 12px;font-style: italic;">
            <span v-if="!text">No picture available</span>
            <img v-else
                 :src="getImgView(text)"
                 :preview="getImgView(text)"
                 alt="Sku picture"
                 style="min-width:50px;max-width:80px;height:50px;"
            />
          </div>
        </template>

        <template slot="shippingQuantitySlot" slot-scope="text, record, index">
          <a-tooltip title="Purchasing" style="color: #00DB00">
            {{ dealNull(record['greenQuantity']) }}
          </a-tooltip>
          |
          <a-tooltip title="Pending" style="color: #cc0000">
            {{ dealNull(record['redQuantity']) }}
          </a-tooltip>
        </template>

      </a-table>
    </div>
    <!-- table区域 end -->

    <!-- 表单区域 -->
    <popup-confirmation ref="popup" :ok-callback="modalFormOk" :data-for-child="skusToBuy"/>
    <a-space class="bottomButtons">
      <a-button type="danger" @click="onClearSelected">
        Reset
      </a-button>
      <a-button type="primary" @click="handleOrder">
        Place Order
        <a-icon type="right"/>
      </a-button>
    </a-space>

  </a-card>
</template>

<script>

import {JeecgListMixin} from '@/mixins/JeecgListMixin'
import SkuPriceSubTable from './subTables/SkuPriceSubTable'
import ShippingDiscountSubTable from './subTables/ShippingDiscountSubTable'
import '@assets/less/TableExpand.less'
import PopupConfirmation from './modules/ConfirmationContainer'


const rootURL = '/business/inventory/client/'

export default {
  name: 'SkuList',
  mixins: [JeecgListMixin],
  components: {
    SkuPriceSubTable,
    ShippingDiscountSubTable,
    PopupConfirmation
  },
  data() {
    return {
      description: 'SKU表列表管理页面',
      // 表头
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
          scopedSlots: {customRender: 'shippingQuantitySlot'}
        },
        {
          title: '平台单数量',
          align: 'center',
          dataIndex: 'platformOrderQuantity',
          customRender:(t,r,ibdex) => {if (t === null) return 0; else return t}
        }
      ],
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: rootURL + 'list',
        exportXlsUrl: rootURL + 'exportXls',
      },
      superFieldList: [],
      skusToBuy: []
    }
  },
  created() {
    this.getSuperFieldList();
  },
  computed: {
    importExcelUrl() {
      return window._CONFIG['domainURL'] + this.url.importExcelUrl
    }

  },
  methods: {
    initDictConfig() {
    },

    handleExpand(expanded, record) {
      this.expandedRowKeys = []
      if (expanded === true) {
        this.expandedRowKeys.push(record.id)
      }
    },
    getSuperFieldList() {
      let fieldList = [];
      fieldList.push({
        type: 'sel_search',
        value: 'productId',
        text: '商品ID',
        dictTable: 'product',
        dictText: 'code',
        dictCode: 'id'
      })
      fieldList.push({type: 'string', value: 'erpCode', text: 'ERP中商品代码', dictCode: ''})
      fieldList.push({type: 'int', value: 'availableAmount', text: '库存数量', dictCode: ''})
      fieldList.push({type: 'int', value: 'purchasingAmount', text: '在途数量', dictCode: ''})
      fieldList.push({type: 'string', value: 'imageSource', text: '图片链接', dictCode: ''})
      this.superFieldList = fieldList
    },
    handleOrder() {
      this.skusToBuy = this.selectionRows.map(r => (r['id']))
      this.$refs.popup.display()
    },
    dealNull(value) {
      if (value === null) {
        return 0
      }
      return value
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>