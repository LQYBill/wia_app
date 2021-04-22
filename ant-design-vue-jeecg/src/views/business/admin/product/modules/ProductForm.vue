<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <!-- 主表单区域 -->
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="12" >
            <a-form-model-item label="商品代码" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="code">
              <a-input v-model="model.code" placeholder="请输入商品代码" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="中文名" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="zhName">
              <a-input v-model="model.zhName" placeholder="请输入中文名" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="英文名" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="enName">
              <a-input v-model="model.enName" placeholder="请输入英文名" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="敏感属性ID" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="sensitiveAttributeId">
              <j-dict-select-tag type="radio" v-model="model.sensitiveAttributeId" dictCode="sensitive_attribute,zh_name,id" placeholder="请选择敏感属性ID" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="重量，单位为克" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="weight">
              <a-input-number v-model="model.weight" placeholder="请输入重量，单位为克" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="体积重，单位为立方厘米" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="volume">
              <a-input-number v-model="model.volume" placeholder="请输入体积重，单位为立方厘米" style="width: 100%" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </j-form-container>
      <!-- 子表单区域 -->
    <a-tabs v-model="activeKey" @change="handleChangeTabs">
      <a-tab-pane tab="SKU表" :key="refKeys[0]" :forceRender="true">
        <j-editable-table
          :ref="refKeys[0]"
          :loading="skuTable.loading"
          :columns="skuTable.columns"
          :dataSource="skuTable.dataSource"
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

  import { getAction } from '@api/manage'
  import { FormTypes,getRefPromise,VALIDATE_NO_PASSED } from '@/utils/JEditableTableUtil'
  import { JEditableTableModelMixin } from '@/mixins/JEditableTableModelMixin'
  import { validateDuplicateValue } from '@/utils/util'

  export default {
    name: 'ProductForm',
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
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        validatorRules: {
           zhName: [
              { required: true, message: '请输入中文名!'},
           ],
           enName: [
              { required: true, message: '请输入英文名!'},
           ],
        },
        refKeys: ['sku', ],
        tableKeys:['sku', ],
        activeKey: 'sku',
        // SKU表
        skuTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: '商品ID',
              key: 'productId',
              type: FormTypes.sel_search,
              dictCode:"product,code,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'ERP中商品代码',
              key: 'erpCode',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '库存数量',
              key: 'availableAmount',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '在途数量',
              key: 'purchasingAmount',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        url: {
          add: "/product/product/add",
          edit: "/product/product/edit",
          queryById: "/product/product/queryById",
          sku: {
            list: '/product/product/querySkuByMainId'
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
        this.skuTable.dataSource=[]
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
          this.requestSubTableData(this.url.sku.list, params, this.skuTable)
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
          skuList: allValues.tablesValue[0].values,
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

    }
  }
</script>

<style scoped>
</style>