<template>
  <a-card :bordered="false">
    <a-table
      rowKey="rowIndex"
      bordered
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="ipagination"
    >
    </a-table>
  </a-card>
</template>

<script>
import { JeecgListMixin } from '@/mixins/JeecgListMixin'
export default {
  name: 'ShippingFeesTotal',
  mixins: [JeecgListMixin],
  data() {
    return {
      columns: [
        {
          title: '#',
          width: '180px',
          align: 'center',
          dataIndex: 'rowIndex',
          customRender: function (text, r, index) {
            return (text !== '合计') ? (parseInt(index) + 1) : text
          }
        },
        {
          title: '客户代码',
          sorter: true,
          dataIndex: 'code',
        },
        {
          title: '店铺',
          sorter: true,
          dataIndex: 'shop',
        },
        {
          title: '未发货订单量',
          sorter: true,
          dataIndex: 'ordersToProcess',
        },
        {
          title: '已发货未开票订单量',
          sorter: true,
          dataIndex: 'processedOrders',
        },
        {
          title: '已发货未开票应收款',
          sorter: true,
          dataIndex: 'dueForProcessedOrders',
        },
      ],
      url: {
        list: "/shippingInvoice/breakdown/byShop"
      },
      /* 分页参数 */
      ipagination:{
        current: 1,
        pageSize: 50,
        pageSizeOptions: ['10', '50', '100'],
        showTotal: (total, range) => {
          return range[0] + "-" + range[1] + " 共" + total + "条"
        },
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      dataLoading: true,

    }
  },
  created(){

  },
  methods: {

  }
}
</script>

<style scoped>

</style>