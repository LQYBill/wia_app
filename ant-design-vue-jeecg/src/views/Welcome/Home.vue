<template>
  <div class="home">
    <div class="banner">
      <a-divider><a-avatar class="avatar" size="large" :src="getAvatar()"/></a-divider>
      <h1 style="margin-top: 1rem">Welcome to WIA App<br>
      <span style ="color: #0c8fcf">{{ nickname() }}</span>
      </h1>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getFileAccessHttpUrl } from '@api/manage'

export default {
  name: 'Home',
  components: {
  },
  data () {
    return {
      targetTime: new Date().getTime() + 3900000
    }
  },
  methods: {
    ...mapGetters(["nickname", "avatar","userInfo"]),
    getAvatar(){
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
