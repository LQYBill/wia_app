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
      <a-button type="primary" icon="download" @click="handleExportXls('平台订单表')">导出</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
        <a-button type="primary" icon="import">导入</a-button>
      </a-upload>
      <!-- 高级查询区域 -->
      <j-super-query :fieldList="superFieldList" ref="superQueryModal" @handleSuperQuery="handleSuperQuery"></j-super-query>
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
            <a-tab-pane tab="平台订单内容" key="platformOrderContent" forceRender>
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
                <a @click="handleDetail(record)">详情</a>
              </a-menu-item>
              <a-menu-item>
                <a-popconfirm title="确定删除吗?" @confirm="() => handleDelete(record.id)">
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

  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import PlatformOrderModal from './modules/PlatformOrderModal'
  import PlatformOrderContentSubTable from './subTables/PlatformOrderContentSubTable'
  import '@/assets/less/TableExpand.less'

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
            dataIndex: '',
            key:'rowIndex',
            width:60,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
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
            title: 'ERP内订单ID',
            align: 'center',
            dataIndex: 'erpOrderId',
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
            title: '订单发货时间',
            align: 'center',
            dataIndex: 'shippingTime',
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
            title: '订单收件人邮编',
            align: 'center',
            dataIndex: 'postcode',
            sorter: true
          },
          {
            title: '物流挂号费',
            align: 'center',
            dataIndex: 'fretFee',
            sorter: true
          },
          {
            title: '订单服务费',
            align: "center",
            dataIndex: 'orderServiceFee',
            sorter: true
          },
          {
            title: '物流发票号',
            align: 'center',
            dataIndex: 'shippingInvoiceNumber_dictText',
            sorter: true
          },
          {
            title: '采购状态',
            align: 'center',
            dataIndex: 'status',
          },
          {
            title: '合并订单目标订单ID',
            align: 'center',
            dataIndex: 'target',
          },
          {
            title: 'ERP中状态',
            align: 'center',
            dataIndex: 'erpStatus',
            sorter: true
          },
          {
            title:'开票物流渠道名称',
            align:"center",
            dataIndex: 'invoiceLogisticChannelName',
            sorter: true
          },
          {
            title: '物流内部单号',
            align: "center",
            dataIndex: 'internalTrackingNumber',
            sorter: true
          },
          {
            title: '有货（1=有，0=没有）',
            align: "center",
            dataIndex: 'productAvailable',
            sorter: true
          },
          {
            title:'可以同步Shopify（1=可以，0=不可以）',
            align:"center",
            dataIndex: 'readyForShopifySync_dictText',
            sorter: true
          },
          {
            title:'待审核订单 1.否 2.是',
            align:"center",
            dataIndex: 'canSend',
            sorter: true
          },
          {
            title: '操作',
            dataIndex: 'action',
            align: 'center',
            width:147,
            scopedSlots: { customRender: 'action' },
          },
        ],
        // 字典选项
        dictOptions: {},
        // 展开的行test
        expandedRowKeys: [],
        url: {
          list: '/business/platformOrder/list',
          delete: '/business/platformOrder/delete',
          deleteBatch: '/business/platformOrder/deleteBatch',
          exportXlsUrl: '/business/platformOrder/exportXls',
          importExcelUrl: '/business/platformOrder/importExcel',
        },
        superFieldList:[],
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
      getSuperFieldList(){
        let fieldList=[];
        fieldList.push({type:'sel_search',value:'shopId',text:'店铺ID',dictTable:'shop', dictText:'erp_code', dictCode:'id'})
        fieldList.push({type:'sel_search',value:'logisticChannelName',text:'物流渠道',dictTable:'logistic_channel', dictText:'zh_name', dictCode:'zh_name'})
        fieldList.push({type:'string',value:'platformOrderId',text:'平台订单号码',dictCode:''})
        fieldList.push({type:'string',value:'platformOrderNumber',text:'平台订单交易号',dictCode:''})
        fieldList.push({type:'string',value:'erpOrderId',text:'ERP内订单ID',dictCode:''})
        fieldList.push({type:'string',value:'trackingNumber',text:'物流跟踪号',dictCode:''})
        fieldList.push({type:'date',value:'orderTime',text:'订单交易时间'})
        fieldList.push({type:'date',value:'shippingTime',text:'订单发货时间'})
        fieldList.push({type:'string',value:'recipient',text:'订单收件人',dictCode:''})
        fieldList.push({type:'string',value:'country',text:'订单收件人国家',dictCode:''})
        fieldList.push({type:'string',value:'postcode',text:'订单收件人邮编',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'fretFee',text:'物流挂号费',dictCode:''})
        fieldList.push({type: 'BigDecimal', value: 'orderServiceFee', text: '订单服务费', dictCode: ''})
        fieldList.push({type:'sel_search',value:'shippingInvoiceNumber',text:'物流发票号',dictTable:'shipping_invoice', dictText:'invoice_number', dictCode:'id'})
        fieldList.push({type:'string',value:'status',text:'采购状态',dictCode:''})
        fieldList.push({type:'string',value:'target',text:'合并订单目标订单ID',dictCode:''})
        fieldList.push({type:'string',value:'erpStatus',text:'ERP中状态',dictCode:''})
        fieldList.push({type: 'string', value: 'internalTrackingNumber', text: '物流内部单号', dictCode: ''})
        fieldList.push({type: 'string', value: 'productAvailable', text: '有货（1=有，0=没有）', dictCode: 'yn'})
        fieldList.push({type:'string',value:'readyForShopifySync',text:'可以同步Shopify（1=可以，0=不可以）',dictCode:'yn'})
        fieldList.push({type:'string',value:'canSend',text:'待审核订单 1.否 2.是',dictCode:''})
        this.superFieldList = fieldList
      }
    }
  }
</script>
<style lang="less" scoped>
  @import '~@assets/less/common.less';
</style>