<template>
  <j-modal
    :title="title"
    :width="1200"
    :visible="visible"
    :maskClosable="false"
    switchFullscreen
    @ok="handleOk"
    :okButtonProps="{ class:{'jee-hidden': disableSubmit} }"
    @cancel="handleCancel">
    <Detail ref="content" :order-ids="dataForChild"/>
  </j-modal>
</template>

<script>
import Detail from './Detail'

export default {
  name: 'ClientPlatformOrderDetailContainer',
  components: {
    Detail
  },
  data() {
    return {
      title: '',
      visible: false,
      disableSubmit: false
    }
  },
  props:{
    dataForChild:Array
  },
  methods: {
    display() {
      this.visible = true
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