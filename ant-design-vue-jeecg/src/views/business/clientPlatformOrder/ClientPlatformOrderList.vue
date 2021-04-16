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
          <span>{{ $t("selected") }}</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>{{ $t("orders") }}</span>
          <a style="margin-left: 24px" @click="onClearSelected">{{$t("clearAll")}}</a>
        </template>
      </a-alert>

      <a-table
        ref="table"
        size="middle"
        bordered
        rowKey="id"
        class="j-table-force-nowrap"
        :scroll="{ x: true }"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource"
        :expandedRowKeys="expandedRowKeys"
        :rowSelection="{selectedRowKeys, onChange: computeInfo}"
        @expand="handleExpand"
        @change="handleTableChange"
      >

        <!-- 内嵌table区域 begin -->
        <template slot="expandedRowRender" slot-scope="record">
          <a-tabs tabPosition="top">
            <a-tab-pane :tab="$t('orderContent')" key="platformOrderContent" forceRender>
              <platform-order-content-sub-table :record="record"/>
            </a-tab-pane>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

        <template slot="htmlSlot" slot-scope="text">
          <div v-html="text"></div>
        </template>

        <template slot="imgSlot" slot-scope="text">
          <div style="font-size: 12px;font-style: italic;">
            <span v-if="!text">无图片</span>
            <img v-else :src="getImgView(text)" alt="" style="max-width:80px;height:25px;"/>
          </div>
        </template>


        <template slot="fileSlot" slot-scope="text">
          <span v-if="!text" style="font-size: 12px;font-style: italic;">无文件</span>
          <a-button
            v-else
            ghost
            type="primary"
            icon="download"
            size="small"
            @click="downloadFile(text)"
          >
            <span>下载</span>
          </a-button>
        </template>

        <template slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">编辑</a>
          <a-divider type="vertical"/>
          <a-dropdown>
            <a class="ant-dropdown-link">
              <span>更多 <a-icon type="down"/></span>
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                  <a>删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

        </template>

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
import {filterMultiDictText} from '@/components/dict/JDictSelectUtil'
import '@/assets/less/TableExpand.less'

const {postAction} = require("@api/manage");

export default {
  name: 'PlatformOrderList',
  mixins: [JeecgListMixin],
  components: {
    PlatformOrderModal,
    PlatformOrderContentSubTable,
  },
  data() {
    return {
      description: '平台订单表列表管理页面',
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
          title: '店铺ID',
          align: 'center',
          dataIndex: 'shopId_dictText',
          width: 60,
        },
        {
          title: '物流渠道',
          align: 'center',
          dataIndex: 'logisticChannelName_dictText',
          ellipsis: true
        },
        {
          title: '平台订单号码',
          align: 'center',
          dataIndex: 'platformOrderId',
          width: 120,
        },
        {
          title: '平台订单交易号',
          align: 'center',
          dataIndex: 'platformOrderNumber',
          width: 100,
        },
        {
          title: '物流跟踪号',
          align: 'center',
          dataIndex: 'trackingNumber',
          width: 120,
        },
        {
          title: '订单交易时间',
          align: 'center',
          dataIndex: 'orderTime',
          width: 120,
        },
        {
          title: '订单发货时间',
          align: 'center',
          dataIndex: 'shippingTime',
          width: 120,
        },
        {
          title: '订单收件人',
          align: 'center',
          dataIndex: 'recepient',
        },
        {
          title: '订单收件人国家',
          align: 'center',
          dataIndex: 'country',
          width: 120,
        },
        {
          title: '订单收件人邮编',
          align: 'center',
          dataIndex: 'postcode',
          width: 80,
        },
        {
          title: '物流挂号费',
          align: 'center',
          dataIndex: 'fretFee',
          width: 80,
        },
        {
          title: '物流发票号',
          align: 'center',
          dataIndex: 'shippingInvoiceNumber_dictText',
          width: 147,
        },
        {
          title: '状态',
          align: 'center',
          dataIndex: 'status',
          width: 147,
        },
        {
          title: '操作',
          dataIndex: 'action',
          align: 'center',
          width: 147,
          scopedSlots: {customRender: 'action'},
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
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>