<template>
  <dynamic-user-table
    :data-source-url="url.list"
    :user-config="roleConfig"
    :host="this"
    ref="table">

    <template slot="sub-table" slot-scope="record">
      <a-tab-pane tab="Purchase detail" key="purchaseOrderSku" forceRender>
        <purchase-order-sku-sub-table :record="record.record"/>
      </a-tab-pane>
      <a-tab-pane tab="Promotion detail" key="skuPromotionHistory" forceRender>
        <sku-promotion-history-sub-table :record="record.record"/>
      </a-tab-pane>
    </template>

    <template slot="uploadSlot" slot-scope="tuple">
      <a-upload
        :disabled="disableUploadButton(tuple.text)"
        :action="url.upload"
        :multiple="false"
        :headers="tokenHeader"
        @change="handleChange"
        :showUploadList="false"
        :data="{purchaseID: tuple.record['id']}"
        :beforeUpload="checkFile"
      >
        <a-button size="small" :disabled="tuple.text === 'confirmed' || tuple.text === 'purchasing'">
          <div v-if="tuple.text === 'waitingPayment'">
            <a-icon type="plus"/>
            Upload
          </div>
          <div v-else-if="tuple.text === 'proofUploaded'">
            <a-icon type="delete"/>
            Delete & re-Upload
          </div>
          <div v-else-if="tuple.text === 'confirmed'">
            <a-icon type="check"/>
            Payment Confirmed
          </div>
          <div v-else>
            <a-icon type="check"/>
            Purchasing
          </div>
        </a-button>
      </a-upload>
    </template>

    <template slot="invoiceSlot" slot-scope="tuple">
      <a-button
        ghost
        type="primary"
        icon="download"
        size="small"
        :disabled="!canDownloadInvoice(tuple.record['status'])"
        @click="downloadInvoice(tuple.record)"
      >
        <span v-if="canDownloadInvoice(tuple.record['status'])">Download</span>
        <span v-else>Not available</span>
      </a-button>

    </template>

    <template slot="fileSlot" slot-scope="{text, record, index}">
      <span v-if="!fileName" style="font-size: 12px;font-style: italic;">无文件</span>
      <a-button
        v-else
        ghost
        type="primary"
        icon="download"
        size="small"
        @click="downloadPaymentFile(fileName)"
      >
        <span>预览</span>
      </a-button>
    </template>

    <template slot="accountAction" slot-scope="{text, record, index}">
      <a-popconfirm
        title="确认将订单状态改为“已付款”？"
        ok-text="确认"
        cancel-text="取消"
        @confirm="confirmPayment(text)"
        :disabled="record['status'] === 'confirmed' || record['status'] === 'purchasing'"
      >
        <a-button :disabled="record['status'] === 'confirmed' || record['status'] === 'purchasing'">
          <a-icon type="pay-circle"/>
          确认支付
        </a-button>
      </a-popconfirm>
    </template>

    <template slot="buyerAction" slot-scope="{text, record, index}">
      <a-popconfirm
        title="确认将订单状态改为“采购中”？"
        ok-text="确认"
        cancel-text="取消"
        @confirm="confirmPurchase(text)"
        :disabled="record['status'] === 'purchasing'"
      >
        <a-button :disabled="record['status'] === 'purchasing'">
          <a-icon type="shopping-cart"/>
          采购开始
        </a-button>
      </a-popconfirm>
    </template>

  </dynamic-user-table>

</template>

<script>

import {JeecgListMixin} from '@/mixins/JeecgListMixin'
import PurchaseOrderModal from './modules/PurchaseOrderModal'
import PurchaseOrderSkuSubTable from './subTables/PurchaseOrderSkuSubTable'
import SkuPromotionHistorySubTable from './subTables/SkuPromotionHistorySubTable'
import '@/assets/less/TableExpand.less'
import DynamicUserTable from "@comp/dynamicTablePage/DynamicUserTable";
import roleConfig from "./RoleConfig"
import {ACCESS_TOKEN, TENANT_ID} from "@/store/mutation-types"
import Vue from 'vue'
import {getFile, postAction} from "@api/manage";
import Template1 from "@views/jeecg/JVxeDemo/layout-demo/Template1";
import {saveAs} from 'file-saver';

const {getAction} = require("@api/manage");

const URL_PREFIX = "/business/purchaseOrder/client/"
export default {
  name: 'PurchaseOrderList',
  mixins: [JeecgListMixin],
  components: {
    Template1,
    PurchaseOrderModal,
    PurchaseOrderSkuSubTable,
    SkuPromotionHistorySubTable,
    DynamicUserTable
  },
  data() {
    return {
      description: 'Purchase management',
      // 字典选项
      dictOptions: {},
      // 展开的行test
      expandedRowKeys: [],
      url: {
        list: URL_PREFIX + 'list',
        upload: window._CONFIG['domainURL'] + URL_PREFIX + 'uploadPaymentFile',
        downloadFile: '/business/purchaseOrder/downloadFile',
        confirmPayment: '/business/purchaseOrder/confirmPayment',
        confirmPurchase: '/business/purchaseOrder/confirmPurchase',
        downloadInvoice: '/business/purchaseOrder/downloadInvoice',
        InvoiceMeta: '/business/purchaseOrder/invoiceMeta',
      },
      superFieldList: [],
      roleConfig: roleConfig,
      // upload button
      headers: {
        authorization: 'authorization-text',
      },
    }
  },
  created() {
  },
  computed: {
    tokenHeader() {
      let head = {'X-Access-Token': Vue.ls.get(ACCESS_TOKEN)}
      let tenantid = Vue.ls.get(TENANT_ID)
      if (tenantid) {
        head['tenant-id'] = tenantid
      }
      return head;
    },
  },
  methods: {
    initDictConfig() {
    },

    disableUploadButton(status) {
      return status === 'confirmed'
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
    },
    /**
     * Client can download purchase invoice when the purchase status is confirmed,
     * purchasing or received.
     * @param status the status of a purchase
     * @returns {boolean}
     */
    canDownloadInvoice(status) {
      return status === "confirmed" || status === "purchasing" || status === "received";
    },

    /**
     * Download payment file by its filename.
     * @param filename name of the file to download.
     */
    downloadPaymentFile(filename) {
      // download file by name
      const param = {filename: filename}
      getFile(this.url.downloadFile, param)
        .then(res => {
          console.log(res)
          saveAs(res, filename)
        })
    },
    /**
     * Download purchase invoice by purchase ID
     * @param purchaseID ID of the purchase that to download invoice
     */
    downloadInvoice(record) {
      const param = {purchaseID: record["id"]}
      getAction(this.url.InvoiceMeta, param).then(
        res => {
          console.log(res)
          let entity = res.entity
          let code = res.code;
          const param = {invoiceCode: res.code}
          getFile(this.url.downloadInvoice, param)
            .then(res => {
              console.log(res)
              saveAs(res, "Invoice N°" + code + " (" + entity + ").xlsx")
            })
        }
      )

    },
    /**
     * Change status of a purchase to confirmed.
     * @param purchaseID ID of the purchase to change.
     */
    confirmPayment(purchaseID) {
      const params = {purchaseID: purchaseID}
      postAction(this.url.confirmPayment, params)
        .then(res => {
          if (res.success) {
            this.$refs.table.loadData()
          }
        })
    },
    /**
     * Change status of a purchase to purchasing.
     * @param purchaseID ID of the purchase to change.
     */
    confirmPurchase(purchaseID) {
      const params = {purchaseID: purchaseID}
      postAction(this.url.confirmPurchase, params)
        .then(res => {
          if (res.success) {
            this.$refs.table.loadData()
          }
        })
    }
  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>