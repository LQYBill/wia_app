<template>
  <a-card class="j-inner-table-wrapper" :bordered="false">
    <template>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic title="SKU Number" :value="orderData.skuNumber" style="margin-right: 50px">
            <template #suffix>
              <span>units</span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic title="Total Quantity" :value="orderData.totalQuantity" class="demo-class">
            <template #suffix>
              <span>units</span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic title="Estimated Price" :value="orderData.estimatedTotalPrice" class="demo-class">
            <template #suffix>
              <span>€</span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic title="Reduced Amount" :value="orderData.reducedAmount" class="demo-class">
            <template #suffix>
              <span>€</span>
            </template>
          </a-statistic>
        </a-col>
      </a-row>
    </template>

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
          <span>{{ $t("operation.selected") }}</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>{{ $t("order.orders") }}</span>
          <a style="margin-left: 24px" @click="onClearSelected">{{ $t("operation.clearAll") }}</a>
        </template>
      </a-alert>

      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-button style="margin-left: 8px">
          <a @click="handleOrder">下单</a>
        </a-button>
      </a-dropdown>

      <a-table
        ref="table"
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
        :rowSelection="{selectedRowKeys, onChange: computeInfo}"
        @expand="handleExpand"
        @change="handleTableChange"
      >

        <!-- 内嵌table区域 begin -->
        <template slot="expandedRowRender" slot-scope="record">
          <a-tabs tabPosition="top">
            <a-tab-pane tab="Order Contents" key="platformOrderContent" forceRender>
              <platform-order-content-sub-table :record="record"/>
            </a-tab-pane>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

      </a-table>
    </div>
    <!-- table区域 end -->

    <!-- 表单区域 -->
    <platform-order-modal ref="modalForm" @ok="modalFormOk"/>
  </a-card>
</template>

<script>

import {JeecgListMixin} from '@/mixins/JeecgListMixin'
import PlatformOrderModal from './modules/ClientPlatformOrderModal'
import PlatformOrderContentSubTable from './subTables/ClientPlatformOrderContentSubTable'
import PurchaseDetail from './PurchaseDetail'

import '@/assets/less/TableExpand.less'

const {postAction} = require("@api/manage");

export default {
  name: 'PlatformOrderList',
  mixins: [JeecgListMixin],
  components: {
    PlatformOrderModal,
    PlatformOrderContentSubTable,
    PurchaseDetail
  },
  data() {
    return {
      description: 'Client platform order page',
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
        list: '/business/clientPlatformOrder/list',
        delete: '/business/clientPlatformOrder/delete',
        deleteBatch: '/business/clientPlatformOrder/deleteBatch',
        exportXlsUrl: '/business/clientPlatformOrder/exportXls',
        importExcelUrl: '/business/clientPlatformOrder/importExcel',
        computeInfo: '/business/clientPlatformOrder/computeInfo'
      },
      superFieldList: [],
      orderData: {
        skuNumber: 0,
        totalQuantity: 0,
        estimatedPrice: 0,
        reducedAmount: 0
      }
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
        value: 'shopId',
        text: '店铺ID',
        dictTable: 'shop',
        dictText: 'erp_code',
        dictCode: 'id'
      })
      fieldList.push({
        type: 'sel_search',
        value: 'logisticChannelName',
        text: '物流渠道',
        dictTable: 'logistic_channel',
        dictText: 'zh_name',
        dictCode: 'zh_name'
      })
      fieldList.push({type: 'string', value: 'platformOrderId', text: '平台订单号码', dictCode: ''})
      fieldList.push({type: 'string', value: 'platformOrderNumber', text: '平台订单交易号', dictCode: ''})
      fieldList.push({type: 'string', value: 'trackingNumber', text: '物流跟踪号', dictCode: ''})
      fieldList.push({type: 'date', value: 'orderTime', text: '订单交易时间'})
      fieldList.push({type: 'date', value: 'shippingTime', text: '订单发货时间'})
      fieldList.push({type: 'string', value: 'recepient', text: '订单收件人', dictCode: ''})
      fieldList.push({type: 'string', value: 'country', text: '订单收件人国家', dictCode: ''})
      fieldList.push({type: 'string', value: 'postcode', text: '订单收件人邮编', dictCode: ''})
      fieldList.push({type: 'BigDecimal', value: 'fretFee', text: '物流挂号费', dictCode: ''})
      fieldList.push({
        type: 'sel_search',
        value: 'shippingInvoiceNumber',
        text: '物流发票号',
        dictTable: 'shipping_invoice',
        dictText: 'invoice_number',
        dictCode: 'id'
      })
      fieldList.push({type: 'string', value: 'status', text: '状态', dictCode: ''})
      this.superFieldList = fieldList
    },
    computeInfo(selectedRowKeys, selectionRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectionRows = selectionRows;

      const params = this.selectedRowKeys
      console.log(params)
      if (params.length === 0) {
        this.orderData = {
          skuNumber: 0,
          totalQuantity: 0,
          estimatedTotalPrice: 0,
          reducedAmount: 0
        }
      } else {
        let self = this
        postAction(this.url.computeInfo, params)
          .then(
            res => {
              console.log(res.result)
              self.orderData = res.result
            }
          )
      }
    },
    handleOrder(){
      this.$refs.modalForm.display()
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>