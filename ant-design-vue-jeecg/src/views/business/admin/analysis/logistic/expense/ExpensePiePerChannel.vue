<template>
  <a-card :loading="!view.ready" :bordered="false" :title="view.title" :style="{ marginTop: '24px' }">
    <pie title="饼图" :height="view.height" :data-source="view.chart.dataSource"/>
  </a-card>
</template>

<script>
import {getAction} from "@api/manage";
import Pie from '@comp/chart/Pie'


export default {
  name: "ExpensePie",
  components: {
    Pie
  },
  props: {
    range: {
      type: Array
    },
    country: {
      type: Array
    },
    channel: {
      type: Array
    }
  },
  watch: {
    range: function() {
      this.param.startDate = this.range[0]
      this.param.endDate = this.range[1]
      this.loadModel().then(this.prepareView)
    },
    country: function() {
      this.param.country = this.country
    },
    channel: function() {
      this.param.channel = this.channel
    }
  },
  data: function () {
    return {
      model: {
        url: {
          logisticExpenseProportion: "/business/logisticExpenseDetail/expenseProportionByChannel"
        },
        data: []
      },
      view: {
        ready: false,
        title: "物流开销占比（渠道）",
        height:300,
        chart: {
          dataSource: []
        },
      },
      param: {
        startDate: null,
        endDate: null,
        country: null,
        channel: null
      }
    }
  },
  created() {
    this.loadModel().then(this.prepareView)
  },
  methods: {
    loadModel() {
      this.view.ready = false
      return getAction(this.model.url.logisticExpenseProportion, this.param)
        .then(
          res => {
            this.model.data = res['result']
          }
        )
    },
    prepareView() {
      this.view.chart.dataSource = this.model.data.map(
        element => ({
          item: element["name"],
          count: element["expense"]
        })
      )
      this.view.ready = true
    }
  }

}
</script>

<style scoped>

</style>