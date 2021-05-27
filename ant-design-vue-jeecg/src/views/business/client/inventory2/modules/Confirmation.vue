<template>
  <a-spin :spinning="confirmLoading">
    <a-card :bordered="false">
      <detail-list title="Purchase">
        <detail-list-item term="Sku Number">{{ orderData.skuNumber }}</detail-list-item>
        <detail-list-item term="Sku Quantity">{{ orderData.totalQuantity }}</detail-list-item>
        <detail-list-item term="Total Amount">{{ orderData.estimatedTotalPrice }}</detail-list-item>
        <detail-list-item term="Discount">{{ orderData.reducedAmount }}</detail-list-item>
      </detail-list>
      <a-divider style="margin-bottom: 10px"/>
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
      <a-divider style="margin-bottom: 10px"/>

      <div class="title">Order Details
        <a-tooltip :title=" getProvisionTooltip() ">
          <a-card style='text-align: center'>
            Stock up for
            <a-input-number
              v-model="currentProvisionDays"
              :defaultValue='0'
              :min='0'
              :max='28'
              @change="adjustProvision"
            />
            {{ currentProvisionDays > 1 ? " Days" : " Day" }}
          </a-card>
        </a-tooltip>
      </div>
      <a-table
        style="margin-bottom: 24px"
        :columns="columns"
        :dataSource="orderDetails"
        :pagination="false"
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

        <template slot="salesQuantitySlot" slot-scope="record">
          <a-tooltip title="Sales from last 7 days">
            {{ getSales(record['skuId'])["sales7"] }}
          </a-tooltip>
          |
          <a-tooltip title="Sales from last 14 days">
            {{ getSales(record['skuId'])["sales14"] }}
          </a-tooltip>
          |
          <a-tooltip title="Sales from last 28 days">
            {{ getSales(record['skuId'])["sales28"] }}
          </a-tooltip>
        </template>

        <template slot="provisionalStockSlot" slot-scope="record">
          <a-tooltip :title=" getProvisionalTooltip(record['skuId']) ">
            {{ getProvisionalStock(record['skuId']) }}
          </a-tooltip>
        </template>

        <template slot="futureProvisionalStockSlot" slot-scope="record">
          <a-tooltip :title=" getFutureProvisionalTooltip(record['skuId'], record['quantity']) ">
            {{ getProvisionalStock(record['skuId']) + record['quantity']}}
          </a-tooltip>
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
          title: 'Sales from last 7/14/28 days',
          align: 'center',
          scopedSlots: {customRender: 'salesQuantitySlot'}
        },
        {
          title: 'Provisional stock',
          scopedSlots: {customRender: 'provisionalStockSlot'}
        },
        {
          title: 'Future provisional stock',
          scopedSlots: {customRender: 'futureProvisionalStockSlot'}
        },
        {
          title: 'Unit Price',
          dataIndex: 'price',
        },
        {
          title: 'Quantity',
          dataIndex: 'quantity',
          align: 'center',
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
      currentProvisionDays: 0,
      currentQuantity: [],
      url: {
        orderInfo: '/business/clientPlatformOrder/placeOrder',
        adjustOrder: '/business/clientPlatformOrder/adjustOrder',
        confirmOrder: '/business/purchaseOrder/client/add'
      }
    }
  },
  props: {
    skuInfoArray: Array,
    okCallback: Function
  },
  methods: {
    loadData() {
      console.log(this.skuInfoArray);
      const params = this.skuInfoArray.map(
        skuInfo => {
          let sku = skuInfo["id"];
          let provisionalStock = this.getProvisionalStock(sku);
          return {
            ID: sku,
            quantity: provisionalStock < 0 ? -provisionalStock : 0
          }
        }
      )
      console.log(params);
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
    getSales(sku) {
      let sales;
      this.skuInfoArray.map(
        skuInfo => {
          if (skuInfo["id"] === sku) {
            sales = {
              sales7: skuInfo["sales7"],
              sales14: skuInfo["sales14"],
              sales28: skuInfo["sales28"]
            }
          }
        }
      )
      return sales;
    },
    getProvisionalStock(sku) {
      let provisionalStock;
      this.skuInfoArray.map(
        skuInfo => {
          if (skuInfo["id"] === sku) {
            provisionalStock = skuInfo["availableQuantity"] +  skuInfo["purchasingQuantity"] - skuInfo["platformOrderQuantity"];
          }
        }
      )
      return provisionalStock;
    },
    getAvailableQuantity(sku) {
      let availableQuantity;
      this.skuInfoArray.map(
        skuInfo => {
          if (skuInfo["id"] === sku) {
            availableQuantity = skuInfo["availableQuantity"];
          }
        }
      )
      return availableQuantity;
    },
    getPurchasingQuantity(sku) {
      let purchasingQuantity;
      this.skuInfoArray.map(
        skuInfo => {
          if (skuInfo["id"] === sku) {
            purchasingQuantity = skuInfo["purchasingQuantity"];
          }
        }
      )
      return purchasingQuantity;
    },
    getPlatformOrderQuantity(sku) {
      let platformOrderQuantity;
      this.skuInfoArray.map(
        skuInfo => {
          if (skuInfo["id"] === sku) {
            platformOrderQuantity = skuInfo["platformOrderQuantity"];
          }
        }
      )
      return platformOrderQuantity;
    },
    getProvisionalTooltip(sku) {
      return this.getAvailableQuantity(sku) + " (Available Quantity)\n"
        + "+"
        + this.getPurchasingQuantity(sku) + " (Purchasing Quantity)\n"
        + "-"
        + this.getPlatformOrderQuantity(sku) + " (Platform order Quantity)"
    },
    getFutureProvisionalTooltip(sku, quantityToPurchase) {
      return this.getProvisionalStock(sku) + " (Provisional Quantity)\n"
        + "+"
        + quantityToPurchase + " (Quantity to purchase)"
    },
    getProvisionTooltip() {
      return "Purchasing additional quantity enough for " + (this.currentProvisionDays || 0) + " day"
        + (this.currentProvisionDays > 1 ? "s" : "")
        + ", based on sales from last 28 days"
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
    calculateSalesPerDay(sales7, sales14, sales28) {
      let salesPerDay = 0;
      if (sales14 === 0) {
        if (sales28 === 0) {
          salesPerDay = sales7 / 7
        } else {
          salesPerDay = sales7 / 7 * 0.9 + sales28/ 28 * 0.1
        }
      } else if (sales28 === 0) {
        salesPerDay = sales7 / 7 * 0.7 + sales14 / 14 * 0.3
      } else {
        salesPerDay = sales7 / 7 * 0.6 + sales14 / 14 * 0.3 + sales28 / 28 * 0.1
      }
      return salesPerDay;
    },
    adjustProvision() {
      for (let i = 0; i < this.skuInfoArray.length; i++) {
        let salesPerDay = 0;
        let sales7 = this.skuInfoArray[i]['sales7'];
        let sales14 = this.skuInfoArray[i]['sales14'];
        let sales28 = this.skuInfoArray[i]['sales28'];
        let availableQuantity = this.skuInfoArray[i]['availableQuantity'];
        let purchasingQuantity = this.skuInfoArray[i]['purchasingQuantity'];
        let platformOrderQuantity = this.skuInfoArray[i]['platformOrderQuantity'];
        salesPerDay = this.calculateSalesPerDay(sales7, sales14, sales28)
        let provisionalQuantity = availableQuantity + purchasingQuantity - platformOrderQuantity;
        let salesEstimate = Math.ceil(salesPerDay * this.currentProvisionDays)
        if (provisionalQuantity - salesEstimate >= 0) {
          this.currentQuantity[i] = 0
        } else {
          this.currentQuantity[i] = salesEstimate - provisionalQuantity
        }
      }
      this.adjustOrder();
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
<style>
/* Force line break in tooltip */
.ant-tooltip-inner {
  white-space: pre-line;
}
</style>