<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="24">
            <a-form-model-item label="平台订单序列号（客户单号）" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="platformOrderSerialId">
              <a-input v-model="model.platformOrderSerialId" placeholder="请输入平台订单序列号（客户单号）"  ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="虚拟单号" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="virtualTrackingNumber">
              <a-input v-model="model.virtualTrackingNumber" placeholder="请输入虚拟单号"  ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="物流商内部单号" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="logisticInternalNumber">
              <a-input v-model="model.logisticInternalNumber" placeholder="请输入物流商内部单号"  ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="物流单号（服务商单号）" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="trackingNumber">
              <a-input v-model="model.trackingNumber" placeholder="请输入物流单号（服务商单号）"  ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="实际重量" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="realWeight">
              <a-input-number v-model="model.realWeight" placeholder="请输入实际重量" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="体积重量" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="volumetricWeight">
              <a-input-number v-model="model.volumetricWeight" placeholder="请输入体积重量" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="计费重量" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="chargingWeight">
              <a-input-number v-model="model.chargingWeight" placeholder="请输入计费重量" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="优惠金额" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="discount">
              <a-input-number v-model="model.discount" placeholder="请输入优惠金额" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="运费金额" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingFee">
              <a-input-number v-model="model.shippingFee" placeholder="请输入运费金额" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="燃油附加费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="fuelSurcharge">
              <a-input-number v-model="model.fuelSurcharge" placeholder="请输入燃油附加费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="挂号费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="registrationFee">
              <a-input-number v-model="model.registrationFee" placeholder="请输入挂号费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="重派费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="secondDeliveryFee">
              <a-input-number v-model="model.secondDeliveryFee" placeholder="请输入重派费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="增值税" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="vat">
              <a-input-number v-model="model.vat" placeholder="请输入增值税" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="增值税服务费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="vatServiceFee">
              <a-input-number v-model="model.vatServiceFee" placeholder="请输入增值税服务费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="附加费用" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="additionnalFee">
              <a-input-number v-model="model.additionalFee" placeholder="请输入附加费用" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="总费用" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="totalFee">
              <a-input-number v-model="model.totalFee" placeholder="请输入总费用" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="物流公司ID" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="logisticCompanyId">
              <j-search-select-tag v-model="model.logisticCompanyId" dict="logistic_company,name,id"  />
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
    name: 'LogisticExpenseDetailForm',
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
           logisticInternalNumber: [
              { required: true, message: '请输入物流商内部单号!'},
           ],
           trackingNumber: [
              { required: true, message: '请输入物流单号（服务商单号）!'},
           ],
           realWeight: [
              { required: true, message: '请输入实际重量!'},
           ],
           volumetricWeight: [
              { required: true, message: '请输入体积重量!'},
           ],
           chargingWeight: [
              { required: true, message: '请输入计费重量!'},
           ],
           discount: [
              { required: true, message: '请输入优惠金额!'},
           ],
           shippingFee: [
              { required: true, message: '请输入运费金额!'},
           ],
           fuelSurcharge: [
              { required: true, message: '请输入燃油附加费!'},
           ],
           registrationFee: [
              { required: true, message: '请输入挂号费!'},
           ],
           secondDeliveryFee: [
              { required: true, message: '请输入重派费!'},
           ],
           vat: [
              { required: true, message: '请输入增值税!'},
           ],
           vatServiceFee: [
              { required: true, message: '请输入增值税服务费!'},
           ],
           additionnalFee: [
              { required: true, message: '请输入附加费用!'},
           ],
           totalFee: [
              { required: true, message: '请输入总费用!'},
           ],
        },
        url: {
          add: "/business/logisticExpenseDetail/add",
          edit: "/business/logisticExpenseDetail/edit",
          queryById: "/business/logisticExpenseDetail/queryById"
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