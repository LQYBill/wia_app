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
      <a-button type="primary" icon="download" @click="handleExportXls('物流发票')">导出</a-button>
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
      <!-- button to download invoice of selected rows -->
      <a-button v-if="selectedRowKeys.length > 0"
        type='dashed'
        icon="download"
        @click='downloadInvoiceExcel("invoice")'
        :disabled='downloadCompleteInvoiceExcelVisible'
      >download invoice</a-button>
      <!-- button to download invoice details of selected rows -->
      <a-button v-if="selectedRowKeys.length > 0"
        type='dashed'
        icon="download"
        @click='downloadInvoiceExcel("detail")'
        :disabled='downloadCompleteInvoiceExcelVisible'
      >download details</a-button>
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
        bordered
        rowKey="id"
        class="j-table-force-nowrap"
        :scroll="{x:true}"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
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

    <shipping-invoice-modal ref="modalForm" @ok="modalFormOk"/>
  </a-card>
</template>

<script>

  import { JeecgListMixin } from '@/mixins/JeecgListMixin';
  import ShippingInvoiceModal from './modules/ShippingInvoiceModal';
  import '@assets/less/TableExpand.less';
  import { getAction, getFile } from '@api/manage'
  import {saveAs} from 'file-saver';
  import moment from 'moment/moment'

  export default {
    name: "ShippingInvoiceList",
    mixins:[JeecgListMixin],
    components: {
      ShippingInvoiceModal
    },
    data () {
      return {
        description: '物流发票管理页面',
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
            title:'发票号码',
            align:"center",
            sorter: true,
            dataIndex: 'invoiceNumber'
          },
          {
            title:'应付金额',
            align:"center",
            dataIndex: 'totalAmount'
          },
          {
            title:'减免金额',
            align:"center",
            dataIndex: 'discountAmount'
          },
          {
            title:'最终金额',
            align:"center",
            dataIndex: 'finalAmount'
          },
          {
            title:'已付金额',
            align:"center",
            dataIndex: 'paidAmount'
          },
          {
            title: '操作',
            dataIndex: 'action',
            align:"center",
            fixed:"right",
            width:147,
            scopedSlots: { customRender: 'action' },
          }
        ],
        url: {
          list: "/generated/shippingInvoice/list",
          delete: "/generated/shippingInvoice/delete",
          deleteBatch: "/generated/shippingInvoice/deleteBatch",
          exportXlsUrl: "/generated/shippingInvoice/exportXls",
          importExcelUrl: "generated/shippingInvoice/importExcel",
          downloadCompleteInvoiceExcel: "/generated/shippingInvoice/downloadCompleteInvoiceExcel",
          getClient: "/generated/shippingInvoice/getClient"
        },
        dictOptions:{},
        superFieldList:[],
        failedExcelList:[],
        failedOwnerList:[],
        errorMessage: "",
        downloadCompleteInvoiceExcelVisible : false
      }
    },
    created() {
      this.getSuperFieldList();
    },
    computed: {
      importExcelUrl: function(){
        return `${window._CONFIG['domainURL']}/${this.url.importExcelUrl}`;
      }
    },
    methods: {
      initDictConfig(){
      },
      getSuperFieldList(){
        let fieldList=[];
         fieldList.push({type:'string',value:'createBy',text:'创建人',dictCode:''})
         fieldList.push({type:'datetime',value:'createTime',text:'创建日期'})
         fieldList.push({type:'string',value:'invoiceNumber',text:'发票号码',dictCode:''})
         fieldList.push({type:'BigDecimal',value:'totalAmount',text:'应付金额',dictCode:''})
         fieldList.push({type:'BigDecimal',value:'discountAmount',text:'减免金额',dictCode:''})
         fieldList.push({type:'BigDecimal',value:'finalAmount',text:'最终金额',dictCode:''})
         fieldList.push({type:'BigDecimal',value:'paidAmount',text:'已付金额',dictCode:''})
        this.superFieldList = fieldList
      },
      downloadInvoiceExcel(e) {
        if (this.selectedRowKeys === 0) {
          this.downloadCompleteInvoiceExcelVisible = true;
        }
        console.log("BUTTON VALUE : " + e);
        for (let i in this.selectionRows) {
          let invoice_number = this.selectionRows[i].invoiceNumber;
          console.log("invoice number " + i + ": " + invoice_number);
          const param = {
            invoiceNumber: invoice_number,
            filetype: e
          }
          getAction(this.url.getClient, param)
            .then(res => {
              if (res.success) {
                let client = res.result;
                console.log("Invoice entity : " + client.invoiceEntity);
                getFile(this.url.downloadCompleteInvoiceExcel, param)
                  //téléchargement du fichier
                  .then(res => {
                    console.log("Excel res :" + res);
                    let filename = "";
                    if(e === "invoice") {
                      filename = "Invoice N°" + invoice_number + " (" + client.invoiceEntity + ").xlsx";
                    }
                    else {
                      let now = moment().format("yyyyMMDD")
                      filename = client.internalCode + "_" + invoice_number + '_Détail_calcul_de_facture_' + now + '.xlsx';
                    }
                    saveAs(res, filename);
                  })
                  .catch((error) => {
                    console.log(error);
                    this.errorMessage = error;
                    this.failedExcelList.push(invoice_number);
                    console.log("failed excel list : " + this.failedExcelList);
                    this.$message.warning(error + " : " + invoice_number);
                  });
              } //endif
              else {
                console.log("Failed to find the shop owner ! Check if invoice is valid : " + invoice_number);
                this.$message.warning("Failed to find the shop owner ! Check if invoice is valid : " + invoice_number);
                this.failedOwnerList.push(invoice_number);
              }
            });
        } //end for
      },// end downloadInvoiceExcel
      failedDownloadRequestTreatment() {
        console.log("failed list length : " + this.failedExcelList.length);
        if(this.failedOwnerList.length === 0 && this.failedExcelList.length === 0) {
          this.$message.success("All invoices downloaded successfully !");
        }
        //there were some errors
        else {
          console.log("There was some failures : ");
          if (this.failedExcelList.length > 0) {
            let errMessage = this.errorMessage;
            console.log("Failed to find file for : " + this.failedExcelList);
            errMessage += (" : [" + this.failedExcelList + "]");
            this.$message.error(errMessage);
          }
          if (this.failedOwnerList.length > 0) {
            let errMessage = "Failed to fetch the shop owner for invoices : ";
            console.log("failed to fetch owner for : " + this.failedOwnerList);
            errMessage+=("[" + this.failedOwnerList + "]");
            this.$message.error(errMessage);
          }
        }

      }
    }// end methods
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>