<template>
    <div>
        <div v-if="isClosed" class="flex justify-center">
            <span class="lang-chat-closed">{{ errorChatClosedMessage }}</span>
        </div>
        <div v-else>
            <div class="justify-center">
                <ul class="mt-4">
                    <li class="flex justify-center mb-2">
                        <div>
                            <label for="codename" class="lang-input-label-codename block pl-1"></label>
                            <input type="text" id="codename" class="wp-text-input px-2 w-auto sm:w-72 disabled:opacity-50" v-model="codename" required :disabled="isWaitingForAuthentication">
                        </div>
                    </li>
                    <li class="flex justify-center">
                        <div>
                            <label for="passphrase" class="lang-input-label-passphrase block pl-1"></label>
                            <textarea id="passphrase" rows="3" class="wp-text-input px-2 pt-1 w-auto sm:w-72 disabled:opacity-50" v-model="passphrase" placeholder="Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua." required :disabled="isWaitingForAuthentication"></textarea>
                        </div>
                    </li>
                </ul>
                <div class="flex justify-center mt-5 mb-5">
                    <button id="submit-button" type="submit" class="lang-send-request-button btn btn-blue" @click="send()"></button>
                </div>
            </div>
            <hr>
            <div class="flex justify-center mt-5 mx-2">
                <span v-if="!isWaitingForAuthentication">
                    <span class="lang-send-auth">{{ defaultMessage }}</span>
                </span>
                <!-- REFACTOR: -->
                <span v-else class="break-all">
                    <span v-if="clientChannel">
                        <span v-if="clientChannel == rejectedStatusString">
                            <span class="lang-rejected">{{ rejectedMessage }}</span>
                        </span>
                        <span v-else>
                            <span class="lang-accepted font-bold">{{ acceptedMessage }}</span><span>: </span>
                            <a :href="authenticatedUrl(clientChannel)" class="underline text-blue-500 break-all overflow-ellipsis">
                                {{ authenticatedUrl(clientChannel) }}
                            </a>
                        </span>
                    </span>
                    <span v-else class="break-all">
                        <span class="lang-waiting-for-authentication">{{ waitingMessage }}</span>
                    </span>
                </span>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropType } from 'vue'
import axios, { AxiosError, AxiosResponse } from 'axios'
import ClientAuthentication from '@app/types/ClientAuthentication'
import LanguageSwitch from '@app/helpers/LanguageSwitch'

// `en` and `ja` are injected at .html file load
declare const en: Map<string, string>
declare const ja: Map<string, string>

export default Vue.extend({
    name: 'ClientLoginRequest',
    data() {
        return {
            clientChannel: '',
            codename: '',
            passphrase: '',
            isWaitingForAuthentication: false,
            eventSource: null as EventSource | null,
            isClosed: false
        }
    },
    props: {
        sseUrl: String,
        requestUrl: String,
        chatUrl: String,
        rejectedStatusString: {
            type: String,
            default: '__rejected__'
        },
        closedRequestStatusString: {
            type: String,
            default: '__closed__'
        },
        // made them props for testing purposes
        enMap: {
            type: Map as PropType<Map<string, string>>,
            default: () => en
        },
        jaMap: {
            type: Map as PropType<Map<string, string>>,
            default: () => ja
        }
    },
    created() {

    },
    mounted() {
        // any process after DOM is bind
    },
    computed: {
        errorChatClosedMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('chat-closed'), this.jaMap.get('chat-closed'))
        },
        defaultMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('send-auth'), this.jaMap.get('send-auth'))
        },
        acceptedMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('accepted'), this.jaMap.get('accepted'))
        },
        rejectedMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('rejected'), this.jaMap.get('rejected'))
        },
        waitingMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('waiting-for-authentication'), this.jaMap.get('waiting-for-authentication'))
        },
        requestFailed(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('request-failed'), this.jaMap.get('request-failed'))
        }

    },
    watch: {
        clientChannel(): void {
            if (this.clientChannel) {
                this.eventSource?.close()
            }
        }
    },
    methods: {

        authenticatedUrl(clientChannel: string): string {
            return this.chatUrl + clientChannel
        },


        send(): void {
            this.isWaitingForAuthentication = true;
            axios.post(this.requestUrl, {
                codename: this.codename,
                passphrase: this.passphrase
            })
            .then((response: AxiosResponse<any>) => {
                if (response.data.message != this.closedRequestStatusString) {
                    this.eventSource = new EventSource(this.sseUrl);
                    this.eventSource.onmessage = this.sseMessageEvent
                } else {
                    alert(this.errorChatClosedMessage)
                    this.isClosed = true
                    this.isWaitingForAuthentication = false
                }
            })
            .catch((error: AxiosError) => {
                // Play does not send BadRequest from ClientController.failedRequest()
                // It just ignore and refresh form.
                this.codename = ''
                this.passphrase = ''
                this.isWaitingForAuthentication = false;
            })
        },


        sseMessageEvent(authentication: MessageEvent<any>): void {
            const serverData: ClientAuthentication = JSON.parse(authentication.data)
            this.clientChannel = serverData.clientChannel
        }
    }
})
</script>

<style scoped lang="scss">

</style>