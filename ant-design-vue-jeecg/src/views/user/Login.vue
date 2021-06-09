<template>
  <div class="main">
    <a-form :form="form" class="user-layout-login" ref="formLogin" id="formLogin">
      <a-tabs
        :activeKey="customActiveKey"
        :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
        @change="handleTabClick">
        <a-tab-pane key="tab1" tab="账号密码登录">
          <a-form-item>
            <a-input
              size="large"
              v-decorator="['username',validatorRules.username,{ validator: this.handleUsernameOrEmail }]"
              type="text"
              placeholder="请输入帐户名">
              <a-icon slot="prefix" type="user" :style="{ color: 'rgba(0,0,0,.25)' }"/>
            </a-input>
          </a-form-item>

          <a-form-item>
            <a-input
              v-decorator="['password',validatorRules.password]"
              size="large"
              type="password"
              autocomplete="false"
              placeholder="密码">
              <a-icon slot="prefix" type="lock" :style="{ color: 'rgba(0,0,0,.25)' }"/>
            </a-input>
          </a-form-item>

        </a-tab-pane>
      </a-tabs>

      <a-form-item>
        <a-checkbox v-decorator="['rememberMe', {initialValue: true, valuePropName: 'checked'}]">自动登录</a-checkbox>
        <router-link :to="{ name: 'alteration'}" class="forge-password" style="float: right;">
          忘记密码
        </router-link>
        <router-link :to="{ name: 'register'}" class="forge-password" style="float: right;margin-right: 10px">
          注册账户
        </router-link>
      </a-form-item>

      <a-form-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="login-button"
          :loading="loginBtn"
          @click.stop.prevent="handleSubmit"
          :disabled="loginBtn">确定
        </a-button>
      </a-form-item>
    </a-form>

    <two-step-captcha
      v-if="requiredTwoStepCaptcha"
      :visible="stepCaptchaVisible"
      @success="stepCaptchaSuccess"
      @cancel="stepCaptchaCancel"></two-step-captcha>

    <login-select-tenant ref="loginSelect" @success="loginSelectOk"></login-select-tenant>
    <statement-cds ref="statement" :ok-callback="welcome"></statement-cds>

  </div>
</template>

<script>
//import md5 from "md5"
import TwoStepCaptcha from '@/components/tools/TwoStepCaptcha'
import {mapActions} from "vuex"
import {timeFix} from "@/utils/util"
import Vue from 'vue'
import {ACCESS_TOKEN, ENCRYPTED_STRING} from "@/store/mutation-types"
import {postAction, getAction} from '@/api/manage'
import {getEncryptedString} from '@/utils/encryption/aesEncrypt'
import ThirdLogin from './third/ThirdLogin'
import LoginSelectTenant from "./LoginSelectTenant";
import StatementCds from '@/components/cdsStatement/StatementCds'

export default {
  components: {
    LoginSelectTenant,
    TwoStepCaptcha,
    ThirdLogin,
    StatementCds
  },
  data() {
    return {
      customActiveKey: "tab1",
      loginBtn: false,
      // login type: 0 email, 1 username, 2 telephone
      loginType: 0,
      requiredTwoStepCaptcha: false,
      stepCaptchaVisible: false,
      form: this.$form.createForm(this),
      encryptedString: {
        key: "",
        iv: "",
      },
      state: {
        time: 60,
        smsSendBtn: false,
      },
      validatorRules: {
        username: {rules: [{required: true, message: '请输入用户名!'}, {validator: this.handleUsernameOrEmail}]},
        password: {rules: [{required: true, message: '请输入密码!', validator: 'click'}]},
        mobile: {rules: [{validator: this.validateMobile}]},
        captcha: {rule: [{required: true, message: '请输入验证码!'}]},
      },
      currentUsername: "",
      url: {
        checkCGS: "/sys/checkCGS"
      }
    }
  },
  created() {
    this.currdatetime = new Date().getTime();
    Vue.ls.remove(ACCESS_TOKEN)
    this.getRouterData();
    // update-begin- --- author:scott ------ date:20190805 ---- for:密码加密逻辑暂时注释掉，有点问题
    //this.getEncrypte();
    // update-end- --- author:scott ------ date:20190805 ---- for:密码加密逻辑暂时注释掉，有点问题
  },
  methods: {
    ...mapActions(['Login', 'Logout', 'PhoneLogin']),
    // handler
    handleUsernameOrEmail(rule, value, callback) {
      const regex = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\.[a-zA-Z0-9_-]{2,3}){1,2})$/;
      if (regex.test(value)) {
        this.loginType = 0
      } else {
        this.loginType = 1
      }
      callback()
    },
    handleTabClick(key) {
      this.customActiveKey = key
      // this.form.resetFields()
    },
    handleSubmit() {
      let that = this
      let loginParams = {};
      that.loginBtn = true;

      that.form.validateFields(['username', 'password', 'rememberMe'], {force: true}, (err, values) => {
        if (!err) {
          loginParams.username = values.username
          // update-begin- --- author:scott ------ date:20190805 ---- for:密码加密逻辑暂时注释掉，有点问题
          //loginParams.password = md5(values.password)
          //loginParams.password = encryption(values.password,that.encryptedString.key,that.encryptedString.iv)
          loginParams.password = values.password
          loginParams.remember_me = values.rememberMe
          // update-begin- --- author:scott ------ date:20190805 ---- for:密码加密逻辑暂时注释掉，有点问题
          console.log("登录参数", loginParams)
          that.Login(loginParams).then((res) => {
            this.$refs.loginSelect.show(res.result)
          }).catch((err) => {
            that.requestFailed(err);
          });
        } else {
          that.loginBtn = false;
        }
      })
    },
    getCaptcha(e) {
      e.preventDefault();
      let that = this;
      this.form.validateFields(['mobile'], {force: true}, (err, values) => {
          if (!values.mobile) {
            that.cmsFailed("请输入手机号");
          } else if (!err) {
            this.state.smsSendBtn = true;
            let interval = window.setInterval(() => {
              if (that.state.time-- <= 0) {
                that.state.time = 60;
                that.state.smsSendBtn = false;
                window.clearInterval(interval);
              }
            }, 1000);

            const hide = this.$message.loading('验证码发送中..', 0);
            let smsParams = {};
            smsParams.mobile = values.mobile;
            smsParams.smsmode = "0";
            postAction("/sys/sms", smsParams)
              .then(res => {
                if (!res.success) {
                  setTimeout(hide, 0);
                  this.cmsFailed(res.message);
                }
                console.log(res);
                setTimeout(hide, 500);
              })
              .catch(err => {
                setTimeout(hide, 1);
                clearInterval(interval);
                that.state.time = 60;
                that.state.smsSendBtn = false;
                this.requestFailed(err);
              });
          }
        }
      );
    },
    stepCaptchaSuccess() {
      this.loginSuccess()
    },
    stepCaptchaCancel() {
      this.Logout().then(() => {
        this.loginBtn = false
        this.stepCaptchaVisible = false
      })
    },
    loginSuccess() {
      let self = this
      getAction(this.url.checkCGS).then(
        res => {
          console.log("CGS: ", res)
          if (res === false) {
            self.$refs["statement"].showModal()
            this.loginBtn = false;
          }else{
            this.welcome()
          }
        }
      );
    },
    welcome() {
      this.$router.push({path: "/dashboard/analysis"}).catch(() => {
        console.log('登录跳转首页出错,这个错误从哪里来的')
      })
      this.$notification.success({
        message: '欢迎',
        description: `${timeFix()}，欢迎回来`,
      });
    },
    cmsFailed(err) {
      this.$notification['error']({
        message: "登录失败",
        description: err,
        duration: 4,
      });
    },
    requestFailed(err) {
      let errMsg = ((err.response || {}).data || {}).message || err.message || "请求出现错误，请稍后再试";
      this.$notification['error']({
        message: '登录失败',
        description: errMsg,
        duration: 4,
      });
      this.loginBtn = false;
    },
    validateMobile(rule, value, callback) {
      if (!value || new RegExp(/^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\d{8}$/).test(value)) {
        callback();
      } else {
        callback("您的手机号码格式不正确!");
      }

    },
    loginSelectOk() {
      this.loginSuccess()
    },
    getRouterData() {
      this.$nextTick(() => {
        if (this.$route.params.username) {
          this.form.setFieldsValue({
            'username': this.$route.params.username
          });
        }
      })
    },
    //获取密码加密规则
    getEncrypte() {
      var encryptedString = Vue.ls.get(ENCRYPTED_STRING);
      if (encryptedString == null) {
        getEncryptedString().then((data) => {
          this.encryptedString = data
        });
      } else {
        this.encryptedString = encryptedString;
      }
    },
  }
}
</script>

<style lang="less" scoped>

.user-layout-login {
  label {
    font-size: 14px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }

  .forge-password {
    font-size: 14px;
  }

  button.login-button {
    padding: 0 15px;
    font-size: 16px;
    height: 40px;
    width: 100%;
  }

  .user-login-other {
    text-align: left;
    margin-top: 24px;
    line-height: 22px;

    .item-icon {
      font-size: 24px;
      color: rgba(0, 0, 0, .2);
      margin-left: 16px;
      vertical-align: middle;
      cursor: pointer;
      transition: color .3s;

      &:hover {
        color: #1890ff;
      }
    }

    .register {
      float: right;
    }
  }
}

</style>
<style>
.valid-error .ant-select-selection__placeholder {
  color: #f5222d;
}
</style>