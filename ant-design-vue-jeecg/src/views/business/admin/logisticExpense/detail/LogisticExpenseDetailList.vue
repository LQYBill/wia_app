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
      <a-button type="primary" icon="download" @click="handleExportXls('物流开销明细')">导出</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
        <a-button type="primary" icon="import">导入</a-button>
      </a-upload>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="import4px" @change="handleImportExcel">
        <a-button type="primary"><img src="~@/assets/4px.svg" class="logo" alt="4px" width='30px'>导入递四方账单明细</a-button>
      </a-upload>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importYunExpress" @change="handleImportExcel">
        <a-button type="primary"><img src="~@/assets/yunexpress.svg" class="logo" alt="yt" width='30px'>导入云途账单明细</a-button>
      </a-upload>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importMs" @change="handleImportExcel">
        <a-button type="primary"><img src="~@/assets/miaoshen.png" class="logo" alt="ms" width='30px'>导入淼深账单明细</a-button>
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

    <logistic-expense-detail-modal ref="modalForm" @ok="modalFormOk"></logistic-expense-detail-modal>
  </a-card>
</template>

<script>

  import '@/assets/less/TableExpand.less'
  import { mixinDevice } from '@/utils/mixin'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import LogisticExpenseDetailModal from './modules/LogisticExpenseDetailModal'

  export default {
    name: 'LogisticExpenseDetailList',
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      LogisticExpenseDetailModal
    },
    data () {
      return {
        description: '物流开销明细管理页面',
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
            title:'平台订单序列号（客户单号）',
            align:"center",
            dataIndex: 'platformOrderSerialId'
          },
          {
            title:'虚拟单号',
            align:"center",
            dataIndex: 'virtualTrackingNumber'
          },
          {
            title:'物流商内部单号',
            align:"center",
            dataIndex: 'logisticInternalNumber'
          },
          {
            title:'物流单号（服务商单号）',
            align:"center",
            dataIndex: 'trackingNumber'
          },
          {
            title:'实际重量',
            align:"center",
            dataIndex: 'realWeight'
          },
          {
            title:'体积重量',
            align:"center",
            dataIndex: 'volumetricWeight'
          },
          {
            title:'计费重量',
            align:"center",
            dataIndex: 'chargingWeight'
          },
          {
            title:'优惠金额',
            align:"center",
            dataIndex: 'discount'
          },
          {
            title:'运费金额',
            align:"center",
            dataIndex: 'shippingFee'
          },
          {
            title:'燃油附加费',
            align:"center",
            dataIndex: 'fuelSurcharge'
          },
          {
            title:'挂号费',
            align:"center",
            dataIndex: 'registrationFee'
          },
          {
            title:'重派费',
            align:"center",
            dataIndex: 'secondDeliveryFee'
          },
          {
            title:'增值税',
            align:"center",
            dataIndex: 'vat'
          },
          {
            title:'增值税服务费',
            align:"center",
            dataIndex: 'vatServiceFee'
          },
          {
            title:'附加费用',
            align:"center",
            dataIndex: 'additionalFee'
          },
          {
            title:'总费用',
            align:"center",
            dataIndex: 'totalFee'
          },
          {
            title:'物流公司ID',
            align:"center",
            dataIndex: 'logisticCompanyId_dictText'
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
          list: "/business/logisticExpenseDetail/list",
          delete: "/business/logisticExpenseDetail/delete",
          deleteBatch: "/business/logisticExpenseDetail/deleteBatch",
          exportXlsUrl: "/business/logisticExpenseDetail/exportXls",
          importExcelUrl: "business/logisticExpenseDetail/importExcel",
          import4px: "business/logisticExpenseDetail/import4px",
          importYunExpress: "business/logisticExpenseDetail/importYunExpress",
          importMs: "business/logisticExpenseDetail/importMs",
        },
        dictOptions:{},
        superFieldList:[],
      }
    },
    created() {
    this.getSuperFieldList();
    },
    computed: {
      importExcelUrl: function(){
        return `${window._CONFIG['domainURL']}/${this.url.importExcelUrl}`;
      },
      import4px: function(){
        return `${window._CONFIG['domainURL']}/${this.url.import4px}`;
      },
      importYunExpress: function(){
        return `${window._CONFIG['domainURL']}/${this.url.importYunExpress}`;
      },
      importMs: function(){
        return `${window._CONFIG['domainURL']}/${this.url.importMs}`;
      },
    },
    methods: {
      initDictConfig(){
      },
      getSuperFieldList(){
        let fieldList=[];
        fieldList.push({type:'string',value:'platformOrderSerialId',text:'平台订单序列号（客户单号）',dictCode:''})
        fieldList.push({type:'string',value:'virtualTrackingNumber',text:'虚拟单号',dictCode:''})
        fieldList.push({type:'string',value:'logisticInternalNumber',text:'物流商内部单号',dictCode:''})
        fieldList.push({type:'string',value:'trackingNumber',text:'物流单号（服务商单号）',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'realWeight',text:'实际重量',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'volumetricWeight',text:'体积重量',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'chargingWeight',text:'计费重量',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'discount',text:'优惠金额',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'shippingFee',text:'运费金额',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'fuelSurcharge',text:'燃油附加费',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'registrationFee',text:'挂号费',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'secondDeliveryFee',text:'重派费',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'vat',text:'增值税',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'vatServiceFee',text:'增值税服务费',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'additionnalFee',text:'附加费用',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'totalFee',text:'总费用',dictCode:''})
        fieldList.push({type:'sel_search',value:'logisticCompanyId',text:'物流公司ID',dictTable:'logistic_company', dictText:'name', dictCode:'id'})
        this.superFieldList = fieldList
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>