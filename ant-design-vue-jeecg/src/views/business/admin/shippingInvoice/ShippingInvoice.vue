<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <!-- 搜索区域 -->
      <a-form layout="inline" ref="searchForm">
        <a-row :gutter="24">
          <a-col
            :md="6"
            :sm="8"
          >
            <a-form-item
              :label="$t('invoice.customer')"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 18}"
            >
              <a-select
                show-search
                :placeholder="$t('invoice.clientInputSearch')"
                option-filter-prop="children"
                :filter-option="customerFilterOption"
                @change="handleClientChange"
              >
                <a-select-option
                  v-for="(item, index) in customerList"
                  :key="index"
                  :value="index"
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :md="6"
            :sm="8"
          >
            <a-form-item
              :label="$t('invoice.shop')"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 18}"
            >
              <a-select
                mode="multiple"
                style="width: 100%"
                :placeholder="$t('invoice.defaultAllShopSelected')"
                @change="handleShopChange"
                :allowClear=true
                v-model="shopIDs"
                :disabled="shopDisable"
              >
                <a-select-option
                  v-for="(item, index) in shopList"
                  :value="item.value"
                  :key="index"
                >
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :md="6"
            :sm="8"
          >
            <a-form-item
              :label="$t('startDate')"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 14}"
            >
              <a-range-picker
                :disabled-date="disabledDate"
                @change="onDateChange"
                :disabled="dataDisable"
                :defaultValue="datePickerDefaultValue()"
              />
            </a-form-item>
          </a-col>
          <span
            style="float: left;overflow: hidden;"
            class="table-page-search-submitButtons"
          >
            <a-col
              :md="6"
              :sm="24"
            >
              <a-button
                type="primary"
                :loading="buttonLoading"
                :disabled="makeInvoiceDisable"
                @click="makeInvoice"
              >{{$t("invoice.generateDocument")}}</a-button>
            </a-col>
          </span>
          <!--  Button to make complete bill of both purchases and transport fee AFTER customer receives merchandise
          - Disabled by default
          - Active if all SKU have a price (no new SKU)
          - else warning with list of SKU without price
          -->
          <span
            style='float: left;overflow: hidden;'
            class='table-page-search-submitButtons'
          >
            <a-col
              :md='6'
              :sm='24'
            >
              <a-button
                type='danger'
                :loading='invoiceLoading'
                @click='makeCompletePostInvoice'
                :disabled='completeInvoiceDisable'
              >{{$t("invoice.generateInvoice7post")}}</a-button>
            </a-col>
          </span>
        </a-row>
      </a-form>
    </div>

  </a-card>
</template>

<script>
import {getAction, getFile} from '@/api/manage';
import {saveAs} from 'file-saver';
import moment from 'moment'
import {postAction} from "@api/manage";

export default {
  name: "GetInvoiceFile",
  components: {},
  data() {
    return {
      queryParam: {},
      customerList: [],
      /**
       *  available shops, array of
       *  {text: shop.erpCode, value: shop.id,}
       */
      shopList: [],
      /**
       * Selected shops, array of shop ID
       */
      shopIDs: [],
      customerId: "",
      selectedClient: null,
      startDate: null,
      endDate: null,
      orderList: [],
      selectedStartDate: "",
      selectedEndDate: "",
      url: {
        makeCompleteInvoice: '/shippingInvoice/postShipping/makeComplete',
        checkOrdersBetweenDate: '/shippingInvoice/postShipping/ordersBetweenDates',
        getClientList: "/client/client/all",
        getValidPeriod: "/shippingInvoice/period",
        getShopsByCustomerId: "/shippingInvoice/shopsByClient",
        makeInvoice: "/shippingInvoice/make",
        downloadInvoice: "/shippingInvoice/download",
        invoiceDetail: "/shippingInvoice/invoiceDetail"
      },
      buttonLoading: false,
      shopDisable: true,
      dataDisable: true,
      invoiceLoading: false,
      completeInvoiceDisable: true,
      makeInvoiceDisable: true,
      purchasePricesAvailable: true
    }
  },
  created() {
    this.loadClientList();
  },

  computed: {}

  ,
  methods: {
    /**
     * Load client list from API
     */
    loadClientList() {
      let self = this
      getAction(this.url.getClientList)
        .then(res => {
          if (res.success) {
            console.log(res)
            self.customerList = res.result.map(customer => ({
              text: `${customer.firstName} ${customer.surname} (${customer.internalCode})`,
              value: customer.id,
              client: customer
            }));
          }
        })
    },

    handleClientChange(index) {
      console.log(`selected ${index}`);
      this.customerId = this.customerList[index].client.id;
      this.client = this.customerList[index].client
      this.loadShopList(this.customerId)
        .then(
          () =>
            this.shopDisable = false
        );
      // clear selected shop IDs
      this.shopIDs = []
    },

    customerFilterOption(input, option) {
      return (
        option.componentOptions.children[0]
          .text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
    },

    /**
     * Send a request to load shop list by client ID from API.
     * In case of success of the request, load shop list, if none shop,
     * display a warning message.
     * In case of failure of the request, a message will be displayed.
     *
     * @param clientID the client ID
     * @return Promise for following operation to synchronize
     */
    loadShopList(clientID) {
      let self = this
      const param = {clientID: clientID}
      return getAction(this.url.getShopsByCustomerId, param)
        .then(res => {
          if (res.success) {
            if (res.result.length === 0) {
              self.$message.warning("没有找到当前客户的相关店铺信息");
            }
            console.log(res.result)
            self.shopList = res.result.map(
              shop => ({
                text: shop.erpCode,
                value: shop.id,
              })
            );
          } else {
            self.$message.warning("Internal server error. Try later.");
          }
        })
    },

    handleShopChange(value) {
      // value returned is array of shop
      this.shopIDs = value
      console.log(this.shopIDs)
      if (this.shopIDs.length !== 0) {
        this.loadAvailableDate()
      } else {
        this.startDate = null;
        this.endDate = null;
        this.selectedStartDate = null;
        this.selectedEndDate = null;
        this.dataDisable = true;
        this.completeInvoiceDisable = true;
        this.makeInvoiceDisable = true;
      }
    },

    /**
     * load available date from API,
     * return promise for synchronizing operation
     * @returns {Promise<void>}
     */
    loadAvailableDate() {
      const param = this.shopIDs
      return postAction(this.url.getValidPeriod, param)
        .then(res => {
          if (res.success) {
            this.startDate = moment(res['result']['start']).startOf('day')
            this.endDate = moment(res['result']['end']).endOf('day')
            this.dataDisable = false
          } else {
            this.$message.warning(res.message)
            this.dataDisable = true
          }

        })
    },

    disabledDate(current) {
      // Can not select days before start or end
      return current < this.startDate
        || current > this.endDate;
    },

    onDateChange(date, dateString) {
      this.selectedStartDate = dateString[0]
      this.selectedEndDate = dateString[1]
      this.checkSkuBetweenDate();
    },

    datePickerDefaultValue() {
      return [this.startDate, this.endDate]
    },

    makeInvoice() {
      let self = this
      self.loading = true;
      if (!this.customerId) {
        this.$message.warning("请选择客户！")
        return
      } else if (!this.selectedStartDate || !self.selectedEndDate) {
        this.$message.warning("请选择日期！")
        return
      }
      const param = {
        clientID: this.customerId,
        shopIDs: this.shopIDs,
        start: this.selectedStartDate,
        end: moment(self.selectedEndDate).add(1, 'days').format('YYYY-MM-DD'),
      }
      self.buttonLoading = true
      postAction(this.url.makeInvoice, param)
        .then(
          res => {
            self.buttonLoading = false
            console.log(res)
            if (!res.success) {
              self.$message.error(res.message, 10)
            } else {
              let filename = res.result.filename
              let code = res.result.invoiceCode
              this.downloadInvoice(filename).then(
                this.$message.info("Download succeed.")
              )
              this.downloadDetailFile(code)

            }
          }
        )
    },//end of makeInvoice()

    downloadInvoice(filename) {
      const param = {filename: filename}
      console.log(filename)
      return getFile(this.url.downloadInvoice, param)
        .then(res => {
          console.log(res)
          saveAs(res, filename)
        }).then(() => {
          this.loadAvailableDate()
        })
    },

    downloadDetailFile(invoiceNumber) {
      const param = {
        invoiceNumber: invoiceNumber
      }
      getFile(this.url.invoiceDetail, param).then(
        res => {
          let now = moment().format("yyyyMMDD")
          let name = this.client.internalCode + "_" + invoiceNumber + '_Détail_calcul_de_facture_' + now + '.xlsx'
          saveAs(res, name)
        }
      )
    },
    //cette méthode est trigger lorsqu'on change la date
    // on va checker si tous les sku des commandes entre les dates sont disponibles
    // si oui, le bouton pour générer les factures complètes post reception s'active
    checkSkuBetweenDate() {
      let self = this
      self.loading = true;
      if (!this.customerId) {
        this.$message.warning("请选择客户！Please Select a client")
        return
      } else if (!this.selectedStartDate || !self.selectedEndDate) {
        this.$message.warning("请选择日期！")
        this.completeInvoiceDisable = true;
        this.makeInvoiceDisable = true;
        this.purchasePricesAvailable = false;
        return
      }
      const param = {
        clientID: this.customerId,
        shopIDs: this.shopIDs,
        start: this.selectedStartDate,
        end: moment(self.selectedEndDate).add(1, 'days').format('YYYY-MM-DD'),
      }
      console.log("button disabled for complete invoice before : " + this.completeInvoiceDisable);

      postAction(self.url.checkOrdersBetweenDate, param)
        .then(res => {
          console.log("check SKU post shipping available price : " + res.code);
          self.purchasePricesAvailable = res.code === 200; //200 : success
          console.log("PurchasesPricesAvailable : " + self.purchasePricesAvailable);
          if (res.message) {
            this.$message.warning(res.message);
          }
          this.completeInvoiceDisable = !this.purchasePricesAvailable;
          this.makeInvoiceDisable = !this.purchasePricesAvailable;
          // if purchasePricesAvailable then enable the red button
          console.log("button disabled for complete invoice : " + this.completeInvoiceDisable);
        });
    }, // end of checkSkuBetweenDate
    makeCompletePostInvoice() {
      console.log("Post Shipping");
      let self = this
      self.loading = true
      if (!this.customerId) {
        this.$message.warning('请选择客户！')
        return
      }
      let param = {
        clientID: this.customerId,
        shopIDs: this.shopIDs,
        start: this.selectedStartDate,
        end: moment(self.selectedEndDate).add(1, 'days').format('YYYY-MM-DD'),
      }
      self.invoiceDisable = true
      self.findOrdersLoading = true
      self.orderListLoading = true
      self.shopDisable = true
      self.clientDisable = true
      postAction(this.url.makeCompleteInvoice, param)
        .then(
          res => {
            console.log(res)
            if (!res.success) {
              self.$message.error(res.message, 10)
            } else {
              let filename = res.result.filename
              let code = res.result.invoiceCode
              this.downloadInvoice(filename).then(
                this.$message.info('Download succeed.')
              )
              this.downloadDetailFile(code)
              this.pagination.current = 1
              this.loadOrders()
            }
            self.clientDisable = false
            self.shopDisable = false
            self.completeInvoiceDisable = false
            self.findOrdersLoading = false
            self.orderListLoading = false
          }
        )
    } // end of makeCompleteInvoice()
  } // end of methods
}
</script>
<style scoped>
@import "~@assets/less/common.less";
</style>
