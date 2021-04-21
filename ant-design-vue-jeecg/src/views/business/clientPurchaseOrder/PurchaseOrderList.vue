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
    <!-- 操作按钮区域 end -->

    <!-- table区域 begin -->
    <div>

      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>Selected</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>items</span>
          <a style="margin-left: 24px" @click="onClearSelected">Reset</a>
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
            <a-tab-pane tab="Purchase detail" key="purchaseOrderSku" forceRender>
              <purchase-order-sku-sub-table :record="record"/>
            </a-tab-pane>
            <a-tab-pane tab="Promotion detail" key="skuPromotionHistory" forceRender>
              <sku-promotion-history-sub-table :record="record"/>
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
    <purchase-order-modal ref="modalForm" @ok="modalFormOk"/>

  </a-card>
</template>

<script>

import {JeecgListMixin} from '@/mixins/JeecgListMixin'
import PurchaseOrderModal from './modules/PurchaseOrderModal'
import PurchaseOrderSkuSubTable from './subTables/PurchaseOrderSkuSubTable'
import SkuPromotionHistorySubTable from './subTables/SkuPromotionHistorySubTable'
import {filterMultiDictText} from '@/components/dict/JDictSelectUtil'
import '@/assets/less/TableExpand.less'

const URL_PREFIX = "/business/purchaseOrder/client/"
export default {
  name: 'PurchaseOrderList',
  mixins: [JeecgListMixin],
  components: {
    PurchaseOrderModal,
    PurchaseOrderSkuSubTable,
    SkuPromotionHistorySubTable,
  },
  data() {
    return {
      description: '商品采购订单列表管理页面',
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
          title: 'Order Time',
          align: 'center',
          sorter: true,
          dataIndex: 'createTime',
        },
        {
          title: 'Invoice Number',
          align: 'center',
          dataIndex: 'invoiceNumber',
        },
        {
          title: 'Original price(€)',
          align: 'center',
          dataIndex: 'totalAmount',
        },
        {
          title: 'Discount(€)',
          align: 'center',
          dataIndex: 'discountAmount',
        },
        {
          title: 'Final price(€)',
          align: 'center',
          dataIndex: 'finalAmount',
        },
      ],
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: URL_PREFIX + 'list'
      },
      superFieldList: [],
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
      fieldList.push({type: 'datetime', value: 'createTime', text: '创建日期'})
      fieldList.push({type: 'string', value: 'invoiceNumber', text: '订单发票号', dictCode: ''})
      fieldList.push({
        type: 'sel_search',
        value: 'clientId',
        text: '客户ID',
        dictTable: 'client',
        dictText: 'internal_code',
        dictCode: 'id'
      })
      fieldList.push({type: 'BigDecimal', value: 'totalAmount', text: '应付金额', dictCode: ''})
      fieldList.push({type: 'BigDecimal', value: 'discountAmount', text: '减免总金额', dictCode: ''})
      fieldList.push({type: 'BigDecimal', value: 'finalAmount', text: '最终金额', dictCode: ''})
      this.superFieldList = fieldList
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>