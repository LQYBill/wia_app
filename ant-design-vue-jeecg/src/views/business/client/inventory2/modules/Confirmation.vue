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
        :dataSource="orderDetails"
      >

        <template slot="imgSlot" slot-scope="text">
          <div style="font-size: 12px;font-style: italic;">
            <span v-if="!text">No picture available</span>
            <img v-else
                 :src="getImgView(text)"
                 :preview="getImgView(text)"
                 alt="SKU photo"
                 style="min-width:50px;max-width:80px;height:50px;"
            />
          </div>
        </template>

        <template slot="adjustNumber" slot-scope="text, record, index">
          <div>
            <a-input-number
              v-model="currentQuantity[index]"
              :min="0"
              @change="adjustOrder"
            />
          </div>
        </template>
      </a-table>


    </a-card>
  </a-spin>
</template>

<script>
import DetailList from '@comp/tools/DetailList'
import {JeecgListMixin} from '@/mixins/JeecgListMixin'
import {JEditableTableModelMixin} from '@/mixins/JEditableTableModelMixin'

const DetailListItem = DetailList.Item

const {postAction} = require("@api/manage");

export default {
  name: 'ClientPlatformOrderDetail',
  mixins: [JEditableTableModelMixin, JeecgListMixin],
  components: {
    DetailList,
    DetailListItem,
  },
  data() {
    return {
      columns: [
        {
          title: 'Photo',
          align: 'center',
          dataIndex: 'imageSource',
          scopedSlots: {customRender: 'imgSlot'}
        },
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
          align: 'right',
          scopedSlots: {customRender: 'adjustNumber'},
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
      currentQuantity: [],
      url: {
        orderInfo: '/business/clientPlatformOrder/placeOrder',
        adjustOrder: '/business/clientPlatformOrder/adjustOrder',
        confirmOrder: '/business/purchaseOrder/client/add'
      }
    }
  },
  props: {
    okCallback: Function
  },
  methods: {
    loadData(data) {
      console.log("data : " + data)
      const params = data.map(
        id => ({
          ID: id,
          quantity: 1
        })
      )
      console.log("SKU to buy: " + this.skuIdentifiers)
      postAction(this.url.adjustOrder, params)
        .then(
          res => {
            this.client = res.result.clientInfo
            this.orderData = res.result.data
            this.orderDetails = res.result.voPurchaseDetails
            this.currentQuantity = this.orderDetails.map(line => (line['quantity']))
            console.log(this.orderDetails)
            console.log(this.minQuantity)
            console.log(this.currentQuantity)
          }
        )
    },
    adjustOrder() {
      let params = []
      for (let i = 0; i < this.orderDetails.length; i++) {
        params.push({
          ID: this.orderDetails[i]['skuId'],
          quantity: this.currentQuantity[i]
        })
      }

      postAction(this.url.adjustOrder, params)
        .then(res => {
          this.client = res.result.clientInfo
          this.orderData = res.result.data
          this.orderDetails = res.result.voPurchaseDetails
          this.currentQuantity = this.orderDetails.map(line => (line['quantity']))
          console.log(this.orderDetails)
          console.log(this.minQuantity)
          console.log(this.currentQuantity)
        })
    },

    confirmOrder() {
      const params = {
        skuQuantity: this.orderDetails.map(
          line => ({
            ID: line['skuId'],
            quantity: line['quantity']
          })
        ),
        platformOrderIDList: []
      }

      postAction(this.url.confirmOrder, params)
        .then((res) => {
          console.log("new purchase id: " + res.result)
          this.okCallback()
        })

    },
    handleCancel() {
    },

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