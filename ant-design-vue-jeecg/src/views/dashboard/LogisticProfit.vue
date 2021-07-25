<template>
  <a-card :loading="loading" :bordered="false" title="物流利润" :style="{ marginTop: '24px' }">
    <a-row>
      <a-col :span="3">
        <a-statistic title="本月开票" :value="112893" suffix="单"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="应收 (EUR)" value="100" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="应收 (CNY)" value="100" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (EUR)" :value="112893" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (CNY)" :value="112893" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="盈利" :value="112893" suffix="¥"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="利润率" :value="50" suffix="%"/>
      </a-col>
    </a-row>
    <a-divider/>
    <a-row>
      <a-col :span="3">
        <a-statistic title="未开票" :value="112893" suffix="单"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (EUR)" :value="112893" suffix="€"/>
      </a-col>
      <a-col :span="3">
        <a-statistic title="实际开销 (CNY)" :value="112893" suffix="¥"/>
      </a-col>
    </a-row>
    <a-divider/>

    <BarMultid
      title="本月实际开销与应收物流费用"
      :data-source="[{type:'实际开销', '1':10, '2':12}, {type: '应收物流费用','1':20, '2':22}]"
      :fields="Array.from({length: 31}, (v, i) => i + 1+'')"
    >

    </BarMultid>
  </a-card>

</template>

<script>
import BarMultid from "@comp/chart/BarMultid";

export default {
  name: "LogisticProfit",
  components:{
    BarMultid
  },
  data(){
    return {
      url
    }
  }
}
</script>

<style scoped>

</style>