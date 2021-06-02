<template>
  <div>
    <a-modal
      v-model="visible"
      title="Client Agreement"
      @ok="handleOk"
      width="1000px"
      okText="Agree"
    >
      <p>
        To use our service, you have to consent our
        <span>General Condition of Collaboration</span>.
      </p>
      <iframe name="pdfFrame" class="pdfIframe">
        pdf
      </iframe>
      <p>Clicking on Agree means you agree to our terms.</p>

    </a-modal>

  </div>
</template>
<script>
import {getAction} from "@api/manage";
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
    okCallback: {
      type: Function,
      required: true
    }
  },
  methods: {
    loadDoc() {
      axios({
        url: this.url.cgs,
        method: 'get',
        responseType: 'arraybuffer',
      }).then(
        data => {
          let file = new Blob([data], {type: 'application/pdf'});
          let fileURL = URL.createObjectURL(file);
          window.open(fileURL, "pdfFrame");
        }
      )

    },
    showModal() {
      this.visible = true;
      this.loadDoc()
    },
    handleOk(e) {
      console.log(e);
      getAction(this.url.agreeCgs).then(
        res => {
          console.log("sent request")
          this.okCallback();
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

<style scoped>
.pdfIframe {
  display: block;
  width: 95%;
  height: 500px;
  margin: 0 auto;
}
</style>
