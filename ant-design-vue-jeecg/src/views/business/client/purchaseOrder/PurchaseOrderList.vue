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

        <template slot="uploadSlot" slot-scope="cellValue, line">
          <a-upload
            :disabled="disableUploadButton(cellValue)"
            :action="url.upload"
            :multiple="false"
            :headers="tokenHeader"
            @change="handleChange"
            :showUploadList="false"
            :data="{purchaseID: line['id']}"
            :beforeUpload="checkFile"
          >
            <a-button size="small" :disabled="cellValue === 'confirmed'">
              <div v-if="cellValue === 'waitingPayment'">
                <a-icon type="plus"/>
                Upload
              </div>
              <div v-else-if="cellValue === 'paid'">
                <a-icon type="delete"/>
                Delete & re-Upload
              </div>
              <div v-else>
                <a-icon type="check"/>
                Already Confirmed
              </div>
            </a-button>
          </a-upload>
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
          title: 'Amount to be paid(€)',
          align: 'center',
          dataIndex: 'finalAmount',
        },
        {
          title: 'Status',
          align: 'center',
          dataIndex: 'status',
          key: 'status',
          width: 147,
          customRender: (
            t => {
              switch (t) {
                case "waitingPayment":
                  return 'Waiting Payment'
                case "paid":
                  return 'Paid'
                case "confirmed":
                  return "Confirmed"
              }
            })
        },
        {
          title: 'Payment Document',
          align: 'center',
          dataIndex: 'status',
          key: 'doc_status',
          scopedSlots: {customRender: 'uploadSlot'},
        }
      ],
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: URL_PREFIX + 'list',
        upload: window._CONFIG['domainURL'] + URL_PREFIX + 'uploadPaymentFile'
      },
      superFieldList: [],
      // upload button
      headers: {
        authorization: 'authorization-text',
      },
    }
  },
  created() {
    this.getSuperFieldList();
  },
  computed: {
  },
  methods: {
    initDictConfig() {
    },

    disableUploadButton(status) {
      return status !== 'waitingPayment'
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
    handleChange(info) {
      if (info.file.status !== 'uploading') {
        console.log(info.file, info.fileList);
      }
      if (info.file.status === 'done') {
        this.$message.success(`${info.file.name} file uploaded successfully`);
      } else if (info.file.status === 'error') {
        this.$message.error(`${info.file.name} file upload failed.`);
      }
      this.loadData()
    },
    checkFile(file, fileList) {
      if (file.name.length >= 50) {
        this.$message.warn("Filename is too long, can not exceed 50 characters!")
        return false
      }
      return true
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>