<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="24">
            <a-form-model-item label="汇率对代码" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="code">
              <a-input v-model="model.code" placeholder="请输入汇率对代码"  ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="初始币种" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="originalCurrency">
              <j-dict-select-tag type="list" v-model="model.originalCurrency" dictCode="currency,zh_name,code" placeholder="请选择初始币种" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="目标币种" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="targetCurrency">
              <j-dict-select-tag type="list" v-model="model.targetCurrency" dictCode="currency,zh_name,code" placeholder="请选择目标币种" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="汇率(1初始币种=？目标币种)" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="rate">
              <a-input-number v-model="model.rate" placeholder="请输入汇率(1初始币种=？目标币种)" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24">
            <a-form-model-item label="生效时间" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="effectiveDate">
              <j-date placeholder="请选择生效时间" v-model="model.effectiveDate"  style="width: 100%" />
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
    name: 'ExchangeRatesForm',
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
           originalCurrency: [
              { required: true, message: '请输入初始币种!'},
           ],
           targetCurrency: [
              { required: true, message: '请输入目标币种!'},
           ],
           rate: [
              { required: true, message: '请输入汇率(1初始币种=？目标币种)!'},
              { pattern: /^-?\d+\.?\d*$/, message: '请输入数字!'},
           ],
        },
        url: {
          add: "/exchange_rates/exchangeRates/add",
          edit: "/exchange_rates/exchangeRates/edit",
          queryById: "/exchange_rates/exchangeRates/queryById"
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