<template>
  <div class="app_container">
    <div class="change-locale" id="language_picker">
      <span>Change language: </span>
      <a-radio-group :default-value="en_US" @change="changeLocale">
        <a-radio-button key="en" :value="en_US">
          English
        </a-radio-button>
        <a-radio-button :default-checked='false' key="cn" :value="zh_CN">
          中文
        </a-radio-button>
      </a-radio-group>

      <!--------------------------------------------->
      <div id='langMenuArrow' @click='toggleLocaleMenu'><i></i></div>


    </div>
    <a-config-provider :locale="locale" @language-event='changeLocale' >
      <div id="app">
        <router-view/>
      </div>
    </a-config-provider>
  </div>
</template>
<script>
  import en_US from 'ant-design-vue/lib/locale-provider/en_US'
  import zh_CN from 'ant-design-vue/es/locale/zh_CN';
  import moment from 'moment';
  import 'moment/locale/zh-cn';
  import enquireScreen from '@/utils/device'
  import i18n from '@/i18n'
  import Vue from 'vue'
  import { USER_LANG } from '@/store/mutation-types'

  export default {
    data () {
      return {
        locale: en_US,
        moment,
        en_US,
        zh_CN,
        isActive: false,
        url: {

        }
      }
    },
    created () {
      let that = this;
      enquireScreen(deviceType => {
        // tablet
        if (deviceType === 0) {
          that.$store.commit('TOGGLE_DEVICE', 'mobile')
          that.$store.dispatch('setSidebar', false)
        }
        // mobile
        else if (deviceType === 1) {
          that.$store.commit('TOGGLE_DEVICE', 'mobile')
          that.$store.dispatch('setSidebar', false)
        }
        else {
          that.$store.commit('TOGGLE_DEVICE', 'desktop')
          that.$store.dispatch('setSidebar', true)
        }
      });
    }, //end of created
    methods: {
      changeLocale(e) {
        this.locale = e.target.value;
        console.log(e.target.value);
        console.log("this locale : " + this.locale);
        console.log("i18n locale "+i18n.locale);
        switch (e.target.value.locale) {
          case "zh-cn":
            i18n.locale = 'zh_CN';
            break;
          case "en":
            i18n.locale = 'en_US';
            break;
          default:
            console.log("what did you say ?");
            break;
        }
      },
      toggleLocaleMenu() {
        this.isActive = !this.isActive;
        let element = document.getElementById("language_picker");
        if(this.isActive) {
          element.classList.add("open");
        }
        else{
          element.classList.remove("open");
        }
      }
    }
  }

export function changeLocale(e) {
  this.locale = e.target.value;
  console.log(this.locale);
  console.log(i18n.locale);
  switch (e.target.value) {
    case this.zh_CN:
      i18n.locale = 'zh_CN';
      break;
    case this.en_US:
      i18n.locale = 'en_US';
      break;
    default:
      console.log("what did you say ?");
      break;
  }
}
</script>
<style>
  #app {
    height: 100%;
  }
  .app_container {
    position: relative;
  }
  #language_picker {
    z-index: 20;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    position: absolute;
    color: white;
    background:  rgb(11, 73, 166);
    width: 200px;
    height: 100px;
    left: 50%;
    margin: 0 0 0 -100px;
    padding: 0.5em 0 0;
    top: -90px;
    transform: translateY(0.5em);
    transition: all 0.5s ease-in-out;
    border-radius: 0 0 0.5em 0.5em;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
  #language_picker.open {
    top: 0;
    transform: translateY(0);
  }
  #language_picker #langMenuArrow {
    width: 100%;
    text-align: center;
    padding: 0.5em 0;
    border-radius: 0 0 0.5em 0.5em;
  }
  #language_picker #langMenuArrow:hover {
    background: rgba(0,0,0,0.2);
  }
  #language_picker #langMenuArrow > i{
    border: solid white;
    border-width: 0 2px 2px 0;
    display: block;
    margin: auto;
    padding: 3px;
    transform: rotate(45deg);
    width: 5px;
    height: 5px;
  }
  #language_picker.open #langMenuArrow > i{
     transform: rotate(-135deg) translate(-2px, -2px);
   }
</style>