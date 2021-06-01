<template>
  <div>
    <a-modal v-model="visible" title="Basic Modal" @ok="handleOk">
      <p>
        To use our service, you have to consent our
        <a @click="openCDG">General Condition of Collaboration</a>.
      </p>
      <p>Clicking ok means you agree to our terms.</p>
    </a-modal>
  </div>
</template>
<script>
import {saveAs} from 'file-saver';
import {getFile, postAction, getAction} from "@api/manage";
import {axios} from '@/utils/request'

export default {
  data() {
    return {
      visible: false,
      url: {
        cgs: "sys/getCGS",
        agreeCgs: "sys/agreeCGS"
      }
    };
  },
  props: {
    onClickOk: {
      type: Function,
      required: true
    }
  },
  methods: {
    showModal() {
      this.visible = true;
    },
    handleOk(e) {
      console.log(e);
      getAction(this.url.agreeCgs).then(
        res => {
          console.log("sent request")
          this.onClickOk();
          this.visible = false;
        }
      )


    },
    openCDG() {
      axios({
        url: this.url.cgs,
        method: 'get',
        responseType: 'arraybuffer',
      }).then(
        data => {
          var file = new Blob([data], {type: 'application/pdf'});
          var fileURL = URL.createObjectURL(file);
          window.open(fileURL);
        }
      )
    }
  },
};
</script>
