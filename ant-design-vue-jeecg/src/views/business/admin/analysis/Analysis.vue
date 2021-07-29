<template>
  <div>
    <LogisticProfitTextCard/>
    <a-row :gutter="24">
      <a-col :span="12">
        <ExpensePiePerChannel/>
      </a-col>
      <a-col :span="12">
        <ExpensePiePerCountry/>
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
  }
}
</script>

<style scoped>

</style>