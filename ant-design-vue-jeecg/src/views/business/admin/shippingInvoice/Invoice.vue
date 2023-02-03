<template>
  <a-card :bordered='false'>
    <!-- table区域 begin -->
    <section>
      <a-row type="flex">
        <a-col
          :md="3"
          :sm="12"
        >
          <a-button
            type='primary'
            @click='downloadPdf'
          >download invoice (pdf)</a-button>
        </a-col>
        <a-col
          :md="3"
          :sm="12"
        >
          <a-button
            type='primary'
            @click='downloadDetail'
          >download invoice detail</a-button>
        </a-col>
      </a-row>
    </section>
    <section>
        <a-table
          ref='table'
          size='middle'
          bordered
          rowKey='id'
          class='j-table-force-nowrap'
          :scroll='{x:true}'
          :columns='columns'
          :dataSource='dataSource'
          :loading='invoiceContentLoading'
        >
          <template #title>
            <h1>Customer</h1>
            <h2>Invoice n. : {{invoice_number}}</h2>
            <h2>Date : <time datetime="2023-02-01"></time>2023-02-01</h2>
          </template>
          <template #footer>
            <a-row type="flex">
              <a-col :span="12"><h2>Total</h2></a-col>
              <a-col :span="3"><h2>Quantity :</h2> {{total_quantity}}</a-col>
              <a-col :span="3"><h2>Discount :</h2> {{total_discount}}</a-col>
              <a-col :span="3"><h2>Refund :</h2> {{total_refund}}</a-col>
              <a-col :span="3"><h2>Total amount :</h2> : {{total}}</a-col>
            </a-row>
          </template>
        </a-table>
    </section>
    <!-- table区域 end -->
  </a-card>
</template>

<script>
import { getAction, getFile } from '@/api/manage'
import { saveAs } from 'file-saver'
import { postAction } from '@api/manage'
import moment from 'moment'

export default {
  name: 'Invoice',
  components: {},

  data() {
    return {
      pageUrl: this.$route.path,
      invoice_number: this.$router.currentRoute.query.invoice_number,
      invoiceNumberTest: '2022-12-7016',
      total_quantity: 0,
      total_discount: 0,
      total_refund: 0,
      paid_amount: 0,
      final_total: 0,
      invoiceContentLoading: false,
      url: {
        downloadInvoice: '/shippingInvoice/download',
        invoiceData: '/shippingInvoice/invoiceData',
      },
      dataSource: [
        {
          key: '1',
          description: 'some description',
          price: 32,
          quantity: 2,
          discount: null,
          refund: 0,
          total_amount: 64
        },
        {
          key: '2',
          description: 'some description',
          price: 15,
          quantity: 5,
          discount: 0,
          refund: 0,
          total_amount: 45
        },
        {
          key: '3',
          description: 'some description',
          price: 1.3,
          quantity: 6,
          discount: 0,
          refund: 0,
          total_amount: 7.8
        },
        {
          key: '4',
          description: 'some description',
          price: 0.2,
          quantity: 25,
          discount: 0,
          refund: 0,
          total_amount: 5
        },
      ],
      columns: [
        {
          title: 'Reference',
          key: 'rowIndex',
          width: 60,
          align: 'center',
          customRender: (t, r, index) => parseInt(index) + 1
        },
        {
          title: 'Description',
          align: 'center',
          dataIndex: 'description',
        },
        {
          title: 'PU (€/P)',
          align: 'center',
          dataIndex: 'price',
        },
        {
          title: 'Order Quantity',
          align: 'center',
          dataIndex: 'quantity',
        },
        {
          title: 'Discount',
          align: 'center',
          dataIndex: 'discount',
        },
        {
          title: 'Refund',
          align: 'center',
          dataIndex: 'refund',
        },
        {
          title: 'Total',
          align: 'center',
          dataIndex: 'total_amount',
        },
      ],
    }
  },
  // TODO : check user role and if user is shop owner
  created() {
    this.loadInvoice();
  },
  computed: {
    // itemsSubTotal() {
    //   return this.dataSource.map(item => ({...item, total_amount: item.quantity*item.price-item.discount}));
    // },
    total: function() {
      let total = 0;
      let qty = 0;
      let discount = 0;
      let refund = 0;
      this.dataSource.forEach((item) => {
        total += item.total_amount;
        qty += item.quantity;
        discount += item.discount;
        refund += item.refund;
        this.total_quantity = qty;
        this.total_discount = discount;
        this.total_refund = refund;
      });
      return total;
    },
    num: getInvoiceNum
  },
  methods: {
    loadInvoice() {
      // let num = this.$router.currentRoute.query.invoice_number;
      const param = {
        invoiceNumber: this.num
      };
      console.log(param);
      getAction(this.url.invoiceData, param).then(res=>{
        this.invoiceContentLoading = true;
        if(res.success) {
          console.log("Res : " + res.result.totalAmount);

          if(res.result !== null) {
            console.log("Invoice for " + this.invoice_number + " exists.");
            this.$message.success("Invoice for " + this.invoice_number + " exists.");
            this.paid_amount = res.result.paid_amount;
          }
          else {
            console.log("Invoice for " + this.invoice_number + " doesn't exists.");
            this.$message.error("Invoice for " + this.invoice_number + " doesn't exists.");
          }

        }
        this.invoiceContentLoading = false;
      })
      // this.dataSource.push(
      //   {
      //     description: 'Total',
      //     price: '',
      //     total_amount: this.total,
      //     quantity: this.total_quantity,
      //     discount: this.total_discount,
      //     refund: this.total_refund
      //   }
      // );
      // TODO : at the very end push a row with total of each column
    },
    downloadInvoice(filename) {
      const param = { filename: filename }
      console.log(filename)
      return getFile(this.url.downloadInvoice, param)
        .then(res => {
          console.log(res)
          saveAs(res, filename)
        })
    },
    downloadDetailFile(invoiceNumber) {
      const param = {
        invoiceNumber: invoiceNumber
      }
      getFile(this.url.invoiceDetail, param).then(
        res => {
          let now = moment().format('yyyyMMDD')
          let name = this.client.internalCode + "_" + invoiceNumber + '_Détail_calcul_de_facture_' + now + '.xlsx'
          saveAs(res, name)
        })
    },
    downloadPdf() {

    },
    downloadDetail() {

    },
  }
}

function getInvoiceNum() {
  this.invoice_number = this.$router.currentRoute.query.invoice_number
  return this.invoice_number
}
</script>
<style scoped>
@import "~@assets/less/common.less";
</style>
