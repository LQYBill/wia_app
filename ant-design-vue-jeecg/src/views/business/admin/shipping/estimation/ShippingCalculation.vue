<template>
  <a-card :bordered="false">
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
            {{ item.code + "-" + item.zh_name + "-" + item.en_name }}
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

    <a-table :columns="columns" :data-source="priceList" rowKey="logisticsChannelName" bordered>
    </a-table>

  </a-card>
</template>

<script>
import {getAction} from "@/api/manage";
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
      form: {
        country: undefined,
        weight: undefined,
        long: null,
        width: null,
        height: null,
      },
      rules: {
        country: [{required: true, message: '请选择国家', trigger: 'change'}],
        weight: [{required: true, message: '请输入重量', trigger: 'blur'}],
      },
      url: {
        countries: "/business/logisticChannel/countries",
        shipSelect: "/business/logisticChannel/find"
      },

    }
  },

  created() {
    getAction(this.url.countries).then(res => {
      this.countries = res.result
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
    filterOption(input, option) {
      return (
          option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
    },
  }

}
</script>

<style scoped>

</style>