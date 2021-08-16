<template>
  <div>
    <LogisticProfitTextCard @range="updateRange" @country='updateCountry' @channel='updateChannel'/>
    <a-row :gutter="24">
      <a-col :span="12">
        <ExpensePiePerChannel :range="dateRange" :country="country" :channel="channel"/>
      </a-col>
      <a-col :span="12">
        <ExpensePiePerCountry :range="dateRange" :country="country" :channel="channel"/>
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
      dateRange: null,
      country: null,
      channel: null,
    }
  },
  methods: {
    updateRange(range) {
      this.dateRange = range
    },
    updateCountry(country) {
      this.country = country
    },
    updateChannel(channel) {
      this.channel = channel
    }
  }
}
</script>

<style scoped>

</style>