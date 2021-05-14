<template>
  <a-card class="j-inner-table-wrapper" :bordered="false">
    <!-- table section begin -->
    <div>
      <!-- multiple selection hint -->
      <a-alert type="info" showIcon style="margin-bottom: 16px;">
        <template slot="message">
          <span>Selected</span>
          <a style="font-weight: 600;padding: 0 4px;">{{ selectedRowKeys.length }}</a>
          <span>Items</span>
          <a style="margin-left: 24px" @click="onClearSelected">Clear</a>
        </template>
      </a-alert>

      <!-- table body -->
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
        <!-- nested sub table section begin -->
        <template slot="expandedRowRender" slot-scope="record">
          <a-tabs tabPosition="top">
            <!-- slot declaration for external component,
             slot scope param:
              {
                record: current line
              },
              html tag should be: a-tab-pane
            -->
            <slot name="sub-table" :record="record"></slot>
          </a-tabs>
        </template>
        <!-- nested table section end -->

        <!-- slot for customer cell render -->
        <template
          v-for="slotItem in rowSlotColumns"
          :slot="slotItem.customRender"
          slot-scope="text, record, index"
        >
          <!--
          Declaration of slot for external component.
           slot scope param:
            object{
              text: current value,
              record: current line,
              index: current line index
            }
           -->
          <slot
            :name="slotItem.customRender"
            :text="text"
            :record="record"
            :index="index"
          ></slot>
        </template>
      </a-table>
    </div>
    <!-- table section end -->

    <!-- bottom buttons-->
    <a-space class="bottomButtons">
      <a-button
        v-if="currentUser.cancelText"
        type="danger"
        @click="cancelHandler(selectedRowKeys, selectionRows)">
        {{ currentUser.cancelText }}
      </a-button>
      <a-button
        v-if="currentUser.okText"
        type="primary"
        @click="okHandler(selectedRowKeys, selectionRows)">
        {{ currentUser.okText }}
        <a-icon type="right"/>
      </a-button>
    </a-space>
  </a-card>
</template>

<script>
import {JeecgListMixin} from "@/mixins/JeecgListMixin";
import Tabs from 'ant-design-vue'
import {getAction} from "@api/manage";

export default {
  name: "DataTableAndAction",
  mixins: [JeecgListMixin],
  components: {
    Tabs
  },
  props: {
    /* url for data source, after the domaine, return type: Page */
    dataSourceUrl: {
      type: String,
      required: true
    },
    userConfig: {
      type: Object,
      required: true
    },
  },
  data() {
    return {
      description: 'Abstract data table',
      /* table column definition, same as (ant-vue-design/a-table/columns) */
      columns: [],
      dictOptions: {},
      // key of expanded row
      expandedRowKeys: [],
      url: {
        list: this.dataSourceUrl
      },
      superFieldList: [],
      buttonDisplay: true,
      currentUser:{}
    }
  },
  created() {
    this.getSuperFieldList();
    this.loadRoleConfig();
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
    loadRoleConfig() {
      getAction("/sys/api/queryLoginUserRole", undefined)
        .then(res => {
          let code = String(res)
          this.currentUser = this.userConfig[code]
          if (!this.currentUser) {
            this.$message.error("An accident occurred, you shouldn't see this page.")
            this.columns = []
            this.dataSource = []
            return
          }
          this.columns = this.currentUser.columns
          this.buttonDisplay = this.currentUser.buttonDisplay
        })
    },
    okHandler(keys, records) {
      if (this.currentUser.okHandler) {
        this.currentUser.okHandler(keys, records, this)
      }
    },
    cancelHandler(keys, records) {
      if (this.currentUser.cancelHandler) {
        this.currentUser.cancelHandler(keys, records, this)
      }
    },
    clearSelected() {
      this.selectedRowKeys = []
      this.selectionRows = []
    },
  },

}
</script>

<style lang="less" scoped>
@import '~@assets/less/common.less';
</style>