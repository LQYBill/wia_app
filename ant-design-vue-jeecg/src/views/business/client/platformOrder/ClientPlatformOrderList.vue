<template>
  <a-tabs default-active-key="1" @change="callback">
    <a-tab-pane key="1">
      <span slot="tab">
        Pending orders <a-badge :count="109" :number-style="{ backgroundColor: '#fa541c' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      <pending-order-list/>
    </a-tab-pane>
    <a-tab-pane key="2" force-render>
      <span slot="tab">
        Purchasing orders <a-badge count="109" :number-style="{ backgroundColor: '#faad14' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      Content of Tab Pane 2
    </a-tab-pane>
    <a-tab-pane key="3">
      <span slot="tab">
        Processed orders <a-badge count="109" :number-style="{ backgroundColor: '#a0d911' }" :overflow-count="999" style='margin-bottom: 5px'/>
      </span>
      Content of Tab Pane 3
    </a-tab-pane>
  </a-tabs>
</template>

<script>

import PendingOrderList from './PendingOrderList'

import '@assets/less/TableExpand.less'

export default {
  name: 'PlatformOrderList',
  components: {
    PendingOrderList
  },
  data() {
    return {
      description: 'Client platform order page',
    }
  },
  created() {

  },
  computed: {},
  methods: {
    callback(key) {
      console.log(key);
    }
  }
}
</script>