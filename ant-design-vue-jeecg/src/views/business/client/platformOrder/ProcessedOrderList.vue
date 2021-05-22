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

    <!-- 操作按钮区域 begin -->
    <div class="table-operator">
      <a-button type="primary" icon="download" @click="handleExportXls('ProcessedOrders')">{{ $t("Export") }}</a-button>
      <!-- 高级查询区域 -->
      <j-super-query :fieldList="superFieldList" ref="superQueryModal"
                     @handleSuperQuery="handleSuperQuery"></j-super-query>
    </div>
    <!-- 操作按钮区域 end -->

    <!-- table区域 begin -->
    <div>

      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>{{ $t("operation.selected") }}</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>{{ $t("order.orders") }}</span>
          <a style="margin-left: 24px" @click="onClearSelected">{{ $t("operation.reset") }}</a>
        </template>
      </a-alert>


      <a-table
        ref="table3"
        size="middle"
        bordered
        rowKey="id"
        class="j-table-force-nowrap"
        :scroll="{ x: true}"
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
            <a-tab-pane tab="Order Contents" key="platformOrderContent" forceRender>
              <order-content :record="record"/>
            </a-tab-pane>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

      </a-table>
    </div>
    <!-- table区域 end -->

    <!-- 表单区域 -->
    <popup-confirmation ref="popup" :ok-callback="modalFormOk" :data-for-child="selectedRowKeys"/>
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
import OrderContent from './subTables/OrderContent'

import '@assets/less/TableExpand.less'

const {postAction} = require("@api/manage");

export default {
  name: 'ProcessedOrderList',
  mixins: [JeecgListMixin],
  components: {
    OrderContent
  },
  data() {
    return {
      description: 'Processed order page',
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
          title: this.$t('storeID'),
          align: 'center',
          dataIndex: 'shopId_dictText',
          width: 60,
        },
        {
          title: this.$t('order.Num'),
          align: 'center',
          dataIndex: 'platformOrderId',
          width: 120,
        },
        {
          title: this.$t("order.transNum"),
          align: 'center',
          dataIndex: 'platformOrderNumber',
          width: 60,
          ellipsis: true,
        },
        {
          title: this.$t('order.trackingNum'),
          align: 'center',
          dataIndex: 'trackingNumber',
          width: 120,
        },
        {
          title: this.$t("order.transTime"),
          align: 'center',
          dataIndex: 'orderTime',
          width: 120,
        },
        {
          title: this.$t("order.deliveryTime"),
          align: 'center',
          dataIndex: 'shippingTime',
          width: 120,
        },
        {
          title: this.$t("recipient.recipient"),
          align: 'center',
          dataIndex: 'recipient',
          ellipsis: true,
          width: 50
        },
        {
          title: this.$t("recipient.country"),
          align: 'center',
          dataIndex: 'country',
          width: 120,
        },
        {
          title: this.$t("recipient.postalCode"),
          align: 'center',
          dataIndex: 'postcode',
          width: 80,
        },
        {
          title: this.$t("logistics.registerFee"),
          align: 'center',
          dataIndex: 'fretFee',
          width: 80,
        },
        {
          title: this.$t("logistics.invoiceNum"),
          align: 'center',
          dataIndex: 'shippingInvoiceNumber_dictText',
          width: 147,
        },
        {
          title: this.$t("status"),
          align: 'center',
          dataIndex: 'status_dictText',
          width: 147,
        },
      ],
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: '/business/clientPlatformOrder/listProcessed',
        delete: '/business/clientPlatformOrder/delete',
        deleteBatch: '/business/clientPlatformOrder/deleteBatch',
        exportXlsUrl: '/business/clientPlatformOrder/exportXls',
        importExcelUrl: '/business/clientPlatformOrder/importExcel'
      },
      superFieldList: []
    }
  },
  created() {
    this.getSuperFieldList();
  },
  computed: {},
  methods: {
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
        value: 'shopId',
        text: 'Shop ID',
        dictTable: 'shop',
        dictText: 'erp_code',
        dictCode: 'id'
      })
      fieldList.push({type: 'string', value: 'platformOrderId', text: 'Serial Number', dictCode: ''})
      fieldList.push({type: 'string', value: 'platformOrderNumber', text: 'Order Number', dictCode: ''})
      fieldList.push({type: 'string', value: 'trackingNumber', text: 'Tracking Number', dictCode: ''})
      fieldList.push({type: 'date', value: 'orderTime', text: 'Order time'})
      fieldList.push({type: 'date', value: 'shippingTime', text: 'Shipping time'})
      fieldList.push({type: 'string', value: 'recepient', text: 'Recipient', dictCode: ''})
      fieldList.push({type: 'string', value: 'country', text: 'Country', dictCode: ''})
      fieldList.push({type: 'string', value: 'postcode', text: 'Postcode', dictCode: ''})
      fieldList.push({type: 'BigDecimal', value: 'fretFee', text: 'FRET fees', dictCode: ''})
      fieldList.push({
        type: 'sel_search',
        value: 'shippingInvoiceNumber',
        text: 'Shipping fees invoice number',
        dictTable: 'shipping_invoice',
        dictText: 'invoice_number',
        dictCode: 'id'
      })
      fieldList.push({type: 'string', value: 'status', text: 'Status', dictCode: ''})
      this.superFieldList = fieldList
    }
  }
}
</script>