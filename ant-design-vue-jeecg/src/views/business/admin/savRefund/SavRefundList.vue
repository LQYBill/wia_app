<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline" @keyup.enter.native="searchQuery">
        <a-row :gutter="24">
        </a-row>
      </a-form>
    </div>
    <!-- 查询区域-END -->

    <!-- 操作按钮区域 -->
    <div class="table-operator">
      <a-button @click="handleAdd" type="primary" icon="plus">新增</a-button>
      <a-button type="primary" icon="download" @click="handleExportXls('售后退款')">导出</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
        <a-button type="primary" icon="import">导入</a-button>
      </a-upload>
      <!-- 高级查询区域 -->
      <j-super-query :fieldList="superFieldList" ref="superQueryModal" @handleSuperQuery="handleSuperQuery"></j-super-query>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="batchDel"><a-icon type="delete"/>删除</a-menu-item>
        </a-menu>
        <a-button style="margin-left: 8px"> 批量操作 <a-icon type="down" /></a-button>
      </a-dropdown>
    </div>

    <!-- table区域-begin -->
    <div>
      <div class="ant-alert ant-alert-info" style="margin-bottom: 16px;">
        <i class="anticon anticon-info-circle ant-alert-icon"></i> 已选择 <a style="font-weight: 600">{{ selectedRowKeys.length }}</a>项
        <a style="margin-left: 24px" @click="onClearSelected">清空</a>
      </div>

      <a-table
        ref="table"
        size="middle"
        :scroll="{x:true}"
        bordered
        rowKey="id"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        class="j-table-force-nowrap"
        @change="handleTableChange">

        <template slot="htmlSlot" slot-scope="text">
          <div v-html="text"></div>
        </template>
        <template slot="imgSlot" slot-scope="text">
          <span v-if="!text" style="font-size: 12px;font-style: italic;">无图片</span>
          <img v-else :src="getImgView(text)" height="25px" alt="" style="max-width:80px;font-size: 12px;font-style: italic;"/>
        </template>
        <template slot="fileSlot" slot-scope="text">
          <span v-if="!text" style="font-size: 12px;font-style: italic;">无文件</span>
          <a-button
            v-else
            :ghost="true"
            type="primary"
            icon="download"
            size="small"
            @click="downloadFile(text)">
            下载
          </a-button>
        </template>

        <span slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">编辑</a>

          <a-divider type="vertical" />
          <a-dropdown>
            <a class="ant-dropdown-link">更多 <a-icon type="down" /></a>
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
        </span>

      </a-table>
    </div>

    <sav-refund-modal ref="modalForm" @ok="modalFormOk"></sav-refund-modal>
  </a-card>
</template>

<script>

  import '@/assets/less/TableExpand.less'
  import { mixinDevice } from '@/utils/mixin'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import SavRefundModal from './modules/SavRefundModal'
  import {filterMultiDictText} from '@/components/dict/JDictSelectUtil'

  export default {
    name: 'SavRefundList',
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      SavRefundModal
    },
    data () {
      return {
        description: '售后退款管理页面',
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
            title:'创建人',
            align:"center",
            sorter: true,
            dataIndex: 'createBy'
          },
          {
            title:'创建日期',
            align:"center",
            sorter: true,
            dataIndex: 'createTime'
          },
          {
            title:'店铺',
            align:"center",
            sorter: true,
            dataIndex: 'erpCode'
          },
          {
            title:'平台订单ID',
            align:"center",
            sorter: true,
            dataIndex: 'platformOrderId_dictText'
          },
          {
            title:'平台交易号',
            align:"center",
            sorter: true,
            dataIndex: 'platformOrderNumber'
          },
          {
            title:'采购退款',
            align:"center",
            sorter: true,
            dataIndex: 'purchaseRefund',
            customRender: (text) => (text ? filterMultiDictText(this.dictOptions['purchaseRefund'], text) : ''),
          },
          {
            title:'采购退款金额',
            align:"center",
            dataIndex: 'purchaseRefundAmount'
          },
          {
            title:'运费退款',
            align:"center",
            sorter: true,
            dataIndex: 'shippingRefund',
            customRender: (text) => (text ? filterMultiDictText(this.dictOptions['shippingRefund'], text) : ''),
          },
          {
            title:'挂号费应退款金额',
            align:"center",
            dataIndex: 'fretFee'
          },
          {
            title:'运费应退款金额',
            align:"center",
            dataIndex: 'shippingFee'
          },
          {
            title:'TVA应退款金额',
            align:"center",
            dataIndex: 'vat'
          },
          {
            title:'服务费应退款金额',
            align:"center",
            dataIndex: 'serviceFee'
          },
          {
            title:'退款发票号',
            align:"center",
            sorter: true,
            dataIndex: 'invoiceNumber'
          },
          {
            title:'实际退款总金额',
            align:"center",
            sorter: true,
            dataIndex: 'totalRefundAmount'
          },
          {
            title:'退款日期',
            align:"center",
            sorter: true,
            dataIndex: 'refundDate',
            customRender:function (text) {
              return !text?"":(text.length>10?text.substr(0,10):text)
            }
          },
          {
            title: '操作',
            dataIndex: 'action',
            align:"center",
            fixed:"right",
            width:147,
            scopedSlots: { customRender: 'action' }
          }
        ],
        url: {
          list: "/savRefund/savRefund/list",
          delete: "/savRefund/savRefund/delete",
          deleteBatch: "/savRefund/savRefund/deleteBatch",
          exportXlsUrl: "/savRefund/savRefund/exportXls",
          importExcelUrl: "savRefund/savRefund/importExcel",

        },
        dictOptions:{},
        superFieldList:[],
      }
    },
    created() {
      this.$set(this.dictOptions, 'purchaseRefund', [{text:'是',value:'Y'},{text:'否',value:'N'}])
      this.$set(this.dictOptions, 'shippingRefund', [{text:'是',value:'Y'},{text:'否',value:'N'}])
    this.getSuperFieldList();
    },
    computed: {
      importExcelUrl: function(){
        return `${window._CONFIG['domainURL']}/${this.url.importExcelUrl}`;
      },
    },
    methods: {
      initDictConfig(){
      },
      getSuperFieldList(){
        let fieldList=[];
        fieldList.push({type:'string',value:'createBy',text:'创建人',dictCode:''})
        fieldList.push({type:'datetime',value:'createTime',text:'创建日期'})
        fieldList.push({type:'sel_search',value:'platformOrderId',text:'平台订单ID',dictTable:'platform_order', dictText:'platform_order_id', dictCode:'id'})
        fieldList.push({type:'switch',value:'purchaseRefund',text:'采购退款'})
        fieldList.push({type:'BigDecimal',value:'purchaseRefundAmount',text:'采购退款金额',dictCode:''})
        fieldList.push({type:'switch',value:'shippingRefund',text:'运费退款'})
        fieldList.push({type:'string',value:'invoiceNumber',text:'退款发票号'})
        fieldList.push({type:'date',value:'refundDate',text:'退款日期'})
        this.superFieldList = fieldList
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>