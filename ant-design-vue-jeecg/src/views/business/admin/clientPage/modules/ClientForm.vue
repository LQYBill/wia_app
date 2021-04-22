<template>
   <a-spin :spinning="confirmLoading">
     <j-form-container :disabled="formDisabled">
       <!-- 主表单区域 -->
       <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
         <a-row>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="姓" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="surname">
              <a-input v-model="model.surname" placeholder="请输入姓" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="名" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="firstName">
              <a-input v-model="model.firstName" placeholder="请输入名" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="简称" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="internalCode">
              <a-input v-model="model.internalCode" placeholder="请输入简称" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="发票实体" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="invoiceEntity">
              <a-input v-model="model.invoiceEntity" placeholder="请输入发票实体" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="email">
              <a-input v-model="model.email" placeholder="请输入邮箱" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="电话" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="phone">
              <a-input v-model="model.phone" placeholder="请输入电话" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="门牌号码" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="streetNumber">
              <a-input v-model="model.streetNumber" placeholder="请输入门牌号码" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="街道名" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="streetName">
              <a-input v-model="model.streetName" placeholder="请输入街道名" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="备用地址" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="additionalAddress">
              <a-input v-model="model.additionalAddress" placeholder="请输入备用地址" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="邮编" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="postcode">
              <a-input v-model="model.postcode" placeholder="请输入邮编" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="城市" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="city">
              <a-input v-model="model.city" placeholder="请输入城市" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="国家" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="country">
              <a-input v-model="model.country" placeholder="请输入国家" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="货币" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="currency">
              <a-input v-model="model.currency" placeholder="请输入货币" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="运费折扣" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingDiscount">
              <a-input-number v-model="model.shippingDiscount" placeholder="请输入运费折扣" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="公司识别码类型" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="companyIdType">
              <a-input v-model="model.companyIdType" placeholder="请输入公司识别码类型" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="公司识别码数值" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="companyIdValue">
              <a-input v-model="model.companyIdValue" placeholder="请输入公司识别码数值" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="账户余额" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="balance">
              <a-input-number v-model="model.balance" placeholder="请输入账户余额" style="width: 100%" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
     </j-form-container>
      <!-- 子表单区域 -->
      <a-tabs v-model="activeKey" @change="handleChangeTabs">
        <a-tab-pane tab="店铺" :key="refKeys[0]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[0]"
            :loading="shopTable.loading"
            :columns="shopTable.columns"
            :dataSource="shopTable.dataSource"
            :maxHeight="300"
            :disabled="formDisabled"
            :rowNumber="true"
            :rowSelection="true"
            :actionButton="true"/>
        </a-tab-pane>
        <a-tab-pane tab="客户名下SKU" :key="refKeys[1]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[1]"
            :loading="clientSkuTable.loading"
            :columns="clientSkuTable.columns"
            :dataSource="clientSkuTable.dataSource"
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
    name: 'ClientForm',
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
           surname: [
              { required: true, message: '请输入姓!'},
           ],
           firstName: [
              { required: true, message: '请输入名!'},
           ],
           internalCode: [
              { required: true, message: '请输入简称!'},
           ],
        },
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        refKeys: ['shop', 'clientSku', ],
        tableKeys:['shop', 'clientSku', ],
        activeKey: 'shop',
        // 店铺
        shopTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: '店主ID',
              key: 'ownerId',
              type: FormTypes.sel_search,
              dictCode:"client,internal_code,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'ERP内名称',
              key: 'erpCode',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '店铺名称',
              key: 'name',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '网站地址',
              key: 'website',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '物流折扣',
              key: 'shippingDiscount',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        // 客户名下SKU
        clientSkuTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: '客户ID',
              key: 'clientId',
              type: FormTypes.sel_search,
              dictCode:"client,internal_code,id",
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
          ]
        },
        url: {
          add: "/client/client/add",
          edit: "/client/client/edit",
          shop: {
            list: '/client/client/queryShopByMainId'
          },
          clientSku: {
            list: '/client/client/queryClientSkuByMainId'
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
            this.shopTable.dataSource=[]
            this.clientSkuTable.dataSource=[]
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
          this.requestSubTableData(this.url.shop.list, params, this.shopTable)
          this.requestSubTableData(this.url.clientSku.list, params, this.clientSkuTable)
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
          shopList: allValues.tablesValue[0].values,
          clientSkuList: allValues.tablesValue[1].values,
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