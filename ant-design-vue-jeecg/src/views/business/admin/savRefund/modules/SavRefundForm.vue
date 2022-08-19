<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="24">
            <a-form-model-item label="平台订单ID" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="platformOrderId">
              <j-search-select-tag v-model="model.platformOrderId" dict="platform_order,platform_order_id,id" :async="true" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="采购退款" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="purchaseRefund">
              <j-switch v-model="model.purchaseRefund"  ></j-switch>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="采购退款金额" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="purchaseRefundAmount">
              <a-input-number v-model="model.purchaseRefundAmount" placeholder="请输入采购退款金额" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="运费退款" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingRefund">
              <j-switch v-model="model.shippingRefund"  ></j-switch>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="退款原因" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="refundReason">
              <a-textarea v-model="model.refundReason" rows="4" placeholder="请输入退款原因" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </j-form-container>
  </a-spin>
</template>

<script>

  import { httpAction, getAction } from '@/api/manage'
  import { validateDuplicateValue } from '@/utils/util'

  export default {
    name: 'SavRefundForm',
    components: {
    },
    props: {
      //表单禁用
      disabled: {
        type: Boolean,
        default: false,
        required: false
      }
    },
    data () {
      return {
        model:{
            purchaseRefund:"N",
            shippingRefund:"Y",
         },
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        confirmLoading: false,
        validatorRules: {
           platformOrderId: [
              { required: true, message: '请输入平台订单ID!'},
           ],
           purchaseRefund: [
              { required: true, message: '请输入采购退款!'},
           ],
           purchaseRefundAmount: [
              { required: true, message: '请输入采购退款金额!'},
           ],
           shippingRefund: [
              { required: true, message: '请输入运费退款!'},
           ],
        },
        url: {
          add: "/savRefund/savRefund/add",
          edit: "/savRefund/savRefund/edit",
          queryById: "/savRefund/savRefund/queryById"
        }
      }
    },
    computed: {
      formDisabled(){
        return this.disabled
      },
    },
    created () {
       //备份model原始值
      this.modelDefault = JSON.parse(JSON.stringify(this.model));
    },
    methods: {
      add () {
        this.edit(this.modelDefault);
      },
      edit (record) {
        this.model = Object.assign({}, record);
        this.visible = true;
      },
      submitForm () {
        const that = this;
        // 触发表单验证
        this.$refs.form.validate(valid => {
          if (valid) {
            that.confirmLoading = true;
            let httpurl = '';
            let method = '';
            if(!this.model.id){
              httpurl+=this.url.add;
              method = 'post';
            }else{
              httpurl+=this.url.edit;
               method = 'put';
            }
            httpAction(httpurl,this.model,method).then((res)=>{
              if(res.success){
                that.$message.success(res.message);
                that.$emit('ok');
              }else{
                that.$message.warning(res.message);
              }
            }).finally(() => {
              that.confirmLoading = false;
            })
          }
         
        })
      },
    }
  }
</script>