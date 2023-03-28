<template>
  <a-table
    rowKey="id"
    size="middle"
    bordered
    :loading="loading"
    :columns="columns"
    :dataSource="dataSource"
    :pagination="false"
  >

    <template slot="htmlSlot" slot-scope="text">
      <div v-html="text"></div>
    </template>

    <template slot="imgSlot" slot-scope="text">
      <div style="font-size: 12px;font-style: italic;">
        <span v-if="!text">无图片</span>
        <img v-else :src="getImgView(text)" alt="" style="max-width:80px;height:25px;"/>
      </div>
    </template>

    <template slot="fileSlot" slot-scope="text">
      <span v-if="!text" style="font-size: 12px;font-style: italic;">无文件</span>
      <a-button
              v-else
              ghost
              type="primary"
              icon="download"
              size="small"
              @click="downloadFile(text)"
      >
        <span>下载</span>
      </a-button>
    </template>

  </a-table>
</template>

<script>
  import { getAction } from '@api/manage'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'

  export default {
    name: 'PlatformOrderContentSubTable',
    mixins: [JeecgListMixin],
    props: {
      record: {
        type: Object,
        default: null,
      }
    },
    data() {
      return {
        description: '平台订单内容内嵌列表',
        disableMixinCreated: true,
        loading: false,
        dataSource: [],
        columns: [
          {
            title: this.$t("invoice.platformOrderID"),
            align: 'center',
            dataIndex: 'platformOrderId_dictText'
          },
          {
            title: 'SKU ID',
            align: 'center',
            dataIndex: 'skuId_dictText'
          },
          {
            title: this.$t("invoice.skuQty"),
            align: 'center',
            dataIndex: 'quantity',
          },
          {
            title: this.$t("invoice.purchaseFee"),
            align: 'center',
            dataIndex: 'purchaseFee',
          },
          {
            title: this.$t("invoice.shippingFee"),
            align: 'center',
            dataIndex: 'shippingFee',
          },
          {
            title: this.$t("invoice.serviceFee"),
            align: 'center',
            dataIndex: 'serviceFee',
          },
          {
            title: this.$t("invoice.vat"),
            align: 'center',
            dataIndex: 'vat',
          },
          {
            title: this.$t("invoice.skuPurchaseStatus"),
            align: 'center',
            dataIndex: 'status_dictText',
          },
          {
            title: this.$t("invoice.erpStatus"),
            align: 'center',
            dataIndex: 'erpStatus',
          },
          {
            title: this.$t("invoice.productAvailable") + '(1='+this.$t("yes")+'，0='+this.$t("no")+'）',
            align: 'center',
            dataIndex: 'productAvailable',
          },
        ],
        url: {
          listByMainId: '/business/platformOrder/queryPlatformOrderContentByMainId',
        },
      }
    },
    watch: {
      record: {
        immediate: true,
        handler() {
          if (this.record != null) {
            this.loadData(this.record)
          }
        }
      }
    },
    methods: {

      loadData(record) {
        this.loading = true
        this.dataSource = []
        getAction(this.url.listByMainId, {
          id: record.id
        }).then((res) => {
          if (res.success) {
            this.dataSource = res.result.records
          }
        }).finally(() => {
          this.loading = false
        })
      },

    },
  }
</script>

<style scoped>

</style>
