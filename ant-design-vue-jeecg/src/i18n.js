
// configuration of internationalization

import Vue from 'vue'
import VueI18n from 'vue-i18n'
Vue.use(VueI18n)

const i18n = new VueI18n({
  locale: "en_US",
  fallbackLocale: 'zh_CN',
  messages: {
    zh_CN: require('@comp/lang/zh-CN.json'),
    en_US: require('@comp/lang/en-US.json')
  }
})
export default i18n;