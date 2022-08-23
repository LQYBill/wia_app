<template>
  <a-card :bordered='false'>
    <!-- 查询区域 -->
    <div class='table-page-search-wrapper'>
      <!-- 搜索区域 -->
      <a-form-model layout='inline' :model='form' ref='searchForm' :rules='rules'>
        <a-row :gutter='24'>
          <a-col
            :md='6'
            :sm='8'
          >
            <a-form-model-item
              label='客户'
              :labelCol='{span: 5}'
              :wrapperCol='{span: 18}'
              prop='clientId'
            >
              <a-select
                show-search
                placeholder='输入客户进行搜索'
                option-filter-prop='children'
                :filter-option='customerFilterOption'
                @change='handleClientChange'
                :disabled='clientDisable'
                v-model='form.clientId'
              >
                <a-select-option
                  v-for='(item, index) in customerList'
                  :key='index'
                  :value='index'
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col
            :md='6'
            :sm='8'
          >
            <a-form-model-item
              label='店铺'
              :labelCol='{span: 5}'
              :wrapperCol='{span: 18}'
              prop='shopIDs'
            >
              <a-select
                mode='multiple'
                style='width: 100%'
                placeholder='不选默认所有店铺'
                @change='handleShopChange'
                :allowClear=true
                v-model='shopIDs'
                :disabled='shopDisable'
              >
                <a-select-option
                  v-for='(item, index) in shopList'
                  :value='item.value'
                  :key='index'
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>

          <span
            style='float: left;overflow: hidden;'
            class='table-page-search-submitButtons'
          >
            <a-col
              :md='6'
              :sm='24'
            >
              <a-button
                type='primary'
                htmlType='submit'
                @click='loadOrders'
                :loading='findOrdersLoading'
              >搜索</a-button>
            </a-col>
          </span>

          <span
            style='float: left;overflow: hidden;'
            class='table-page-search-submitButtons'
          >
            <a-col
              :md='6'
              :sm='24'
            >
              <a-button
                type='primary'
                :loading='invoiceLoading'
                @click='makeInvoice'
                :disabled='invoiceDisable'
              >生成物流发票文件</a-button>
            </a-col>
          </span>
          <span
            style='float: left;overflow: hidden;'
            class='table-page-search-submitButtons'
          >
            <a-col
              :md='6'
              :sm='24'
            >
              <a-button
                type='danger'
                :loading='invoiceLoading'
                @click='makeCompleteInvoice'
                :disabled='completeInvoiceDisable'
              >生成完整（物流+采购）发票文件</a-button>
            </a-col>
          </span>
        </a-row>
      </a-form-model>
    </div>

    <!-- table区域 begin -->
    <div>

      <a-alert type='info' showIcon style='margin-bottom: 16px;'>
        <template slot='message'>
          <span>已选择</span>
          <a style='font-weight: 600;padding: 0 4px;'>{{ selectedRowKeys.length }}</a>
          <span>项</span>
          <a style='margin-left: 24px' @click='onClearSelected'>清空</a>
        </template>
      </a-alert>

      <a-table
        ref='table'
        size='middle'
        bordered
        rowKey='id'
        class='j-table-force-nowrap'
        :scroll='{x:true}'
        :loading='orderListLoading'
        :columns='columns'
        :dataSource='orderList'
        :pagination='pagination'
        :expandedRowKeys='expandedRowKeys'
        :rowSelection='{selectedRowKeys, onChange: onSelectChange, getCheckboxProps: getCheckboxProps}'
        @expand='handleExpand'
        @change='handleTableChange'
      >

        <!-- 内嵌table区域 begin -->
        <template slot='expandedRowRender' slot-scope='record'>
          <a-tabs tabPosition='top'>
            <a-tab-pane tab='平台订单内容' key='platformOrderContent' forceRender>
              <platform-order-content-sub-table :record='record' />
            </a-tab-pane>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

        <template slot="erpStatus" slot-scope="record">
          <a-tag
            v-for="erpStatus in record"
            :key="erpStatus"
            :color="erpStatus === '1' ? 'volcano' : 'green'"
          >
            {{ erpStatus === '1' ? '待处理' : '配货中'}}
          </a-tag>
        </template>

        <template slot="productAvailability" slot-scope="record">
          <a-tag
            v-for="productAvailable in record"
            :key="productAvailable"
            :color="productAvailable === '1' ? 'green' : 'volcano'"
          >
            {{productAvailable === '1' ? '有货' : '缺货'}}
          </a-tag>
        </template>

        <template slot='htmlSlot' slot-scope='text'>
          <div v-html='text'></div>
        </template>

      </a-table>
    </div>
    <!-- table区域 end -->
  </a-card>
</template>

<script>
import { getAction, getFile } from '@/api/manage'
import { saveAs } from 'file-saver'
import { postAction } from '@api/manage'
import moment from 'moment'
import PlatformOrderContentSubTable from '../platformOrder/subTables/PlatformOrderContentSubTable'

export default {
  name: 'GetInvoiceFile',
  components: {
    PlatformOrderContentSubTable
  },
  data() {
    return {
      customerList: [],
      client: undefined,
      /**
       *  available shops, array of
       *  {text: shop.erpCode, value: shop.id,}
       */
      shopList: [],
      shopIDs: [],
      customerId: '',
      form: {
        clientId: undefined,
        shopIDs: undefined
      },
      orderList: [],
      expandedRowKeys: [],
      selectedRowKeys: [],
      selectionRows: [],
      rules: {
        clientId: [{ required: true, message: '请选择客户', trigger: 'blur' }]
      },
      url: {
        listOrders: '/shippingInvoice/preShipping/orders',
        getClientList: '/client/client/all',
        getShopsByCustomerId: '/shippingInvoice/shopsByClient',
        makeInvoice: '/shippingInvoice/preShipping/make',
        checkSkuPrices: '/shippingInvoice/preShipping/checkSkuPrices',
        makeCompleteInvoice: '/shippingInvoice/preShipping/makeComplete',
        downloadInvoice: '/shippingInvoice/download',
        invoiceDetail: '/shippingInvoice/invoiceDetail'
      },
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
          sorter: true
        },
        {
          title: '物流渠道',
          align: 'center',
          dataIndex: 'logisticChannelName_dictText',
          sorter: true
        },
        {
          title: '平台订单号码',
          align: 'center',
          dataIndex: 'platformOrderId',
          sorter: true
        },
        {
          title: '平台订单交易号',
          align: 'center',
          dataIndex: 'platformOrderNumber',
          sorter: true
        },
        {
          title: '物流跟踪号',
          align: 'center',
          dataIndex: 'trackingNumber',
          sorter: true
        },
        {
          title: '订单交易时间',
          align: 'center',
          dataIndex: 'orderTime',
          sorter: true
        },
        {
          title: '订单收件人',
          align: 'center',
          dataIndex: 'recipient',
          sorter: true
        },
        {
          title: '订单收件人国家',
          align: 'center',
          dataIndex: 'country',
          sorter: true
        },
        {
          title: 'ERP中状态',
          align: 'center',
          dataIndex: 'erpStatus',
          sorter: true,
          scopedSlots: { customRender : 'erpStatus' }
        },

        {
          title: '是否有货',
          align: "center",
          dataIndex: 'productAvailable',
          sorter: true,
          scopedSlots: { customRender : 'productAvailability' }
        },
      ],
      pagination: {
        current: 1,
        pageSize: 50,
        pageSizeOptions: ['10', '25', '50', '100', '200', '500'],
        showTotal: (total, range) => {
          return range[0] + '-' + range[1] + ' / ' + total
        },
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      /* 排序参数 */
      isorter:{
        column: 'order_time',
        order: 'desc',
      },
      invoiceLoading: false,
      findOrdersLoading: false,
      shopDisable: true,
      clientDisable: false,
      purchasePricesAvailable: true,
      completeInvoiceDisable: true,
      invoiceDisable: true,
      dataDisable: true,
      orderListLoading: false
    }
  },
  created() {
    this.loadClientList()
  },
  computed: {},
  methods: {
    /**
     * Load client list from API
     */
    loadClientList() {
      let self = this
      getAction(this.url.getClientList)
        .then(res => {
          if (res.success) {
            self.customerList = res.result.map(customer => ({
              text: `${customer.firstName} ${customer.surname}`,
              value: customer.id,
              client: customer
            }))
          }
        })
    },
    handleClientChange(index) {
      this.customerId = this.customerList[index].client.id
      this.client = this.customerList[index].client
      this.loadShopList(this.customerId)
        .then(
          () =>
            this.shopDisable = false
        )
      // clear selected shop IDs
      this.shopIDs = []
      this.orderList = []
      this.selectedRowKeys = []
      this.selectionRows = []
      this.pagination.current = 1
      this.pagination.pageSize = 100
      this.pagination.total = 0
      this.invoiceDisable = true
      this.completeInvoiceDisable = true
    },
    handleShopChange(value) {
      // value returned is array of shop
      this.shopIDs = value
      console.log(this.shopIDs)
      if (this.shopIDs.length === 0) {
        this.dataDisable = true
      }
    },
    handleExpand(expanded, record) {
      this.expandedRowKeys = []
      if (expanded === true) {
        this.expandedRowKeys.push(record.id)
      }
    },
    customerFilterOption(input, option) {
      return (
        option.componentOptions.children[0]
          .text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      )
    },
    /**
     * Send a request to load shop list by client ID from API.
     * In case of success of the request, load shop list, if none shop,
     * display a warning message.
     * In case of failure of the request, a message will be displayed.
     *
     * @param clientID the client ID
     * @return Promise for following operation to synchronize
     */
    loadShopList(clientID) {
      let self = this
      const param = { clientID: clientID }
      return getAction(this.url.getShopsByCustomerId, param)
        .then(res => {
          if (res.success) {
            if (res.result.length === 0) {
              self.$message.warning('没有找到当前客户的相关店铺信息')
            }
            self.shopList = res.result.map(
              shop => ({
                text: shop.erpCode,
                value: shop.id
              })
            )
          } else {
            self.$message.warning('Internal server error. Try later.')
          }
        })
    },
    loadOrders() {
      let self = this
      this.$refs.searchForm.validate(
        (valid) => {
          if (valid) {
            let requestParam = {
              clientId: self.customerId,
              shopIds: self.shopIDs,
              pageNo: self.pagination.current,
              pageSize: self.pagination.pageSize
            }
            if (Object.keys(self.isorter).length > 0) {
              requestParam.order = self.isorter.order;
              requestParam.column = self.isorter.column
            }
            this.findOrdersLoading = true
            this.orderListLoading = true
            getAction(self.url.listOrders, requestParam)
              .then(res => {
                self.orderList = res.result.records
                console.log(res.result)

                if (res.result.total) {
                  self.pagination.total = res.result.total
                } else {
                  self.pagination.total = 0
                }
                if (self.orderList.length > 0) {
                  let orderIdList = []
                  self.orderList.map(order => {
                    orderIdList.push(order.id)
                  })
                  let param = {
                    clientID : self.customerId,
                    orderIds : orderIdList
                  }
                  postAction(self.url.checkSkuPrices, param)
                    .then(res => {
                      self.purchasePricesAvailable = res.code === 200;
                      console.log(res.code)
                      if (res.message) {
                        this.$message.warning(res.message)
                      }
                    })
                }
                this.findOrdersLoading = false
                this.orderListLoading = false
              })
          }
        }
      )
    },
    makeInvoice() {
      let self = this
      self.loading = true
      if (!this.customerId) {
        this.$message.warning('请选择客户！')
        return
      }
      let param = {
        clientID: this.customerId,
        orderIds: this.selectedRowKeys
      };
      self.invoiceDisable = true
      self.findOrdersLoading = true
      self.orderListLoading = true
      self.shopDisable = true
      self.clientDisable = true
      postAction(this.url.makeInvoice, param)
        .then(
          res => {
            console.log(res)
            if (!res.success) {
              self.$message.error(res.message, 10)
            } else {
              self.selectionRows = []
              self.selectedRowKeys = []
              let filename = res.result.filename
              let code = res.result.invoiceCode
              this.downloadInvoice(filename).then(
                this.$message.info('Download succeed.')
              )
              this.downloadDetailFile(code)
              this.pagination.current = 1
              this.loadOrders()
            }
            self.clientDisable = false
            self.shopDisable = false
            self.invoiceDisable = false
            self.findOrdersLoading = false
            self.orderListLoading = false
          }
        )
    },
    makeCompleteInvoice() {
      let self = this
      self.loading = true
      if (!this.customerId) {
        this.$message.warning('请选择客户！')
        return
      }
      let param = {
        clientID: this.customerId,
        orderIds: this.selectedRowKeys
      };
      self.invoiceDisable = true
      self.findOrdersLoading = true
      self.orderListLoading = true
      self.shopDisable = true
      self.clientDisable = true
      postAction(this.url.makeCompleteInvoice, param)
        .then(
          res => {
            console.log(res)
            if (!res.success) {
              self.$message.error(res.message, 10)
            } else {
              self.selectionRows = []
              self.selectedRowKeys = []
              let filename = res.result.filename
              let code = res.result.invoiceCode
              this.downloadInvoice(filename).then(
                this.$message.info('Download succeed.')
              )
              this.downloadDetailFile(code)
              this.pagination.current = 1
              this.loadOrders()
            }
            self.clientDisable = false
            self.shopDisable = false
            self.completeInvoiceDisable = false
            self.findOrdersLoading = false
            self.orderListLoading = false
          }
        )
    },
    downloadInvoice(filename) {
      const param = { filename: filename }
      console.log(filename)
      return getFile(this.url.downloadInvoice, param)
        .then(res => {
          console.log(res)
          saveAs(res, filename)
        })
    },
    handleTableChange(pagination, filters, sorter) {
      //分页、排序、筛选变化时触发
      //TODO 筛选
      console.log(pagination)
      if (Object.keys(sorter).length > 0) {
        this.isorter.column = sorter.field
        this.isorter.order = 'ascend' === sorter.order ? 'asc' : 'desc'
      }
      this.pagination = pagination
      this.loadOrders()
    },
    onSelectChange(selectedRowKeys, selectionRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectionRows = selectionRows
      // No selected row, no invoice
      this.invoiceDisable = this.selectionRows.length === 0
      this.completeInvoiceDisable = this.selectionRows.length === 0 || !this.purchasePricesAvailable
    },
    getCheckboxProps: record => ({
      props: {
        disabled: record.logisticChannelName === null && record.invoiceLogisticChannelName === null,
        name: record.logisticChannelName,
      },
    }),
    onClearSelected() {
      this.selectedRowKeys = []
      this.selectionRows = []
      this.invoiceDisable = true
      this.completeInvoiceDisable = true
    },
    downloadDetailFile(invoiceNumber) {
      const param = {
        invoiceNumber: invoiceNumber
      }
      getFile(this.url.invoiceDetail, param).then(
        res => {
          let now = moment().format('yyyyMMDD')
          let name = 'Détail_calcul_de_facture_' + this.client.internalCode + '_' + now + '.xlsx'
          saveAs(res, name)
        })
    }
  }
}
</script>
<style scoped>
@import "~@assets/less/common.less";
</style>
