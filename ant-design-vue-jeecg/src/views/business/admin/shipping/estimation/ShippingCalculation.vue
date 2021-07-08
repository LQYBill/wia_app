<template>
  <a-card :bordered="false" :loading="pageDisable">
    <!-- sku by measure-->
    <a-form-model ref="searchForm" :model="form" :rules="rules" layout="inline">
      <a-form-model-item label="目的地" prop="country">
        <a-select
          show-search
          placeholder="输入国家选择"
          option-filter-prop="children"
          :filter-option="filterOption"
          style="width:200px"
          v-model="form.country">

          <a-select-option :value="item.code" v-for="item in countries" :key="item.code">
            {{ item.code + "-" + item.nameZh + "-" + item.nameEn }}
          </a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item ref="weight" label="重量(g)" prop="weight">
        <a-input v-model="form.weight" style="width:50px"/>
      </a-form-model-item>
      <a-form-model-item>
        <span>体积</span>
      </a-form-model-item>
      <a-form-model-item ref="long" label="长(cm)">
        <a-input v-model="form.long" style="width:50px"/>
      </a-form-model-item>
      <a-form-model-item>
        <span>X</span>
      </a-form-model-item>
      <a-form-model-item ref="width" label="宽(cm)">
        <a-input v-model="form.width" style="width:50px"/>
      </a-form-model-item>
      <a-form-model-item>
        <span>X</span>
      </a-form-model-item>
      <a-form-model-item ref="height" label="高(cm)">
        <a-input v-model="form.height" style="width:50px"/>
      </a-form-model-item>
      <a-form-model-item>
        <a-button type="primary" htmlType="submit" @click="handleSubmit">
          搜索
        </a-button>
      </a-form-model-item>
    </a-form-model>
    <a-divider/>
    <!--  search by sku  -->
    <a-form-model ref="searchFormBySku" :model="formForSku" :rules="rulesForSku" layout="inline">
      <!-- select country -->
      <a-form-model-item label="目的地" prop="country">
        <a-select
          show-search
          placeholder="输入国家选择"
          option-filter-prop="children"
          :filter-option="filterOption"
          style="width:200px"
          v-model="formForSku.country"
        >
          <a-select-option :value="item.code" v-for="item in countries" :key="item.code">
            {{ item.code + "-" + item.nameZh + "-" + item.nameEn }}
          </a-select-option>
        </a-select>
      </a-form-model-item>

      <!-- select sku  -->
      <a-form-model-item label="sku" prop="sku">
        <a-select
          show-search
          placeholder="输入sku选择"
          option-filter-prop="children"
          :filter-option="filterOption"
          style="width:500px"
          v-model="formForSku.sku">
          <a-select-option v-for="(item,index) in skus" :key="index" :value="index">
            {{ item["erpCode"] + "-" + item.zhName }}
          </a-select-option>
        </a-select>
      </a-form-model-item>

      <a-form-item>
        <a-input-number id="inputNumber" v-model="quantity" :min="1"/>
        当前值：{{ quantity }}
      </a-form-item>

      <a-form-model-item>
        <a-button type="primary" @click="handleAdd">
          增加
        </a-button>
      </a-form-model-item>

      <br/>

      <a-form-model-item label="已选中">
        <a-tag
          v-for="(item,index) in formForSku.selectedSkuList"
          :key="index"
          :closable="true"
          @close="() => handleClose(index)"
          color="cyan"
        >
          {{ item.quantity + ' X ' + item.sku.zhName + item.sku.erpCode }}
        </a-tag>
      </a-form-model-item>


      <a-form-model-item>
        <a-button type="primary" htmlType="submit" @click="handleSkuSubmit">
          搜索
        </a-button>
      </a-form-model-item>

    </a-form-model>
    <a-divider/>
    <!--  data display table  -->
    <a-table :columns="columns" :data-source="priceList" rowKey="logisticsChannelName" bordered>
    </a-table>

  </a-card>
</template>

<script>
import {getAction, postAction} from "@/api/manage";
import {FormModel} from 'ant-design-vue';
import Vue from "vue";

Vue.use(FormModel)
const columns = [
  {
    title: "渠道名称",
    align: "center",
    dataIndex: "logisticsChannelName",
    width: 180,
  }, {
    title: "运费",
    children: [
      {
        title: "总运费(€)",
        align: "center",
        dataIndex: "TotalCost",
        width: 80,
        sorter: (a, b) => a.TotalCost - b.TotalCost
      },
      {
        title: "明细",
        children: [
          {
            title: "运费(€)",
            align: "center",
            dataIndex: "shippingCost",
            width: 80,
            sorter: (a, b) => a.shippingCost - b.shippingCost
          },
          {
            title: "挂号费(€)",
            align: "center",
            dataIndex: "registrationCost",
            width: 80,
            sorter: (a, b) => a.registrationCost - b.registrationCost
          }, {
            title: "额外费用(€)",
            align: "center",
            dataIndex: "additionalCost",
            width: 80,
            sorter: (a, b) => a.additionalCost - b.additionalCost
          },
        ]
      }
    ],
  },
  {
    title: "生效日期",
    align: "center",
    dataIndex: "effectiveDate",
    width: 120,
  }, {
    title: "单价(€/g)",
    align: "center",
    dataIndex: "unitPrice",
    width: 80,
  }
]


export default {
  name: "ShippingCalculation",
  data() {
    return {
      priceList: [],
      columns: columns,
      countries: undefined,
      skus: undefined,
      form: {
        country: undefined,
        weight: undefined,
        long: null,
        width: null,
        height: null,
      },
      formForSku: {
        country: undefined,
        sku: undefined,
        selectedSkuList: []
      },
      rules: {
        country: [{required: true, message: '请选择国家', trigger: 'change'}],
        weight: [{required: true, message: '请输入重量', trigger: 'blur'}],
      },
      rulesForSku: {
        country: [{required: true, message: '请选择国家', trigger: 'change'}],
        sku: [{required: true, message: '请选择SKU', trigger: 'change'}],
      },
      url: {
        countries: "/business/logisticChannel/countries",
        skus: "/business/sku/all",
        shipSelect: "/business/logisticChannel/find",
        shipSelectBySku: "/business/logisticChannel/findBySku"
      },
      pageDisable: true,
      quantity: 1
    }
  },

  created() {
    getAction(this.url.countries)
      .then(res => {
        this.countries = res.result
        this.pageDisable = false
      })
    getAction(this.url.skus).then(res => {
      console.log(res)
      this.skus = res.result
    })

  },

  methods: {
    getCountries() {
      getAction(self.url.shipSelect, self.form)
    },
    handleSubmit() {
      let self = this
      this.$refs.searchForm.validate(
        (valid) => {
          if (valid) {
            let requestParam = {
              country: self.form.country,
              weight: self.form.weight,
              volume: self.form.long * self.form.width * self.form.height
            }
            getAction(self.url.shipSelect, requestParam)
              .then(res => {
                self.priceList = res.result
                console.log(res.result)
              })
          }
        }
      )
    },

    handleAdd() {
      console.log("Click on add")
      console.log(this.formForSku.sku, "quantity", this.quantity)
      // exit in case of null or undefined
      if (this.formForSku.sku === undefined) {
        return
      }
      let i = this.formForSku.sku
      let data = this.skus
      this.formForSku.selectedSkuList.push({
        sku: data[i],
        quantity: this.quantity
      })
      console.log("Selected sku:", this.formForSku.selectedSkuList)
    },

    handleClose(index) {
      console.log("Deleting index", index)
      delete this.formForSku.selectedSkuList[index]
      console.log("after delete Selected sku:", this.formForSku.selectedSkuList)
    },

    handleSkuSubmit() {
      console.log("click on submit")
      let self = this
      this.$refs.searchFormBySku.validate(
        (valid) => {
          if (valid) {
            const requestParam = {
              country: self.formForSku.country,
              skuList: self.formForSku.selectedSkuList.map(
                (item) => ({
                  ID: item.sku.id,
                  quantity: item.quantity
                })),
            }
            console.log("Sending request with param")
            console.log(requestParam)
            postAction(self.url.shipSelectBySku, requestParam)
              .then(res => {
                self.priceList = res.result
                console.log(res.result)
              })
          }
        }
      )
    },
    filterOption(input, option) {
      return (
        option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
    }
  }

}
</script>

<style scoped>

</style>