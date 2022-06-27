<template>
  <a-card :bordered='false'>
    <div class='table-page-search-wrapper'>
      <a-form layout='inline'>
        <a-row :gutter='24'>
          <a-col
            :md='6'
            :sm='8'
          >
            <a-form-item
              label='客户'
            >
              <a-select
                show-search
                placeholder='输入客户进行搜索'
                option-filter-prop='children'
                :filter-option='customerFilterOption'
                @change='handleClientChange'
                style='width: 240px'
              >
                <a-select-option
                  v-for='(item, index) in customerList'
                  :key='index'
                  :value='index'
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <span
            style='float: left;overflow: hidden;'
            class='table-page-search-submitButtons'
          >
          <a-col
            :md='6'
            :sm='8'
          >
            <a-upload name='file' :showUploadList='false' :multiple='false' :headers='tokenHeader'
                      :action='importInventory' @change='handleInventoryImport' :data='{clientId: customerId}'>
              <a-button type='primary' :disabled='selectedClient === null'>导入采购清单</a-button>
            </a-upload>
          </a-col>
        </span>
        </a-row>
      </a-form>
    </div>

    <a-table
      ref='table'
      size='middle'
      bordered
      rowKey='rowIndex'
      class='j-table-force-nowrap'
      :scroll='{x:true}'
      :loading='loading'
      :columns='columns'
      :dataSource='inventory'
      :pagination='pagination'
      :expandedRowKeys='expandedRowKeys'
      :rowSelection='{selectedRowKeys, onChange: onSelectChange}'
      @expand='handleExpand'
    >

      <!-- 内嵌table区域 begin -->
      <template slot='expandedRowRender' slot-scope='record'>
        <a-tabs tabPosition='top'>
          <a-tab-pane tab='SKU价格表' key='skuPrice' forceRender>
            <sku-price-sub-table :record='record' />
          </a-tab-pane>
          <a-tab-pane tab='SKU物流折扣' key='shippingDiscount' forceRender>
            <shipping-discount-sub-table :record='record' />
          </a-tab-pane>
        </a-tabs>
      </template>
      <!-- 内嵌table区域 end -->

      <template slot='imgSlot' slot-scope='text'>
        <div style='font-size: 12px;font-style: italic;'>
          <span v-if='!text'>No picture available</span>
          <img v-else
               :src='getImgView(text)'
               :preview='getImgView(text)'
               alt='SKU picture'
               style='min-width:50px;max-width:80px;height:50px;'
          />
        </div>
      </template>

      <template slot='salesQuantitySlot' slot-scope='text, record, index'>
        <a-tooltip title='Sales from last 7 days'>
          {{ dealNull(record['sales7']) }}
        </a-tooltip>
        |
        <a-tooltip title='Sales from last 14 days'>
          {{ dealNull(record['sales14']) }}
        </a-tooltip>
        |
        <a-tooltip title='Sales from last 28 days'>
          {{ dealNull(record['sales28']) }}
        </a-tooltip>
      </template>

    </a-table>
  </a-card>
</template>

<script>
import { getAction, getFile, getFileAccessHttpUrl } from '@/api/manage'
import { saveAs } from 'file-saver'
import { postAction } from '@api/manage'
import SkuPriceSubTable from '../sku/subTables/SkuPriceSubTable'
import ShippingDiscountSubTable from '../sku/subTables/ShippingDiscountSubTable'
import '@/assets/less/TableExpand.less'
import { ACCESS_TOKEN, TENANT_ID } from '@/store/mutation-types'
import Vue from 'vue'
import { Modal } from 'ant-design-vue'

export default {
  name: 'InventoryPurchase',
  components: {
    SkuPriceSubTable,
    ShippingDiscountSubTable
  },
  data() {
    return {
      description: '按SKU采购页面',
      customerList: [],
      inventory: [],
      customerId: '',
      selectedClient: null,
      loading: false,
      // 表头
      columns: [
        {
          title: '#',
          dataIndex: '',
          key: 'rowIndex',
          width: 60,
          align: 'center',
          customRender: function(t, r, index) {
            return parseInt(index) + 1
          }
        },
        {
          title: '图片',
          align: 'center',
          dataIndex: 'imageSource',
          scopedSlots: { customRender: 'imgSlot' }
        },
        {
          title: 'SKU ERP code',
          align: 'center',
          sorter: true,
          dataIndex: 'erpCode'
        },
        {
          title: '英文名',
          align: 'center',
          dataIndex: 'nameEn'
        },
        {
          title: '中文名',
          align: 'center',
          dataIndex: 'nameZh'
        },
        {
          title: '购买数量',
          align: 'center',
          dataIndex: 'quantity'
        }
      ],
      pagination: {
        current: 1,
        pageSize: 25,
        pageSizeOptions: ['10', '25', '50', '100'],
        showTotal: (total, range) => {
          return range[0] + '-' + range[1] + ' / ' + total
        },
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      // 展开的行
      expandedRowKeys: [],
      // 选择的行
      selectedRowKeys: [],
      url: {
        loadInventory: '/business/purchaseOrder/admin/loadInventory',
        getClientList: '/client/client/all',
        importInventory: 'business/purchaseOrder/admin/importInventory',
        makeInvoice: '/business/purchaseOrder/admin/makeInvoice',
        downloadInvoice: '/business/purchaseOrder/admin/downloadInvoice'
      },
      dictOptions: {},
      superFieldList: []
    }
  },
  created() {
    this.loadClientList()
  },
  computed: {
    importInventory: function() {
      return `${window._CONFIG['domainURL']}/${this.url.importInventory}`
    },
    tokenHeader() {
      let head = { 'X-Access-Token': Vue.ls.get(ACCESS_TOKEN) }
      let tenantid = Vue.ls.get(TENANT_ID)
      if (tenantid) {
        head['tenant-id'] = tenantid
      }
      return head
    }
  },
  methods: {
    handleExpand(expanded, record) {
      this.expandedRowKeys = []
      if (expanded === true) {
        console.log(expanded)
        this.expandedRowKeys.push(record.skuId)
        console.log(record.skuId)
      }
    },
    /**
     * Load client list from API
     */
    loadClientList() {
      let self = this
      getAction(this.url.getClientList)
        .then(res => {
          if (res.success) {
            console.log(res)
            self.customerList = res.result.map(customer => ({
              text: `${customer.firstName} ${customer.surname}`,
              value: customer.id,
              client: customer
            }))
          }
        })
    },
    handleClientChange(index) {
      this.loading = true;
      console.log(`selected ${index}`)
      this.customerId = this.customerList[index].client.id
      this.selectedClient = this.customerList[index].client
      this.$message.warning("客户选择成功");
      this.loadInventoryForCurrentClient();
    },
    onSelectChange(selectedRowKeys, selectionRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectionRows = selectionRows
      // No selected row, no invoice
      this.invoiceDisable = this.selectionRows.length === 0
    },
    customerFilterOption(input, option) {
      return (
        option.componentOptions.children[0]
          .text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      )
    },
    loadInventoryForCurrentClient() {
      let self = this
      getAction(this.url.loadInventory, {id: this.customerId})
        .then(res => {
          self.loading = false;
          if (res.success) {
            console.log(res)
            self.inventory = res.result.voPurchaseDetails;
          } else {
            self.inventory = []
          }
        })
    },
    handleInventoryImport(info){
      if (info.file.status !== 'uploading') {
        console.log(info.file, info.fileList);
      }
      if (info.file.status === 'done') {
        if (info.file.response.success) {
          this.$message.success(info.file.response.message || `${info.file.name} 文件上传成功`);
          this.loadInventoryForCurrentClient();
        } else {
          this.$message.error(`${info.file.name} ${info.file.response.message}.`);
        }
      } else if (info.file.status === 'error') {
        if (info.file.response.status === 500) {
          let data = info.file.response
          const token = Vue.ls.get(ACCESS_TOKEN)
          if (token && data.message.includes("Token失效")) {
            Modal.error({
              title: '登录已过期',
              content: '很抱歉，登录已过期，请重新登录',
              okText: '重新登录',
              mask: false,
              onOk: () => {
                store.dispatch('Logout').then(() => {
                  Vue.ls.remove(ACCESS_TOKEN)
                  window.location.reload();
                })
              }
            })
          }
        } else {
          this.$message.error(`文件上传失败: ${info.file.msg} `);
        }
      }
    },
    /* 图片预览 */
    getImgView(text){
      if(text && text.indexOf(",")>0){
        text = text.substring(0,text.indexOf(","))
      }
      return getFileAccessHttpUrl(text)
    },
  }
}
</script>
<style scoped>
@import '~@assets/less/common.less';
</style>