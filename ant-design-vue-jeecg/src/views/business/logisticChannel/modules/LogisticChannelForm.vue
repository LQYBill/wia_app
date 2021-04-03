<template>
  <a-spin :spinning="confirmLoading">
    <j-form-container :disabled="formDisabled">
      <!-- 主表单区域 -->
      <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
        <a-row>
          <a-col :span="24" >
            <a-form-model-item label="内部名称" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="internalName">
              <a-input v-model="model.internalName" placeholder="请输入内部名称" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="中文名称" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="zhName">
              <a-input v-model="model.zhName" placeholder="请输入中文名称" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="英文名称" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="enName">
              <a-input v-model="model.enName" placeholder="请输入英文名称" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="公司名称" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="company">
              <a-input v-model="model.company" placeholder="请输入公司名称" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="是否使用抛重" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="useVolumetricWeight">
              <j-dict-select-tag type="radio" v-model="model.useVolumetricWeight" dictCode="" placeholder="请选择是否使用抛重" />
            </a-form-model-item>
          </a-col>
          <a-col :span="24" >
            <a-form-model-item label="抛重系数" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="volumetricWeightFactor">
              <a-input-number v-model="model.volumetricWeightFactor" placeholder="请输入抛重系数" style="width: 100%" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </j-form-container>
      <!-- 子表单区域 -->
    <a-tabs v-model="activeKey" @change="handleChangeTabs">
      <a-tab-pane tab="物流渠道价格" :key="refKeys[0]" :forceRender="true">
        <j-editable-table
          :ref="refKeys[0]"
          :loading="logisticChannelPriceTable.loading"
          :columns="logisticChannelPriceTable.columns"
          :dataSource="logisticChannelPriceTable.dataSource"
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
    name: 'LogisticChannelForm',
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
            useVolumetricWeight:0,
        },
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        validatorRules: {
           zhName: [
              { required: true, message: '请输入中文名称!'},
           ],
           useVolumetricWeight: [
              { required: true, message: '请输入是否使用抛重!'},
           ],
           volumetricWeightFactor: [
              { required: true, message: '请输入抛重系数!'},
           ],
        },
        refKeys: ['logisticChannelPrice', ],
        tableKeys:['logisticChannelPrice', ],
        activeKey: 'logisticChannelPrice',
        // 物流渠道价格
        logisticChannelPriceTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: '物流渠道',
              key: 'channelId',
              type: FormTypes.sel_search,
              dictCode:"logistic_channel,zh_name,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '生效日期',
              key: 'effectiveDate',
              type: FormTypes.date,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '有效国家',
              key: 'effectiveCountry',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '重量范围起始',
              key: 'weightRangeStart',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '重量范围截至',
              key: 'weightRangeEnd',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '首重',
              key: 'minimumWeight',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '首重价格',
              key: 'minimumWeightPrice',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '续重单位',
              key: 'calUnit',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '续重单价',
              key: 'calUnitPrice',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '操作附加费',
              key: 'additionalCost',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '挂号费',
              key: 'registrationFee',
              type: FormTypes.inputNumber,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
          ]
        },
        url: {
          add: "/business/logisticChannel/add",
          edit: "/business/logisticChannel/edit",
          queryById: "/business/logisticChannel/queryById",
          logisticChannelPrice: {
            list: '/business/logisticChannel/queryLogisticChannelPriceByMainId'
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
        this.logisticChannelPriceTable.dataSource=[]
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
          this.requestSubTableData(this.url.logisticChannelPrice.list, params, this.logisticChannelPriceTable)
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
          logisticChannelPriceList: allValues.tablesValue[0].values,
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