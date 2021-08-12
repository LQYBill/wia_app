<template>
  <div>
    <LogisticProfitTextCard @range="updateRange"/>
    <a-row :gutter="24">
      <a-col :span="12">
        <ExpensePiePerChannel :range="dateRange"/>
      </a-col>
      <a-col :span="12">
        <ExpensePiePerCountry :range="dateRange"/>
      </a-col>

    </a-row>
  </div>
</template>

<script>
import TextCard from "@views/business/admin/analysis/logistic/profit/MonthlyProfit";
import ExpensePiePerChannel from "@views/business/admin/analysis/logistic/expense/ExpensePiePerChannel";
import ExpensePiePerCountry from "@views/business/admin/analysis/logistic/expense/ExpensePiePerCountry";


export default {
  name: "Analysis",
  components:{
    LogisticProfitTextCard: TextCard,
    ExpensePiePerChannel,
    ExpensePiePerCountry
  },
  data: function() {
    return {
      dateRange: null
    }
  },
  methods: {
    updateRange(range) {
      this.dateRange = range
    }
  }
}
</script>

<style scoped>

</style>