<template>
  <div class="home">
    <div class="banner">
      <a-divider>
        <a-avatar className="avatar" size="large" :src="getAvatar()" />
      </a-divider>
      <h1 style="margin-top: 1rem">{{ $t('welcome') }} WIA App<br>
        <span style="color: #0c8fcf">{{ nickname() }}</span>
      </h1>
    </div>
    <a-form layout="inline" ref="searchForm">
      <a-row :gutter="24">
        <a-col
          :md="6"
          :sm="8"
        >
          <a-form-item
            :label="$t('startDate')"
            :labelCol="{span: 5}"
            :wrapperCol="{span: 14}"
          >
            <a-range-picker :locale='locale' />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getFileAccessHttpUrl } from '@api/manage'
import { userInfo } from 'os'
import Vue from 'vue'
import { USER_LANG } from '@/store/mutation-types';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import en_US from 'ant-design-vue/lib/locale-provider/en_US';
import App from '@/App.vue'

export default {
  name: 'Analysis',
  computed: {
  },
  components: {},
  data() {
    return {
      locale,
      en_US,
      language: en_US,
    }
  },
  created() {
    console.log(Vue.ls.get(USER_LANG));
  },
  methods: {
    userInfo,
    ...mapGetters(["nickname", "avatar", "userInfo"]),
    getAvatar() {
      return getFileAccessHttpUrl(this.avatar())
    },
  }
}
</script>

<style scoped>
.home {
  width: 80%;
  margin: 0 auto;
  padding: 25px 0;
}

.home > .banner {
  text-align: center;
  padding: 25px 0;
  margin: 25px 0;
}
</style>
