<template>
  <a-card :bordered="false" :loading="!pageReady">
    <!-- search by sku's measure-->
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
        <a-button type="primary" htmlType="submit" @click="sendReqForSearchBySize" :loading="submitButtonLoading">
          搜索
        </a-button>
      </a-form-model-item>
    </a-form-model>
    <a-divider/>
    <!--  search by countries and sku  -->
    <a-form-model ref="searchFormBySku" :model="skuAndCountryForm" layout="inline">
      <!-- select countries -->
      <a-form-model-item label="目的地" prop="country">
        <div>
          <a-space>
            <a-select class="select-country"
                      show-search
                      placeholder="输入国家选择"
                      option-filter-prop="children"
                      :filter-option="filterOption"
                      style="width:200px"
                      v-model="skuAndCountryForm.country.selectedKey"
            >
              <a-select-option :value="item.code" v-for="item in skuAndCountryForm.country.sortedCandidates"
                               :key="item.code">
                {{ item.code + "-" + item.name_zh + "-" + item.name_en }}
              </a-select-option>
            </a-select>

            <a-button @click="addSelectedCountry">Add</a-button>
          </a-space>

          <div class="pool">
            <a-tag
              v-for="item in skuAndCountryForm.country.sortedPool"
              :key="item.code"
              :closable="true"
              @close="() => deleteSelectedCountry(item.code)"
              color="cyan"
            >
              {{ item["name_en"] }} - {{ item["name_zh"] }} - {{ item["code"] }}
            </a-tag>
          </div>
        </div>
      </a-form-model-item>

      <!-- select sku  -->
      <a-form-model-item label="sku" prop="sku">
        <a-space>
          <a-select
            show-search
            placeholder="输入sku选择"
            option-filter-prop="children"
            :filter-option="filterOption"
            style="width:300px"
            v-model="skuAndCountryForm.sku.selectedKey">
            <a-select-option v-for="item in skuAndCountryForm.sku.candidates" :key="item.id" :value="item.id">
              {{ item["erpCode"] + "-" + item.zhName }}
            </a-select-option>
          </a-select>

          <a-button @click="addSelectedSku">
            Add
          </a-button>
        </a-space>

        <div class="pool" style="width: 400px">
          <div v-for="item of skuAndCountryForm.sku.pool" :key="item.id" style="display: block">
            <a-tag
              :closable="true"
              @close="() => deleteSelectedSku(item.id)"
              color="orange"
            >
              {{ item.zhName + "-" + item.erpCode }}
            </a-tag>
            <a-input-number :value=" skuAndCountryForm.sku.poolKeyToQuantity.get(item.id)"
                            :min="1"
                            @change="(quantity)=>updateSkuPoolQuantity(item.id, quantity)">

            </a-input-number>
          </div>
        </div>

      </a-form-model-item>

      <a-form-model-item>
        <a-button
          type="primary"
          htmlType="submit"
          @click="sendReqByCountriesAndSkus"
          :loading="submitButtonLoading"
        >
          搜索
        </a-button>
      </a-form-model-item>

    </a-form-model>
    <a-divider/>
    <!--  data display table  -->
    <a-table :columns="columns"
             :data-source="priceList"
             :rowKey="(record)=>record['countryCode'] + record['logisticsChannelName']"
             :loading="dataTableLoading"
             bordered>
    </a-table>


  </a-card>
</template>

<script>
import {getAction, postAction} from "@/api/manage";
import {FormModel} from 'ant-design-vue';
import Vue from "vue";
import {CandidatePool} from "@comp/CandidatePool";

Vue.use(FormModel)
const columns = [
  {
    title: "国家",
    align: "center",
    dataIndex: "countryCode",
    width: 80,
    sorter: (a, b) => a['countryCode'].localeCompare(b['countryCode'])
  },
  {
    title: "渠道名称",
    align: "center",
    dataIndex: "logisticsChannelName",
    width: 180,
  },
  {
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
      skuAndCountryForm: {
        country: {
          selectedKey: undefined,
          candidatePool: undefined,
          sortedCandidates: [],
          sortedPool: []
        },
        sku: {
          // map between id and object
          allSku: new Map(),
          selectedKey: undefined,
          candidatePool: undefined,
          candidates: [],
          pool: [],
          poolKeyToQuantity: new Map()
        }
      },
      rules: {
        country: [{required: true, message: '请选择国家', trigger: 'change'}],
        weight: [{required: true, message: '请输入重量', trigger: 'blur'}],
      },
      url: {
        countries: "/business/logisticChannel/countries",
        popularCountries: "/business/logisticChannel/popularCountries",
        skus: "/business/sku/all",
        shipSelect: "/business/logisticChannel/find",
        shipSelectBySku: "/business/logisticChannel/findBySku",
        shipSelectByCountriesAndSku: "/business/logisticChannel/findByCountriesAndSku"
      },
      countryReady: false,
      skuReady: false,
      popularCountryReady: false,
      submitButtonLoading: false,
      dataTableLoading: false
    }
  },

  created() {
    // load country list
    getAction(this.url.countries)
      .then(res => {
        this.countries = res.result
        this.countryReady = true
      })
    // load sku list
    getAction(this.url.skus)
      .then(res => {
        res.result.forEach(item => {
          this.skuAndCountryForm.sku.allSku.set(item['id'], item)
        })
        this.skuAndCountryForm.sku.candidatePool = new CandidatePool(res.result, (item) => item.id)
        this.updateSkuCandidateAndPool()
        this.skuReady = true
      })
    // load country list sorted by its popularity, different API than the first country list
    getAction(this.url.popularCountries)
      .then(res => {
          console.log(res)
          let allPopularCountriesList = res.result
          // convert to map between code and country object
          this.skuAndCountryForm.country.candidatePool = new CandidatePool(
            allPopularCountriesList,
            (country) => country.code
          )
          // add the most popular country to selected key
          this.skuAndCountryForm.country.candidatePool.addAll(
            allPopularCountriesList.splice(0, 5).map(item => item.code)
          )
          this.updateSelectedSortedCountryList()
          this.updateUnselectedSortedCountryList()
          this.popularCountryReady = true
        }
      )
  },

  computed: {
    pageReady() {
      return this.skuReady && this.countryReady && this.popularCountryReady
    }
  },


  methods: {
    /********************** search by size ***********************/
    sendReqForSearchBySize() {
      let self = this
      this.$refs.searchForm.validate(
        (valid) => {
          if (valid) {
            let requestParam = {
              country: self.form.country,
              weight: self.form.weight,
              volume: self.form.long * self.form.width * self.form.height
            }
            this.dataTableLoading = true
            this.submitButtonLoading = true
            getAction(self.url.shipSelect, requestParam)
              .then(res => {
                self.priceList = res.result
                console.log(res.result)
                this.dataTableLoading = false
                this.submitButtonLoading = false
              })
          }
        }
      )
    },
    /************************** search by multiple countries and skus ***************************/
    /************************** Country Part ***********************************/
    updateSelectedSortedCountryList() {
      let form = this.skuAndCountryForm
      const l = Array.from(form.country.candidatePool.getPool())
      l.sort((a, b) => (a.quantity - b.quantity))
      l.reverse()
      this.skuAndCountryForm.country.sortedPool = l
      console.log("selected country after update", l)
    },

    updateUnselectedSortedCountryList() {
      let form = this.skuAndCountryForm
      const l = Array.from(form.country.candidatePool.getCandidates())
      l.sort((a, b) => (a.quantity - b.quantity))
      l.reverse()
      this.skuAndCountryForm.country.sortedCandidates = l
      console.log("unselected country after update", l)
    },

    addSelectedCountry() {
      let key = this.skuAndCountryForm.country.selectedKey
      this.skuAndCountryForm.country.candidatePool.add(key)
      this.updateSelectedSortedCountryList()
      this.updateUnselectedSortedCountryList()
      this.skuAndCountryForm.country.selectedKey = undefined
    },

    deleteSelectedCountry(key) {
      this.skuAndCountryForm.country.candidatePool.remove(key)
      this.updateSelectedSortedCountryList()
      this.updateUnselectedSortedCountryList()
    },
    /************************** SKU Part ***********************************/
    addSelectedSku() {
      let form = this.skuAndCountryForm.sku
      let key = form.selectedKey
      console.log("Adding a sku", key)
      // exit in case of null or undefined
      if (key === undefined) {
        return
      }
      form.candidatePool.add(key)
      // update quantity
      form.poolKeyToQuantity.set(key, 1)
      this.updateSkuCandidateAndPool()
      form.selectedKey = undefined
    },

    deleteSelectedSku(key) {
      console.log("Deleting index", key)
      let form = this.skuAndCountryForm.sku
      form.candidatePool.remove(key)
      // update quantity
      form.poolKeyToQuantity.delete(key)
      this.updateSkuCandidateAndPool()
    },

    /**
     * Update data source that used by sku selection widget to refresh UI,
     * sorted by sku's ERP code
     */
    updateSkuCandidateAndPool() {
      let sortByERP = (a, b) => a['erpCode'] - b['erpCode']
      let form = this.skuAndCountryForm.sku
      form.candidates = Array.from(form.candidatePool.getCandidates()).sort(sortByERP)
      form.pool = Array.from(form.candidatePool.getPool()).sort(sortByERP)
    },

    updateSkuPoolQuantity(key, quantity) {
      console.log({key, quantity})
      let form = this.skuAndCountryForm.sku
      form.poolKeyToQuantity.set(key, quantity)
      let backup = form.pool
      form.pool = null
      form.pool = backup
      console.log(this.skuAndCountryForm.sku.poolKeyToQuantity)
    },

    /**
     * Send request to remote API
     */
    sendReqByCountriesAndSkus() {
      console.log("click on submit")
      let self = this
      let form = this.skuAndCountryForm
      this.$refs.searchFormBySku.validate(
        (valid) => {
          if (valid) {
            this.dataTableLoading = true
            this.submitButtonLoading = true
            const requestParam = {
              countries: form.country.sortedPool.map(country => country.code),
              skuQuantities: form.sku.pool.map(
                (sku) => ({
                  ID: sku.id,
                  quantity: form.sku.poolKeyToQuantity.get(sku.id)
                })),
            }
            if (requestParam.countries.length === 0) {
              this.$message.warn("Please select destination country")
              return
            }
            if (requestParam.skuQuantities.length === 0) {
              this.$message.warn("Please select sku")
              return;
            }
            console.log("Sending request with param")
            console.log(requestParam)
            postAction(self.url.shipSelectByCountriesAndSku, requestParam)
              .then(res => {
                self.priceList = res.result
                console.log(res.result)
                this.dataTableLoading = false
                this.submitButtonLoading = false
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
    onClickTestButton() {
      this.test_data = "click on test button"
    }
  }

}
</script>

<style scoped>

.pool {
  border: solid darkgray 2px;
  border-radius: 5px;
  width: 500px;
  min-height: 80px;
}

.select-country {
  display: inline-block;
}
</style>