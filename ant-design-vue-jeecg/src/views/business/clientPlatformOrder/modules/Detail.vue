<template>
  <a-spin :spinning="confirmLoading">
    <a-card :bordered="false">
      <detail-list title="Purchase">
        <detail-list-item term="Sku Number">{{ detail.skuNumber }}</detail-list-item>
        <detail-list-item term="Sku Quantity">{{ detail.totalQuantity }}</detail-list-item>
        <detail-list-item term="Total Amount">{{ detail.estimatedTotalPrice }}</detail-list-item>
        <detail-list-item term="Discount">{{ detail.reducedAmount }}</detail-list-item>
      </detail-list>
      <a-divider style="margin-bottom: 32px"/>
      <detail-list title="Client Information">
        <detail-list-item term="First Name">{{ client.firstName }}</detail-list-item>
        <detail-list-item term="Family Name">{{ client.surname }}</detail-list-item>
        <detail-list-item term="Invoice Name">{{ client.invoiceEntity }}</detail-list-item>
        <detail-list-item term="Email">{{ client.email }}</detail-list-item>
        <detail-list-item term="phone">{{ client.phone }}</detail-list-item>
        <detail-list-item term="Address">{{ client.streetNumber + " " + client.streetName }}</detail-list-item>
        <detail-list-item term="City">{{ client.city + ", " + client.country }}</detail-list-item>
        <detail-list-item v-if="client.companyIdValue" term="Company">{{
            client.companyIdValue
          }}({{ client.companyIdType }})
        </detail-list-item>
      </detail-list>
      <a-divider style="margin-bottom: 32px"/>

      <div class="title">Order Details</div>
      <a-table
        style="margin-bottom: 24px"
        :columns="columns"
        :dataSource="orderData">
      </a-table>
    </a-card>
  </a-spin>
</template>

<script>
import DetailList from '@/components/tools/DetailList'

const DetailListItem = DetailList.Item

import {JEditableTableModelMixin} from '@/mixins/JEditableTableModelMixin'
const {postAction} = require("@api/manage");


export default {
  name: 'ClientPlatformOrderDetail',
  mixins: [JEditableTableModelMixin],
  components: {
    DetailList,
    DetailListItem,
  },
  data() {
    return {
      columns: [
        {
          title: 'SKU Code',
          dataIndex: 'erpCode',
          key: 'erpCode'
        },
        {
          title: 'Product Name',
          dataIndex: 'product',
          key: 'product'
        },
        {
          title: 'Unit Price',
          dataIndex: 'price',
          key: 'price'
        },
        {
          title: 'Quantity',
          dataIndex: 'quantity',
          key: 'quantity',
          align: 'right'
        },
        {
          title: 'Amount',
          dataIndex: 'total',
          key: 'total',
          align: 'right'
        }
      ],
      client: {
        city: undefined,
        companyIdType: undefined,
        companyIdValue: undefined,
        country: undefined,
        email: undefined,
        firstName: undefined,
        invoiceEntity: undefined,
        phone: undefined,
        postcode: undefined,
        streetName: undefined,
        streetNumber: undefined,
        surname: undefined
      },
      detail: {
        estimatedTotalPrice: undefined,
        reducedAmount: undefined,
        skuNumber: undefined,
        totalQuantity: undefined,
      },
      orderData: undefined,
      url: {
        orderInfo: '/business/clientPlatformOrder/purchase',
      },
    }
  },
  props: {
    orderIds: Array
  },
  methods: {
    loadData() {
      const params = this.orderIds
      postAction(this.url.orderInfo, params)
        .then(
          res => {
            console.log(res.result)
            this.client = res.result.clientInfo
            this.detail = res.result.data
            this.orderData = res.result.voPurchaseDetails
          }
        )
    },
  },
  computed: {},
  created() {
  }

}
</script>

<style scoped>
.title {
  color: rgba(0,0,0,.85);
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
}
</style>