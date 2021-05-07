<template>
  <a-spin :spinning="confirmLoading">
    <a-card :bordered="false">
      <detail-list title="Purchase">
        <detail-list-item term="Sku Number">{{ orderData.skuNumber }}</detail-list-item>
        <detail-list-item term="Sku Quantity">{{ orderData.totalQuantity }}</detail-list-item>
        <detail-list-item term="Total Amount">{{ orderData.estimatedTotalPrice }}</detail-list-item>
        <detail-list-item term="Discount">{{ orderData.reducedAmount }}</detail-list-item>
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
        :dataSource="orderDetails">
      </a-table>
    </a-card>
  </a-spin>
</template>

<script>
import DetailList from '@comp/tools/DetailList'

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
        },
        {
          title: 'Unit Price',
          dataIndex: 'price',
        },
        {
          title: 'Quantity',
          dataIndex: 'quantity',
          align: 'right'
        },
        {
          title: 'Amount',
          dataIndex: 'total',
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
      orderData: {
        estimatedTotalPrice: undefined,
        reducedAmount: undefined,
        skuNumber: undefined,
        totalQuantity: undefined,
      },
      orderDetails: [],
      url: {
        orderInfo: '/business/clientPlatformOrder/placeOrder',
        confirmOrder: '/business/purchaseOrder/client/add'
      },
    }
  },
  props: {
    orderIDs: Array,
    okCallback:Function
  },
  methods: {
    loadData() {
      const params = this.orderIDs
      console.log("get confirmation data: " + params)
      postAction(this.url.orderInfo, params)
        .then(
          res => {
            this.client = res.result.clientInfo
            this.orderData = res.result.data
            this.orderDetails = res.result.voPurchaseDetails
          }
        )
    },
    confirmOrder() {
      const params = {
        skuQuantity:[
          {
            ID:"1386681311858663426",
            quantity: 2
          },
          {
            ID: "1386681312626221057",
            quantity: 3
          }
        ],
        platformOrderIDList: this.orderIDs
      }

      postAction(this.url.confirmOrder, params).then((res) => {
        console.log("new purchase id: " + res.result)
        this.okCallback()
      })

    },
    handleCancel() {
    }
  },
  computed: {},
  created() {
  }

}
</script>

<style scoped>
.title {
  color: rgba(0, 0, 0, .85);
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
}
</style>