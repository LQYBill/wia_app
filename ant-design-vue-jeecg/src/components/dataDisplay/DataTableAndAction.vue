<template>
  <a-card class="j-inner-table-wrapper" :bordered="false">
    <!-- table区域 begin -->
    <div>
      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>Selected</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>Items</span>
          <a style="margin-left: 24px" @click="onClearSelected">Clear</a>
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
            <slot name="sub-table" :record="record"></slot>
          </a-tabs>
        </template>
        <!-- 内嵌table区域 end -->

        <template
          v-for="slotItem in rowSlotColumns"
          :slot="slotItem.customRender"
          slot-scope="text, record, index"
        >
          <slot
            :name="slotItem.customRender"
            :text="text"
            :record="record"
            :index="index"
          ></slot>
        </template>

      </a-table>
    </div>
    <!-- table区域 end -->

    <slot name="popup" :selectedRowKeys="selectedRowKeys" :selectionRows="selectionRows"></slot>
    <!-- 表单区域 -->
    <a-space class="bottomButtons">
      <a-button type="danger" @click="cancelHandler(selectedRowKeys, selectionRows)">
        {{ cancelText }}
      </a-button>
      <a-button type="primary" @click="okHandler(selectedRowKeys, selectionRows)">
        {{ okText }}
        <a-icon type="right"/>
      </a-button>
    </a-space>
  </a-card>
</template>

<script>
import {JeecgListMixin} from "@/mixins/JeecgListMixin";
import SkuPriceSubTable from "@views/business/client/inventory/subTables/SkuPriceSubTable";
import ShippingDiscountSubTable from "@views/business/client/inventory/subTables/ShippingDiscountSubTable";
import PopupConfirmation from "@views/business/client/inventory/modules/ConfirmationContainer";
import Tabs from 'ant-design-vue'
import {getFileAccessHttpUrl} from "@api/manage";

export default {
  name: "DataTableAndAction",
  mixins: [JeecgListMixin],
  components: {
    SkuPriceSubTable,
    ShippingDiscountSubTable,
    PopupConfirmation,
    Tabs
  },
  props: {
    /* column definition, see a-table / columns */
    columns: Array,
    /* server urls, begin after domaine */
    url: {
      list: String,
      exportXlsUrl: String
    },
    okText: String,
    okHandler: Function,
    cancelText: String,
    cancelHandler: Function,
  },
  data() {
    return {
      description: 'Abstract data table',
      dictOptions: {},
      // key of expanded row
      expandedRowKeys: [],

      superFieldList: [],
    }
  },
  created() {
    this.getSuperFieldList();
    for (const item of this.columns) {
      console.log("col:")
      console.log(item)
      console.log("col slot:")
      console.log(item.scopedSlots)
    }
  },
  computed: {
    importExcelUrl() {
      return window._CONFIG['domainURL'] + this.url.importExcelUrl
    },
    rowSlotColumns() {
      return this.columns
        .filter(item => {
          return item.scopedSlots;
        })
        .map(item => item.scopedSlots);
    },

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
    getSuperFieldList() {
      let fieldList = [];
      fieldList.push({
        type: 'sel_search',
        value: 'productId',
        text: '商品ID',
        dictTable: 'product',
        dictText: 'code',
        dictCode: 'id'
      })
      fieldList.push({type: 'string', value: 'erpCode', text: 'ERP中商品代码', dictCode: ''})
      fieldList.push({type: 'int', value: 'availableAmount', text: '库存数量', dictCode: ''})
      fieldList.push({type: 'int', value: 'purchasingAmount', text: '在途数量', dictCode: ''})
      fieldList.push({type: 'string', value: 'imageSource', text: '图片链接', dictCode: ''})
      this.superFieldList = fieldList
    },
    clearSelected(){
      this.selectedRowKeys = []
      this.selectionRows = []
    }
  },

}
</script>

<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>