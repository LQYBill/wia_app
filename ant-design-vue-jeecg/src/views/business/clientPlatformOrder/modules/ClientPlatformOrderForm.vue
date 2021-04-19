<template>
   <a-spin :spinning="confirmLoading">
     <j-form-container :disabled="formDisabled">
       <!-- 主表单区域 -->
       <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
         <a-row>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="店铺ID" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shopId">
              <j-search-select-tag v-model="model.shopId" dict="shop,erp_code,id" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="物流渠道" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="logisticChannelName">
              <j-search-select-tag v-model="model.logisticChannelName" dict="logistic_channel,zh_name,zh_name" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="平台订单号码" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="platformOrderId">
              <a-input v-model="model.platformOrderId" placeholder="请输入平台订单号码" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="平台订单交易号" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="platformOrderNumber">
              <a-input v-model="model.platformOrderNumber" placeholder="请输入平台订单交易号" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="物流跟踪号" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="trackingNumber">
              <a-input v-model="model.trackingNumber" placeholder="请输入物流跟踪号" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="订单交易时间" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="orderTime">
              <j-date placeholder="请选择订单交易时间" v-model="model.orderTime" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="订单发货时间" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingTime">
              <j-date placeholder="请选择订单发货时间" v-model="model.shippingTime" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="订单收件人" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="recepient">
              <a-input v-model="model.recepient" placeholder="请输入订单收件人" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="订单收件人国家" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="country">
              <a-input v-model="model.country" placeholder="请输入订单收件人国家" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="订单收件人邮编" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="postcode">
              <a-input v-model="model.postcode" placeholder="请输入订单收件人邮编" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="物流挂号费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="fretFee">
              <a-input-number v-model="model.fretFee" placeholder="请输入物流挂号费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="物流发票号" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingInvoiceNumber">
              <j-search-select-tag v-model="model.shippingInvoiceNumber" dict="shipping_invoice,invoice_number,id" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="status">
              <a-input v-model="model.status" placeholder="请输入状态" ></a-input>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
     </j-form-container>
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
         <detail-list-item v-if="client.companyIdValue" term="Company">{{ client.companyIdValue }}({{ client.companyIdType }})</detail-list-item>
       </detail-list>
       <a-divider style="margin-bottom: 32px"/>

       <div class="title">Order Details</div>
       <a-table
         style="margin-bottom: 24px"
         :columns="columns"
         :dataSource="orderData">
       </a-table>
     </a-card>
       <a-row v-if="showFlowSubmitButton" style="text-align: center;width: 100%;margin-top: 16px;"><a-button @click="handleOk">提 交</a-button></a-row>
    </a-spin>
</template>

<script>

  import { FormTypes,getRefPromise,VALIDATE_NO_PASSED } from '@/utils/JEditableTableUtil'
  import { JEditableTableModelMixin } from '@/mixins/JEditableTableModelMixin'
  import { validateDuplicateValue } from '@/utils/util'
  import PageLayout from '@/components/page/PageLayout'
  import STable from '@/components/table/'
  import DetailList from '@/components/tools/DetailList'
  import ABadge from "ant-design-vue/es/badge/Badge"

  export default {
    name: 'PlatformOrderForm',
    mixins: [JEditableTableModelMixin],
    components: {
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
      //流程表单data
      formData: {
        type: Object,
        default: ()=>{},
        required: false
      },
      //表单模式：false流程表单 true普通表单
      formBpm: {
        type: Boolean,
        default: false,
        required: false
      },
      //表单禁用
      disabled: {
        type: Boolean,
        default: false,
        required: false
      }
    },
    computed: {
      formDisabled(){
        if(this.formBpm===true){
          if(this.formData.disabled===false){
            return false
          }
          return true
        }
        return this.disabled
      },
      showFlowSubmitButton(){
        if(this.formBpm===true){
          if(this.formData.disabled===false){
            return true
          }
        }
        return false
      }
    },
    created () {
      //如果是流程中表单，则需要加载流程表单data
      this.showFlowData();
    },
    methods: {
     addBefore(){
            this.platformOrderContentTable.dataSource=[]
      },
      getAllTable() {
        let values = this.tableKeys.map(key => getRefPromise(this, key))
        return Promise.all(values)
      },
      /** 调用完edit()方法之后会自动调用此方法 */
      editAfter() {
        this.$nextTick(() => {
        })
        // 加载子表数据
        if (this.model.id) {
          let params = { id: this.model.id }
          this.requestSubTableData(this.url.platformOrderContent.list, params, this.platformOrderContentTable)
        }
      },
      //校验所有一对一子表表单
    validateSubForm(allValues){
        return new Promise((resolve,reject)=>{
          Promise.all([
          ]).then(() => {
            resolve(allValues)
          }).catch(e => {
            if (e.error === VALIDATE_NO_PASSED) {
              // 如果有未通过表单验证的子表，就自动跳转到它所在的tab
              this.activeKey = e.index == null ? this.activeKey : this.refKeys[e.index]
            } else {
              console.error(e)
            }
          })
        })
    },
      /** 整理成formData */
      classifyIntoFormData(allValues) {
        let main = Object.assign(this.model, allValues.formValue)

        return {
          ...main, // 展开
          platformOrderContentList: allValues.tablesValue[0].values,
        }
      },
      //渲染流程表单数据
     showFlowData(){
        if(this.formBpm === true){
            let params = {id:this.formData.dataId};
            getAction(this.url.queryById,params).then((res)=>{
              if(res.success){
                this.edit (res.result);
              }
            })
         }
      },
      validateError(msg){
        this.$message.error(msg)
      },
     close() {
        this.visible = false
        this.$emit('close')
        this.$refs.form.clearValidate();
      },

    }
  }
</script>

<style scoped>
</style>