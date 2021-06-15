<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <!-- 主表单区域 -->
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="24" >
            <a-form-model-item label="product name" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="name">
              <a-input v-model="model.name" placeholder="请输入product name" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="client name" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="createBy">
              <a-input v-model="model.createBy" placeholder="请输入client name" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="create time" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="createTime">
              <j-date placeholder="请选择create time" v-model="model.createTime" :show-time="true" date-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="status" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="status">
              <a-input-number v-model="model.status" placeholder="请输入status" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="url 1" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="url1">
              <a-input v-model="model.url1" placeholder="请输入url 1" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="url 2" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="url2">
              <a-input v-model="model.url2" placeholder="请输入url 2" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="url 3" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="url3">
              <a-input v-model="model.url3" placeholder="请输入url 3" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="image" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="image">
              <j-image-upload isMultiple  v-model="model.image" ></j-image-upload>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="description" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="description">
              <j-editor v-model="model.description" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="quantity" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="quantity">
              <a-input-number v-model="model.quantity" placeholder="请输入quantity" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="unit price" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="unitPrice">
              <a-input-number v-model="model.unitPrice" placeholder="请输入unit price" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="unit shipfee" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="unitShipfee">
              <a-input-number v-model="model.unitShipfee" placeholder="请输入unit shipfee" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="destination country" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="destination">
              <j-multi-select-tag type="list_multi" v-model="model.destination" dictCode="" placeholder="请选择destination country" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="comment" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="comment">
              <j-editor v-model="model.comment" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="file 1" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="file1">
              <a-input v-model="model.file1" placeholder="请输入file 1" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="file 2" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="file2">
              <a-input v-model="model.file2" placeholder="请输入file 2" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="file 3" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="file3">
              <a-input v-model="model.file3" placeholder="请输入file 3" ></a-input>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </j-form-container>
      <!-- 子表单区域 -->
    <a-tabs v-model="activeKey" @change="handleChangeTabs">
      <a-tab-pane tab="Response Order" :key="refKeys[0]" :forceRender="true">
        <j-editable-table
          :ref="refKeys[0]"
          :loading="responseOrderTable.loading"
          :columns="responseOrderTable.columns"
          :dataSource="responseOrderTable.dataSource"
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

  import { getAction } from '@/api/manage'
  import { FormTypes,getRefPromise,VALIDATE_NO_PASSED } from '@/utils/JEditableTableUtil'
  import { JEditableTableModelMixin } from '@/mixins/JEditableTableModelMixin'
  import { validateDuplicateValue } from '@/utils/util'

  export default {
    name: 'RequestOrderForm',
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
           name: [
              { required: true, message: '请输入product name!'},
           ],
           createBy: [
              { required: true, message: '请输入client name!'},
           ],
           createTime: [
              { required: true, message: '请输入create time!'},
           ],
           status: [
              { required: true, message: '请输入status!'},
           ],
           description: [
              { required: true, message: '请输入description!'},
           ],
           quantity: [
              { required: true, message: '请输入quantity!'},
           ],
           unitPrice: [
              { required: true, message: '请输入unit price!'},
           ],
           unitShipfee: [
              { required: true, message: '请输入unit shipfee!'},
           ],
           destination: [
              { required: true, message: '请输入destination country!'},
           ],
        },
        refKeys: ['responseOrder', ],
        tableKeys:['responseOrder', ],
        activeKey: 'responseOrder',
        // Response Order
        responseOrderTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: 'request id (foreigner key)',
              key: 'requestId',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'request_quantity',
              key: 'requestQuantity',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'employee',
              key: 'createBy',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'create time',
              key: 'createTime',
              type: FormTypes.datetime,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'avalable time',
              key: 'avalable',
              type: FormTypes.date,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'status',
              key: 'status',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'bid unit price',
              key: 'bidUnitPrice',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'bid unit shipfee',
              key: 'bidUnitShipfee',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'unit registration fee',
              key: 'registrationFee',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'source country',
              key: 'source',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'destination country',
              key: 'destination',
              type: FormTypes.select,
              dictCode:"",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'discount (%)',
              key: 'discount',
              type: FormTypes.select,
              dictCode:"",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: 'bit total price',
              key: 'bitTotalPrice',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        url: {
          add: "/order/requestOrder/add",
          edit: "/order/requestOrder/edit",
          queryById: "/order/requestOrder/queryById",
          responseOrder: {
            list: '/order/requestOrder/queryResponseOrderByMainId'
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
        this.responseOrderTable.dataSource=[]
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
          this.requestSubTableData(this.url.responseOrder.list, params, this.responseOrderTable)
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
          responseOrderList: allValues.tablesValue[0].values,
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