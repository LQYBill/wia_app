<template>
  <a-card :bordered='false'>
    <div class="table-operator">
      <!-- button to download invoice -->
      <a-button v-if="downloadReady"
                type='primary'
                icon="download"
                @click='downloadPdf()'
      >download invoice</a-button>

      <!-- button to download invoice details -->
      <a-button v-if="downloadReady && hasEmail"
                type='primary'
                icon="mail"
                @click='sendEmail()'
      >receive details by email</a-button>
    </div>
    <section>
      <a-table  v-if="invoice_type === '2'"
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
          <h1 style='font-size: 2em'>{{customer}} <span style='font-weight: 200'>({{invoice_entity}})</span></h1>
          <a-row type="flex" justify='space-between' align-items='center'>
            <h2 >Invoice n. : {{invoice_number}}</h2>
            <h3>Currency : {{currency}}/{{currencySymbol}}</h3>
          </a-row>
        </template>
        <template #footer>
          <a-row type="flex">
            <a-col :span="12"><h2>Total</h2></a-col>
            <a-col :span="3"><h2 class='center'>Quantity : </h2></a-col>
            <a-col :span="3"><div class='center' style='font-size: 1.5em;'>{{total_quantity}}</div></a-col>
            <a-col :span="3"><h2 class='center'>Total amount : </h2></a-col>
            <a-col :span="3">
              <div class='center'  style='font-size: 1.5em;'>€{{final_total_euro}} EUR</div>
              <div class='center' v-if='currency !== "EUR"'>({{currencySymbol}}{{final_total_customer_curr}} {{currency}})</div>
            </a-col>
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
import { mapGetters } from 'vuex'

export default {
  name: 'Invoice',
  components: {},

  data() {
    return {
      customer: '',
      email: '',
      invoice_entity: '',
      currency: null,
      currencySymbol: "$",
      pageUrl: this.$route.path,
      invoice_number: null,
      invoiceID: null,
      total_quantity: 0,
      final_total_euro: 0,
      final_total_customer_curr: 0,
      keyNumber: 0,
      invoice_type: null,
      invoiceContentLoading: false,
      downloadReady: false,
      hasEmail: false,
      failedPdfList:[],
      url: {
        checkInvoiceValidity: '/shippingInvoice/checkInvoiceValidity',
        downloadInvoice: '/shippingInvoice/download',
        invoiceData: '/shippingInvoice/invoiceData',
        downloadCompleteInvoicePdf: "/generated/shippingInvoice/downloadPdf",
        sendDetailsByEmail: "/generated/shippingInvoice/sendDetailsByEmail"
      },
      dataSource: [],
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
          align: 'left',
          className: 'column_description',
          dataIndex: 'description',
        },
        {
          title: 'Order Quantity',
          align: 'center',
          dataIndex: 'quantity',
        },
        {
          title: 'Sub-total',
          align: 'center',
          dataIndex: 'total_amount',
        }
      ],
    }
  },
  created() {
    this.checkInvoice();
  },
  computed: {
    num: getInvoiceNum
  },
  methods: {
    ...mapGetters(["nickname", "userInfo"]),
    checkInvoice() { //2022-12-2004, 1587806418814332930
      let email = this.userInfo().email;
      let orgCode = this.userInfo().orgCode;
      console.log("User : " + email + " " + orgCode);
      if(orgCode.includes("A01") || orgCode.includes("A02") ||orgCode.includes("A03") || orgCode.includes("A04")) {
        let param = {
          invoiceID: this.num,
          email: email,
          orgCode: orgCode
        };
        getAction(this.url.checkInvoiceValidity, param).then(res=>{
          if(res.success){
            this.$message.success("Permission granted.");
            this.invoice_number = res.result.invoiceNumber;
            this.customer = res.result.name;
            this.email = res.result.email;
            this.hasEmail = !(this.email === "" || this.email === null);
            this.invoice_entity = res.result.invoiceEntity;
            let currency = res.result.currency;
            if(currency === 'EUR' || currency === 'euro' || currency === 'eur' || currency === 'EURO') {
              this.currency = 'EUR';
              this.currencySymbol = "€"
            }
            if(currency === "USD" || currency === 'usd') {
              this.currency = 'USD'
            }
            if(currency === "RMB" || currency === "rmb") {
              this.currency = "RMB";
              this.currency = "¥";
            }
            this.loadInvoice();
          }
          else {
            console.log("Error : " + res.message);
            this.$message.error(res.message);
          }
        });
      }
      else {
        this.$message.error("Not authorized to access this page.");
        return;
      }
    },
    loadInvoice() {
      const param = {
        invoiceNumber: this.invoice_number,
        originalCurrency: "EUR",
        targetCurrency: this.currency
      };
      // on identifie le type de facture (1 : purchase, 2: shipping, 7: purchase + shipping
      this.invoice_type = this.getInvoiceType();
      if(this.invoice_type == null) {
        return;
      }
      getAction(this.url.invoiceData, param).then(res=>{
        this.invoiceContentLoading = true;
        if(res.success) {
          if(res.result !== null) {
            this.downloadReady = true;
            for(let i in res.result.feeAndQtyPerCountry) {
              for(let key in res.result.feeAndQtyPerCountry[i]) {
                let subtotal = res.result.feeAndQtyPerCountry[i][key];
                this.final_total_euro += subtotal;
                this.total_quantity += Number(key);
                this.dataSource.push({
                  key: this.keyNumber,
                  description: "Total shipping cost for " + i,
                  quantity: key,
                  total_amount: subtotal,
                });
                // incrémente la clé
                this.keyNumber = this.keyNumber+1;
              }
            }
            // VAT
            this.dataSource.push({
              key: this.keyNumber,
              description: "Total VAT fee for EU",
              quantity: null,
              total_amount: res.result.vat
            });
            this.final_total_euro += res.result.vat;
            this.keyNumber = this.keyNumber+1;
            // SERVICE FEE
            this.dataSource.push({
              key: this.keyNumber,
              description: "Total service fee",
              quantity: null,
              total_amount: res.result.serviceFee
            });
            this.keyNumber = this.keyNumber+1;
            // REFUND
            if(res.result.refund > 0) {
              this.dataSource.push({
                key: this.keyNumber,
                description: 'Refund',
                quantity: null,
                total_amount: res.result.refund
              })
              this.final_total_euro -= res.result.refund;
              this.keyNumber = this.keyNumber + 1;
            }
            // DISCOUNT (not used yet)
            if(res.result.discount > 0) {
              this.dataSource.push({
                key: this.keyNumber,
                description: 'Discount',
                quantity: null,
                total_amount: res.result.discount
              })
              this.final_total_euro -= res.result.discount;
              this.keyNumber = this.keyNumber + 1;
            }
            if(this.currency !== "EUR") {
              this.final_total_customer_curr = res.result.finalAmount;
            }
          }
          else {
            this.$message.error("No data : " + this.invoice_number);
          }

        }
        this.invoiceContentLoading = false;
      })
    }, //end of loadInvoice()
    downloadPdf() {
      const param = {
        invoiceNumber: this.invoice_number
      }
      getFile(this.url.downloadCompleteInvoicePdf, param)
        .then(res => {
          let filename = "Invoice N°" + this.invoice_number + " (" + this.customer + ").pdf";
          saveAs(res, filename);
        })
        .catch((error) => {
          console.log(error);
          this.errorMessage = error;
          this.failedPdfList.push(this.invoice_number);
          console.log("failed pdf list : " + this.failedPdfList);
          this.$message.warning(error + " : " + this.invoice_number);
        });
    }, // end of DownloadPdf()
    sendEmail(){
      const param = {
        invoiceNumber: this.invoice_number,
        invoiceID: this.invoiceID,
        email: this.email,
        invoiceEntity: this.invoice_entity,
      }
      getAction(this.url.sendDetailsByEmail, param)
        .then(res => {
          if(res.success) {
            this.$message.success(res.result);
          }
          else {
            this.$message.error("Error "+ res.code + " : " + res.message);
          }
        })
        .catch((error) => {
          console.log(error);
          this.$message.error(error);
        });
    },
    getInvoiceType() {
      let re = new RegExp('^[0-9]{4}-[0-9]{2}-([0-9])[0-9]{3}$');
      if (re.test(this.invoice_number)) {
        let match = re.exec(this.invoice_number);
        return match[1];
      }
      else {
        this.$message.error("Invalid invoice number.");
        return null;
      }
    }, // end of getInvoiceType()
  }
}

function getInvoiceNum() {
  this.invoiceID = this.$router.currentRoute.query.invoiceID
  return this.invoiceID
}
</script>
<style scoped>
@import "~@assets/less/common.less";
</style>

<style>
h2.center, div.center {
  text-align: center;
}
</style>