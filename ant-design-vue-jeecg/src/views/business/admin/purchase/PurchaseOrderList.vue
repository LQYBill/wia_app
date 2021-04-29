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
      <a-button type="primary" icon="plus" @click="handleAdd">新增</a-button>
      <a-button type="primary" icon="download" @click="handleExportXls('商品采购订单')">导出</a-button>
      <a-upload name="file"
                :showUploadList="false"
                :multiple="false"
                :headers="tokenHeader" :action="importExcelUrl"
                @change="handleImportExcel">
        <a-button type="primary" icon="import">导入</a-button>
      </a-upload>
      <!-- 高级查询区域 -->
      <j-super-query :fieldList="superFieldList" ref="superQueryModal"
                     @handleSuperQuery="handleSuperQuery"></j-super-query>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="batchDel">
            <a-icon type="delete"/>
            <span>删除</span>
          </a-menu-item>
        </a-menu>
        <a-button>
          <span>批量操作</span>
          <a-icon type="down"/>
        </a-button>
      </a-dropdown>
    </div>
    <!-- 操作按钮区域 end -->

    <!-- table区域 begin -->
    <div>

      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>已选择</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>项</span>
          <a style="margin-left: 24px" @click="onClearSelected">清空</a>
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
            <a-tab-pane tab="商品采购订单SKU" key="purchaseOrderSku" forceRender>
              <purchase-order-sku-sub-table :record="record"/>
            </a-tab-pane>
            <a-tab-pane tab="SKU采购折扣历史" key="skuPromotionHistory" forceRender>
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


        <template slot="fileSlot" slot-scope="fileName, record">
          <span v-if="!fileName" style="font-size: 12px;font-style: italic;">无文件</span>
          <a-button
            v-else
            ghost
            type="primary"
            icon="download"
            size="small"
            @click="downloadFile(fileName)"
          >
            <span>预览</span>
          </a-button>
        </template>

        <template slot="action" slot-scope="ID, record">
          <a-popconfirm
            title="确认将订单状态改为“已付款”？"
            ok-text="确认"
            cancel-text="取消"
            @confirm="confirmPayment(ID)"
            :disabled="record['status'] === 'confirmed' || record['status'] === 'purchasing'"
          >
            <a-button :disabled="record['status'] === 'confirmed' || record['status'] === 'purchasing'">
              <a-icon type="pay-circle" />
              确认支付
            </a-button>
          </a-popconfirm>
          <a-divider type="vertical" />
          <a-popconfirm
            title="确认将订单状态改为“采购中”？"
            ok-text="确认"
            cancel-text="取消"
            @confirm="confirmPurchase(ID)"
            :disabled="record['status'] === 'purchasing'"
          >
            <a-button :disabled="record['status'] === 'purchasing'">
              <a-icon type="shopping-cart" />
              采购开始
            </a-button>
          </a-popconfirm>
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
import {saveAs} from 'file-saver';
import {makeFile, getFile, getAction, postAction} from '@/api/manage';

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
          title: '创建日期',
          align: 'center',
          sorter: true,
          dataIndex: 'createTime',
        },
        {
          title: '订单发票号',
          align: 'center',
          dataIndex: 'invoiceNumber',
        },
        {
          title: '客户ID',
          align: 'center',
          dataIndex: 'clientId_dictText'
        },
        {
          title: '应付金额',
          align: 'center',
          dataIndex: 'totalAmount',
        },
        {
          title: '减免总金额',
          align: 'center',
          dataIndex: 'discountAmount',
        },
        {
          title: '最终金额',
          align: 'center',
          dataIndex: 'finalAmount',
        },
        {
          title: '订单状态',
          dataIndex: 'status',
          align: 'center',
          width: 147,
          customRender: (
            t => {
              switch (t) {
                case "waitingPayment":
                  return '等待支付'
                case "proofUploaded":
                  return '已上传支付凭证'
                case "confirmed":
                  return "已支付"
                case "purchasing":
                  return "采购中"
              }
            })
        },
        {
          title: '支付凭证',
          dataIndex: 'paymentDocument',
          align: 'center',
          width: 147,
          scopedSlots: {customRender: 'fileSlot'},
        },
        {
          title: '操作',
          dataIndex: 'id',
          align: 'center',
          scopedSlots: {customRender: 'action'},
        }
      ],
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: '/business/purchaseOrder/list',
        delete: '/business/purchaseOrder/delete',
        deleteBatch: '/business/purchaseOrder/deleteBatch',
        exportXlsUrl: '/business/purchaseOrder/exportXls',
        importExcelUrl: '/business/purchaseOrder/importExcel',
        downloadFile: '/business/purchaseOrder/downloadFile',
        confirmPayment: '/business/purchaseOrder/confirmPayment',
        confirmPurchase: '/business/purchaseOrder/confirmPurchase',
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
    },
    downloadFile(filename) {
      // download file by name
      const param = {filename: filename}
      getFile(this.url.downloadFile, param)
        .then(res => {
          console.log(res)
          //let rawData = window.atob(res.result.data)
          //console.log("decode: \n" + rawData)
          saveAs(res, filename)
        })
    },
    confirmPayment(purchaseID) {
      const params = {purchaseID: purchaseID}
      postAction(this.url.confirmPayment, params)
        .then(res => {
          if (res.success) {
            this.loadData()
          }
        })
    },
    confirmPurchase(purchaseID) {
      const params = {purchaseID: purchaseID}
      postAction(this.url.confirmPurchase, params)
        .then(res => {
          if (res.success) {
            this.loadData()
          }
        })
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>