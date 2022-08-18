<template>
   <a-spin :spinning="confirmLoading">
     <j-form-container :disabled="formDisabled">
       <!-- 主表单区域 -->
       <a-form-model ref="form" :model="model" :rules="validatorRules" slot="detail">
         <a-row>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="商品ID" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="productId">
              <j-search-select-tag v-model="model.productId" dict="product,code,id" :async="true"/>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="ERP中商品代码" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="erpCode">
              <a-input v-model="model.erpCode" placeholder="请输入ERP中商品代码" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="库存数量" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="availableAmount">
              <a-input-number v-model="model.availableAmount" placeholder="请输入库存数量" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="在途数量" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="purchasingAmount">
              <a-input-number v-model="model.purchasingAmount" placeholder="请输入在途数量" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-model-item label="图片链接" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="imageSource">
              <a-input v-model="model.imageSource" placeholder="请输入图片链接地址" ></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="运费折扣" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="shippingDiscount">
              <a-input-number v-model="model.shippingDiscount" placeholder="请输入运费折扣" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12" >
            <a-form-model-item label="服务费" :labelCol="labelCol" :wrapperCol="wrapperCol" prop="serviceFee">
              <a-input-number v-model="model.serviceFee" placeholder="请输入服务费" style="width: 100%" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
     </j-form-container>
      <!-- 子表单区域 -->
      <a-tabs v-model="activeKey" @change="handleChangeTabs">
        <a-tab-pane tab="SKU价格表" :key="refKeys[0]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[0]"
            :loading="skuPriceTable.loading"
            :columns="skuPriceTable.columns"
            :dataSource="skuPriceTable.dataSource"
            :maxHeight="300"
            :disabled="formDisabled"
            :rowNumber="true"
            :rowSelection="true"
            :actionButton="true"/>
        </a-tab-pane>
        <a-tab-pane tab="SKU物流折扣" :key="refKeys[1]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[1]"
            :loading="shippingDiscountTable.loading"
            :columns="shippingDiscountTable.columns"
            :dataSource="shippingDiscountTable.dataSource"
            :maxHeight="300"
            :disabled="formDisabled"
            :rowNumber="true"
            :rowSelection="true"
            :actionButton="true"/>
        </a-tab-pane>
        <a-tab-pane tab="SKU申报价格" :key="refKeys[2]" :forceRender="true">
          <j-editable-table
            :ref="refKeys[2]"
            :loading="skuDeclaredValueTable.loading"
            :columns="skuDeclaredValueTable.columns"
            :dataSource="skuDeclaredValueTable.dataSource"
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
    name: 'SkuForm',
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
          shippingDiscount: [
            { required: true, message: '请输入运费折扣!'},
          ],
          serviceFee: [
            { required: true, message: '请输入服务费!'},
          ],
        },
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 0,
        refKeys: ['skuPrice', 'shippingDiscount', 'skuDeclaredValue', ],
        tableKeys:['skuPrice', 'shippingDiscount', 'skuDeclaredValue', ],
        activeKey: 'skuPrice',
        // SKU价格表
        skuPriceTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: 'SKU ID',
              key: 'skuId',
              type: FormTypes.sel_search,
              dictCode:"sku,erp_code,id",
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '价格',
              key: 'price',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
              validateRules: [{ required: true, message: '${title}不能为空' }],
            },
            {
              title: '优惠价起订量',
              key: 'threshold',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '优惠价',
              key: 'discountedPrice',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '生效日期',
              key: 'date',
              type: FormTypes.date,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        // SKU物流折扣
        shippingDiscountTable: {
          loading: false,
          dataSource: [],
          columns: [
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
              title: '折扣',
              key: 'discount',
              type: FormTypes.input,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
            {
              title: '生效日期',
              key: 'date',
              type: FormTypes.date,
              width:"200px",
              placeholder: '请输入${title}',
              defaultValue:'',
            },
          ]
        },
        // SKU申报价格
        skuDeclaredValueTable: {
          loading: false,
          dataSource: [],
          columns: [
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
              title: '申报价格',
              key: 'declaredValue',
              type: FormTypes.input,
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
            },
          ]
        },
        url: {
          add: "/business/sku/add",
          edit: "/business/sku/edit",
          skuPrice: {
            list: '/business/sku/querySkuPriceByMainId'
          },
          shippingDiscount: {
            list: '/business/sku/queryShippingDiscountByMainId'
          },
          skuDeclaredValue: {
            list: '/business/sku/querySkuDeclaredValueByMainId'
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
            this.skuPriceTable.dataSource=[]
            this.shippingDiscountTable.dataSource=[]
            this.skuDeclaredValueTable.dataSource=[]
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
          this.requestSubTableData(this.url.skuPrice.list, params, this.skuPriceTable)
          this.requestSubTableData(this.url.shippingDiscount.list, params, this.shippingDiscountTable)
          this.requestSubTableData(this.url.skuDeclaredValue.list, params, this.skuDeclaredValueTable)
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
          skuPriceList: allValues.tablesValue[0].values,
          shippingDiscountList: allValues.tablesValue[1].values,
          skuDeclaredValueList: allValues.tablesValue[2].values,
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