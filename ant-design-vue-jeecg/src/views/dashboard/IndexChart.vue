<template>
  <div class="page-header-index-wide">
    <a-row :gutter="24">
      <a-col :sm="24" :md="12" :xl="6" :style="{ marginBottom: '24px' }">
        <chart-card :loading="loading" title="订单量To be translated" :total="8846 | NumberFormat">
          <a-tooltip title="指标说明To be translated" slot="action">
            <a-icon type="info-circle-o"/>
          </a-tooltip>
          <div>
            <mini-area/>
          </div>
          <template slot="footer">日订单量To be translated<span> {{ '1234' | NumberFormat }}</span></template>
        </chart-card>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <LogisticProfitTextCard/>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="12">
        <InvoicedChart/>
      </a-col>
      <a-col :span="12">
        <UninvoicedChart/>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="12">
        <ExpensePie/>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import ChartCard from '@/components/ChartCard'
import ACol from "ant-design-vue/es/grid/Col"
import ATooltip from "ant-design-vue/es/tooltip/Tooltip"
import MiniArea from '@/components/chart/MiniArea'
import MiniBar from '@/components/chart/MiniBar'
import MiniProgress from '@/components/chart/MiniProgress'
import RankList from '@/components/chart/RankList'
import Bar from '@/components/chart/Bar'
import LineChartMultid from '@/components/chart/LineChartMultid'
import HeadInfo from '@/components/tools/HeadInfo.vue'

import Trend from '@/components/Trend'
import {getLoginfo, getVisitInfo} from '@/api/api'
import TextCard from "@views/dashboard/logistic/profit/TextCard";
import InvoicedChart from "@views/dashboard/logistic/profit/InvoicedChart";
import UninvoicedChart from "@views/dashboard/logistic/profit/UninvoicedChart";
import ExpensePie from "@views/dashboard/logistic/expense/ExpensePie";

const rankList = []
for (let i = 0; i < 7; i++) {
  rankList.push({
    name: '白鹭岛 ' + (i + 1) + ' 号店',
    total: 1234.56 - i * 100
  })
}
const barData = []
for (let i = 0; i < 12; i += 1) {
  barData.push({
    x: `${i + 1}月`,
    y: Math.floor(Math.random() * 1000) + 200
  })
}
export default {
  name: "IndexChart",
  components: {
    ExpensePie,
    ATooltip,
    ACol,
    ChartCard,
    MiniArea,
    MiniBar,
    MiniProgress,
    RankList,
    Bar,
    Trend,
    LineChartMultid,
    HeadInfo,
    LogisticProfitTextCard: TextCard,
    InvoicedChart,
    UninvoicedChart
  },
  data() {
    return {
      loading: true,
      center: null,
      rankList,
      barData,
      loginfo: {},
      visitFields: ['ip', 'visit'],
      visitInfo: [],
      indicator: <a-icon type="loading" style="font-size: 24px" spin/>,
      orderNumber: 0,
      url: {
        monthOrderQuantity: "/business/platformOrder/monthOrderQuantity"
      }
    }
  },
  created() {
    setTimeout(() => {
      this.loading = !this.loading
    }, 1000)
    this.initLogInfo();
  },
  methods: {
    initLogInfo() {
      getLoginfo(null).then((res) => {
        if (res.success) {
          Object.keys(res.result).forEach(key => {
            res.result[key] = res.result[key] + ""
          })
          this.loginfo = res.result;
        }
      })
      getVisitInfo().then(res => {
        if (res.success) {
          this.visitInfo = res.result;
        }
      })
    },
  }
}
</script>

<style lang="less" scoped>
.circle-cust {
  position: relative;
  top: 28px;
  left: -100%;
}

.extra-wrapper {
  line-height: 55px;
  padding-right: 24px;

  .extra-item {
    display: inline-block;
    margin-right: 24px;

    a {
      margin-left: 24px;
    }
  }
}

/* 首页访问量统计 */
.head-info {
  position: relative;
  text-align: left;
  padding: 0 32px 0 0;
  min-width: 125px;

  &.center {
    text-align: center;
    padding: 0 32px;
  }

  span {
    color: rgba(0, 0, 0, .45);
    display: inline-block;
    font-size: .95rem;
    line-height: 42px;
    margin-bottom: 4px;
  }

  p {
    line-height: 42px;
    margin: 0;

    a {
      font-weight: 600;
      font-size: 1rem;
    }
  }
}
</style>