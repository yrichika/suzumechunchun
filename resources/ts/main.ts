import Global from '@app/types/Global'
import Vue from 'vue'
import axios from 'axios'
import Cookie from '@app/helpers/Cookie'
import LanguageSwitch from '@app/helpers/LanguageSwitch'
import Utils from '@app/helpers/Utils'

import ManageClientRequest from '@app/components/ManageClientRequest.vue'
import ClientLoginRequest from '@app/components/ClientLoginRequest.vue'
import Chat from '@app/components/Chat.vue'

import '@css/main.scss'
import 'bootstrap-icons/font/bootstrap-icons.css'

// Get csrf token for ajax request
const csrfToken = Cookie.get('csrf_token')
axios.defaults.headers.common = {
  'X-Requested-With': 'XMLHttpRequest',
  'Csrf-Token': csrfToken ?? ''
}

// Add Vue components
const app = new Vue({
  el: '#vue-app',
  components: {
    'manage-client-request': ManageClientRequest,
    'client-login-request': ClientLoginRequest,
    'chat': Chat
  }
})

// `en` and `ja` are injected at .html file load
declare const en: Map<string, string>
declare const ja: Map<string, string>

// Initialize language
declare const global: Global
global.L = new LanguageSwitch(en, ja)
global.Utils = Utils
try {
  global.L.initLanguage()
  Utils.hideTips('.scc-tip')
} catch (exception) {
  console.log('Not enough language files provided: ' + exception)
}
