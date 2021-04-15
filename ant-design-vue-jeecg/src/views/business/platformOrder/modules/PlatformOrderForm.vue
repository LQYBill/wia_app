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
              <j-search-select-tag v-model="model.status" dict="sku_status,status_text,status_code" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
     </j-form-container>
      <!-- 子表单区域 -->
      <a-tabs v-model="activeKey" @change="handleChangeTabs">
        <a-tab-pane tab="平台订单内容" :key="refKeys[0]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[0]"
            :loading="platformOrderContentTable.loading"
            :columns="platformOrderContentTable.columns"
            :dataSource="platformOrderContentTable.dataSource"
            :maxHeight="300"
            :disabled="formDisabled"
            :rowNumber="true"
            :rowSelection="true"
            :actionButton="true"/>
        </a-tab-pane>
      </a-tabs>
       <a-row v-if="showFlowSubmitButton" style="text-align: center;width: 100%;margin-top: 16px;"><a-button @click="handleOk">提 交</a-button></a-row>
    </a-spin>
</template>

<script>

  import { FormTypes,getRefPromise,VALIDATE_NO_PASSED } from '@/utils/JEditableTableUtil'
  import { JEditableTableModelMixin } from '@/mixins/JEditableTableModelMixin'
  import { validateDuplicateValue } from '@/utils/util'

  export default {
    name: 'PlatformOrderForm',
    mixins: [JEditableTableModelMixin],
    components: {
    },
    data() {
      return {
        labelCol: {
          xs: { span: 24 },
          sm: { span: 6 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        labelCol2: {
          xs: { span: 24 },
          sm: { span: 3 },
        },
        wrapperCol2: {
          xs: { span: 24 },
          sm: { span: 20 },
        },
        model:{
        },
        validatorRules: {
        },
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        refKeys: ['platformOrderContent', ],
        tableKeys:['platformOrderContent', ],
        activeKey: 'platformOrderContent',
        // 平台订单内容
        platformOrderContentTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: '平台订单ID',
              key: 'platformOrderId',
              type: FormTypes.sel_search,
              dictCode:"platform_order,platform_order_id,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'SKU ID',
              key: 'skuId',
              type: FormTypes.sel_search,
              dictCode:"sku,erp_code,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'SKU数量',
              key: 'quantity',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '商品采购总费用',
              key: 'purchaseFee',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '物流总费用',
              key: 'shippingFee',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '服务总费用',
              key: 'serviceFee',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'SKU 状态',
              key: 'status',
              type: FormTypes.sel_search,
              dictCode:"sku_status,status_text,status_code",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        url: {
          add: "/business/platformOrder/add",
          edit: "/business/platformOrder/edit",
          platformOrderContent: {
            list: '/business/platformOrder/queryPlatformOrderContentByMainId'
          },
        }
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