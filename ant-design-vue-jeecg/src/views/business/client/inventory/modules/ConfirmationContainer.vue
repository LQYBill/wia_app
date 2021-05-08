<template>
  <j-modal
    :title="title"
    :width="1200"
    :visible="visible"
    :maskClosable="false"
    switchFullscreen
    ok-text="Confirm"
    @ok="handleOk"
    @cancel="handleCancel">
    <Confirmation ref="content" :sku-identifiers="dataForChild" :ok-callback="okCallback"/>
  </j-modal>
</template>

<script>
import Confirmation from './Confirmation'

export default {
  name: 'ClientPlatformOrderDetailContainer',
  components: {
    Confirmation
  },
  data() {
    return {
      title: '',
      visible: false,
    }
  },
  props: {
    dataForChild: Array,
    okCallback: Function
  },
  methods: {
    display() {
      this.visible = true
      console.log("container prop: " + this.dataForChild)
      /* child component does not finish rendering, one should call its methods at next tick */
      this.$nextTick(() => {
          this.$refs.content.loadData();
        }
      )
    },
    close() {
      this.$emit('close');
      this.visible = false;
    },
    handleOk() {
      this.$refs.content.confirmOrder();
      this.close()
    },
    handleCancel() {
      this.close()
    }
  }
}
</script>

<style scoped>
</style>