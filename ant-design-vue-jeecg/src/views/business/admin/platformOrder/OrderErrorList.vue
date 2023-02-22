<template>
  <a-card :bordered="false">
    <div class="table-operator">
      <a-button type="primary" icon="download" @click="handleExportXls('有货未配单订表')">导出</a-button>
      <j-super-query :fieldList="superFieldList" ref="superQueryModal" @handleSuperQuery="handleSuperQuery"></j-super-query>
    </div>
    <a-divider/>
    <!--  data display table  -->
    <div>
      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>已选择</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>项</span>
          <a style="margin-left: 24px" @click="onClearSelected">清空</a>
        </template>
      </a-alert>

      <a-table :columns="columns"
               :data-source="dataSource"
               :pagination="ipagination"
               :rowKey="(record)=>record['id']"
               :loading="loading"
               :rowSelection="{selectedRowKeys, onChange: onSelectChange}"
               bordered
               @change="handleTableChange"
      >

        <template slot='erpStatus' slot-scope='record'>
          <a-tag
            v-for='erpStatus in record'
            :key='erpStatus'
            :color="erpStatus === '1' ? 'volcano' : 'green'"
          >
            {{ erpStatus === '1' ? '待处理' : '配货中' }}
          </a-tag>
        </template>

        <template slot='toReview' slot-scope='record'>
          <a-tag
            v-for='canSend in record'
            :key='canSend'
            :color="canSend === '2' ? 'volcano' : 'green'"
          >
            {{ canSend === '2' ? '异常订单' : '正常订单'}}
          </a-tag>
        </template>

      </a-table>
    </div>
  </a-card>
</template>

<script>
import {getAction, postAction} from "@api/manage";
import Vue from "vue";
import { JeecgListMixin } from '@/mixins/JeecgListMixin'
import PlatformOrderModal from '@views/business/admin/platformOrder/modules/PlatformOrderModal'



export default {
  name: "OrderErrorList",
  mixins: [JeecgListMixin],
  data() {
    return {
      columns: [
        {
          title: '#',
          dataIndex: 'id',
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
          title: '订单交易时间',
          align: 'center',
          dataIndex: 'orderTime',
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
          title: '订单收件人',
          align: 'center',
          dataIndex: 'recipient',
          sorter: true
        },
        {
          title:'待审核订单',
          align:"center",
          dataIndex: 'canSend',
          sorter: true,
          scopedSlots: { customRender: 'toReview' }
        },
        {
          title: 'ERP中状态',
          align: 'center',
          dataIndex: 'erpStatus',
          sorter: true,
          scopedSlots: { customRender: 'erpStatus' }
        }
      ],
      // 字典选项
      dictOptions: {},
      url: {
        list: '/business/platformOrder/errorList',
        exportXlsUrl: '/business/platformOrder/exportErrorXls',
      },
      superFieldList:[]
    }
  },

  created() {
    this.getSuperFieldList();
  },
  methods: {
    getSuperFieldList(){
      let fieldList=[];
      fieldList.push({type:'sel_search',value:'shopId',text:'店铺ID',dictTable:'shop', dictText:'erp_code', dictCode:'id'})
      fieldList.push({type:'string',value:'platformOrderId',text:'平台订单号码',dictCode:''})
      fieldList.push({type:'string',value:'platformOrderNumber',text:'平台订单交易号',dictCode:''})
      fieldList.push({type:'string',value:'erpOrderId',text:'ERP内订单ID',dictCode:''})
      fieldList.push({type:'date',value:'orderTime',text:'订单交易时间'})
      fieldList.push({type:'string',value:'recipient',text:'订单收件人',dictCode:''})
      fieldList.push({type:'string',value:'country',text:'订单收件人国家',dictCode:''})
      fieldList.push({type:'string',value:'postcode',text:'订单收件人邮编',dictCode:''})
      fieldList.push({type: 'BigDecimal', value: 'orderServiceFee', text: '订单服务费', dictCode: ''})
      fieldList.push({type:'string',value:'canSend',text:'待审核订单 1.否 2.是',dictCode:''})
      fieldList.push({type:'string',value:'erpStatus',text:'ERP中状态',dictCode:''})
      fieldList.push({type: 'string', value: 'productAvailable', text: '有货（1=有，0=没有）', dictCode: 'yn'})
      fieldList.push({type:'string',value:'trackingNumber',text:'物流跟踪号',dictCode:''})
      fieldList.push({type:'sel_search',value:'logisticChannelName',text:'物流渠道',dictTable:'logistic_channel', dictText:'zh_name', dictCode:'zh_name'})
      this.superFieldList = fieldList
    }
  }


}
</script>

<style scoped>

</style>