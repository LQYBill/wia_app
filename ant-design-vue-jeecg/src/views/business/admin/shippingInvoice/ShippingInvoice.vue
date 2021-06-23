<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <!-- 搜索区域 -->
      <a-form layout="inline">
        <a-row :gutter="24">
          <a-col
            :md="6"
            :sm="8"
          >
            <a-form-item
              label="客户"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 18}"
            >
              <a-select
                show-search
                placeholder="输入客户进行搜索"
                option-filter-prop="children"
                :filter-option="customerFilterOption"
                @change="handleClientChange"
              >
                <a-select-option
                  v-for="(item, index) in customerList"
                  :key="index"
                  :value="item.value"
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
              label="店铺"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 18}"
            >
              <a-select
                mode="multiple"
                style="width: 100%"
                placeholder="不选默认所有店铺"
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
            :md="8"
            :sm="10"
          >
            <a-form-item
              label="开始时间"
              :labelCol="{span: 5}"
              :wrapperCol="{span: 14}"
            >
              <a-range-picker
                :disabled-date="disabledDate"
                @change="onDateChange"
                :disabled="dataDisable"
                :defaultPickerValue="[this.startDate, this.endDate]"
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
                @click="makeInvoice"
              >生成文件</a-button>
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
      // customerList:[{"id":"111111","surname":"TEST","name":"one","invoiceEntityName":"one TEST Invoice Name","codeName":"customerOne","email":"123123123@163.com","phone":"123123123","invoiceAddress":"testAddress1","currency":0,"discount":1.0,"createBy":null,"createTime":null,"updateBy":null,"updateTime":null},{"id":"222222","surname":"REAL","name":"two","invoiceEntityName":"two REAL Invoice Name","codeName":"customerTwo","email":"321321321@qq.com","phone":"321321321","invoiceAddress":"testAddress2","currency":1,"discount":1.0,"createBy":null,"createTime":null,"updateBy":null,"updateTime":null}],
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
      startDate: null,
      endDate: null,
      selectedStartDate: "",
      selectedEndDate: "",
      url: {
        getClientList: "/client/client/all",
        getValidPeriod: "/shippingInvoice/period",
        getShopsByCustomerId: "/shippingInvoice/shopsByClient",
        makeInvoice: "/shippingInvoice/make",
        downloadInvoice: "/shippingInvoice/download"
      },
      buttonLoading: false,
      shopDisable: true,
      dataDisable: true
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
              text: `${customer.firstName} ${customer.surname}`,
              value: customer.id,
            }));
          }
        })
    },

    handleClientChange(value) {
      console.log(`selected ${value}`);
      this.customerId = value;
      this.loadShopList(value)
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
        this.loadAvailableDate().then(
          this.dataDisable = false
        )
      } else {
        this.dataDisable = false
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
          console.log(res.result)
          this.startDate = moment(res['result']['start'])
          this.endDate = moment(res['result']['end'])
        })
    },

    disabledDate(current) {
      // Can not select days before start or end
      return current <= this.startDate.endOf('day')
        || current >= moment(this.endDate).endOf('day').add(3, 'day');
    },

    onDateChange(date, dateString) {
      this.selectedStartDate = dateString[0]
      this.selectedEndDate = dateString[1]
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
              self.$message.error(res.message)
            } else {
              this.downloadInvoice(res.result)
            }
          }
        )
    },

    downloadInvoice(filename) {
      const param = {filename: filename}
      getFile(this.url.downloadInvoice, param)
        .then(res => {
          console.log(res)
          saveAs(res, filename)
        })
    }
  }
}
</script>
<style scoped>
@import "~@assets/less/common.less";
</style>
