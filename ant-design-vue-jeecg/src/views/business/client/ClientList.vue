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
      <a-button type="primary" icon="plus" @click="handleAdd">{{ $t("add") }}</a-button>
      <a-button type="primary" icon="download" @click="handleExportXls('客户')">{{$t("export")}}</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
        <a-button type="primary" icon="import">{{$t('import')}}</a-button>
      </a-upload>
      <!-- 高级查询区域 -->
      <j-super-query :fieldList="superFieldList" ref="superQueryModal" @handleSuperQuery="handleSuperQuery"></j-super-query>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="batchDel">
            <a-icon type="delete"/>
            <span>{{$t("delete")}}</span>
          </a-menu-item>
        </a-menu>
        <a-button>
          <span>{{$t("batchOperation")}}</span>
          <a-icon type="down"/>
        </a-button>
      </a-dropdown>
    </div>
    <!-- 操作按钮区域 end -->

    <!-- table区域 begin -->
    <div>

      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>{{$t("selected")}}</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>{{$t("items")}}</span>
          <a style="margin-left: 24px" @click="onClearSelected">{{$t("clear")}}</a>
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
            <a-tab-pane tab="店铺" key="shop" forceRender>
              <shop-sub-table :record="record"/>
            </a-tab-pane>
            <a-tab-pane tab="客户名下SKU" key="clientSku" forceRender>
              <client-sku-sub-table :record="record"/>
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
            <span>{{$t("download")}}</span>
          </a-button>
        </template>

        <template slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">{{$t("edit")}}</a>
          <a-divider type="vertical"/>
          <a-dropdown>
            <a class="ant-dropdown-link">
              <span>{{$t("more")}} <a-icon type="down"/></span>
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-popconfirm :title="$t('deleteConfirmation')" @confirm="handleDelete(record.id)">
                  <a>{{$t("delete")}}</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

        </template>

      </a-table>
    </div>
    <!-- table区域 end -->

    <!-- 表单区域 -->
    <client-modal ref="modalForm" @ok="modalFormOk"/>

  </a-card>
</template>

<script>

  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import ClientModal from './modules/ClientModal'
  import ShopSubTable from './subTables/ShopSubTable'
  import ClientSkuSubTable from './subTables/ClientSkuSubTable'
  import '@/assets/less/TableExpand.less'

  export default {
    name: 'ClientList',
    mixins: [JeecgListMixin],
    components: {
      ClientModal,
      ShopSubTable,
      ClientSkuSubTable,
    },
    data() {
      return {
        description: '客户列表管理页面',
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
            title: '姓',
            align: 'center',
            dataIndex: 'surname',
          },
          {
            title: '名',
            align: 'center',
            dataIndex: 'firstName',
          },
          {
            title: '简称',
            align: 'center',
            dataIndex: 'internalCode',
          },
          {
            title: '发票实体',
            align: 'center',
            dataIndex: 'invoiceEntity',
          },
          {
            title: '邮箱',
            align: 'center',
            dataIndex: 'email',
          },
          {
            title: '电话',
            align: 'center',
            dataIndex: 'phone',
          },
          {
            title: '门牌号码',
            align: 'center',
            dataIndex: 'streetNumber',
          },
          {
            title: '街道名',
            align: 'center',
            dataIndex: 'streetName',
          },
          {
            title: '备用地址',
            align: 'center',
            dataIndex: 'additionalAddress',
          },
          {
            title: '邮编',
            align: 'center',
            dataIndex: 'postcode',
          },
          {
            title: '城市',
            align: 'center',
            dataIndex: 'city',
          },
          {
            title: '国家',
            align: 'center',
            dataIndex: 'country',
          },
          {
            title: '货币',
            align: 'center',
            dataIndex: 'currency',
          },
          {
            title: '运费折扣',
            align: 'center',
            dataIndex: 'shippingDiscount',
          },
          {
            title: '公司识别码类型',
            align: 'center',
            dataIndex: 'companyIdType',
          },
          {
            title: '公司识别码数值',
            align: 'center',
            dataIndex: 'companyIdValue',
          },
          {
            title: '账户余额',
            align: 'center',
            dataIndex: 'balance',
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
          list: '/client/client/list',
          delete: '/client/client/delete',
          deleteBatch: '/client/client/deleteBatch',
          exportXlsUrl: '/client/client/exportXls',
          importExcelUrl: '/client/client/importExcel',
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
        fieldList.push({type:'string',value:'surname',text:'姓',dictCode:''})
        fieldList.push({type:'string',value:'firstName',text:'名',dictCode:''})
        fieldList.push({type:'string',value:'internalCode',text:'简称',dictCode:''})
        fieldList.push({type:'string',value:'invoiceEntity',text:'发票实体',dictCode:''})
        fieldList.push({type:'string',value:'email',text:'邮箱',dictCode:''})
        fieldList.push({type:'string',value:'phone',text:'电话',dictCode:''})
        fieldList.push({type:'string',value:'streetNumber',text:'门牌号码',dictCode:''})
        fieldList.push({type:'string',value:'streetName',text:'街道名',dictCode:''})
        fieldList.push({type:'string',value:'additionalAddress',text:'备用地址',dictCode:''})
        fieldList.push({type:'string',value:'postcode',text:'邮编',dictCode:''})
        fieldList.push({type:'string',value:'city',text:'城市',dictCode:''})
        fieldList.push({type:'string',value:'country',text:'国家',dictCode:''})
        fieldList.push({type:'string',value:'currency',text:'货币',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'shippingDiscount',text:'运费折扣',dictCode:''})
        fieldList.push({type:'string',value:'companyIdType',text:'公司识别码类型',dictCode:''})
        fieldList.push({type:'string',value:'companyIdValue',text:'公司识别码数值',dictCode:''})
        fieldList.push({type:'BigDecimal',value:'balance',text:'账户余额',dictCode:''})
        this.superFieldList = fieldList
      }
    }
  }
</script>
<style lang="less" scoped>
  @import '~@assets/less/common.less';
</style>