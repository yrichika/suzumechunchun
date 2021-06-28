<template>
    <div>
        <div class="flex justify-center">
            <h2 class="lang-clients-list-title text-lg"></h2>
            <button id="close-request-button" class="lang-stop-client-requests rounded text-white px-2 text-sm mx-2 bg-blue-500 hover:bg-blue-700 disabled:opacity-50" @click="closeRequest()" :disabled="isRequestClosed">
            </button>
        </div>
        <div class="flex justify-center mx-5 mt-2">
            <p class="text-xs opacity-50 scc-tip">
                <span class="lang-stop-client-requests font-bold"></span>
                <span class="lang-stop-client-requests-tip"></span>
            </p>
        </div>
        <div class="flex justify-center mt-5">
            <ul>
                <li v-for="(request, index) in requests" :key="index" class="border border-blue-300 rounded shadow py-1 px-2 mb-3">
                    <div class="grid grid-rows-2 md:grid-rows-1 grid-cols-1 md:grid-cols-2 md:gap-4">
                        <div class="flex justify-center items-center order-last md:order-first">
                            <button :id="acceptBtnIdPrefix + index" class="btn btn-blue mr-2 focus:opacity-50 disabled:opacity-50"
                            @click="accept(request, index)"
                            :disabled="(request.isAuthenticated != null)">
                                Accept
                            </button>
                            <button :id="rejectBtnIdPrefix + index" class="btn btn-red focus:opacity-50 disabled:opacity-50"
                            @click="reject(request, index)"
                            :disabled="(request.isAuthenticated != null)">
                                Reject
                            </button>
                        </div>
                        <div>
                            <p class="text-sm md:text-base">
                                <span>Codename</span>
                                <span class="mr-2">:</span>
                                <span>{{ request.codename }}</span>
                            </p>
                            <p class="text-sm md:text-base">
                                <span>Passphrase</span>
                                <span class="mr-2">:</span>
                                <span>{{ request.passphrase }}</span>
                            </p>
                            <p class="text-sm md:text-base">
                                <span class="lang-requester-status">Status</span>
                                <span class="mr-2">:</span>
                                <span
                                class="text-sm px-1"
                                :class="request.isAuthenticated ? 'border border-green-500 bg-green-500 text-white rounded-full' : 'border border-red-500 bg-red-500 text-white rounded-full'"
                                >
                                    {{ showStatus(request.isAuthenticated) }}
                                </span>
                            </p>
                        </div>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropType } from 'vue'
import axios, { AxiosError } from 'axios'
import ClientRequest from '@app/types/ClientRequest'
import Utils from '@app/helpers/Utils'
import LanguageSwitch from '@app/helpers/LanguageSwitch'
// `en` and `ja` are injected at .scala.html file load
declare const en: Map<string, string>
declare const ja: Map<string, string>

export default Vue.extend({
    name: 'ManageClientRequest',
    data() {
        return {
            requests: Array<ClientRequest>(),
            dataSource: new EventSource(this.sseUrl),
            isRequestClosed: false,
            Utils: Utils
        };
    },
    props: {
        sseUrl: String,
        postUrl: String,
        closeRequestUrl: String,
        acceptBtnIdPrefix: {
            type: String,
            default: 'accept-btn-'
        },
        rejectBtnIdPrefix: {
            type: String,
            default: 'reject-btn-'
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
        this.dataSource.onmessage = event => {
            this.requests = JSON.parse(event.data);
        };

        this.dataSource.onerror = event => {
            console.warn('Event source has been closed.');
        };

    },

    computed: {
        closeRequestMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('confirm-closing-request'), this.jaMap.get('confirm-closing-request'))
        },
        closeRequestFailedMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('closing-request-failed'), this.jaMap.get('closing-request-failed'))
        },
        mangeRequestFailedMessage(): string {
            return LanguageSwitch.pickLangMessage(this.enMap.get('manage-request-failed'), this.jaMap.get('manage-request-failed'))
        }
    },

    methods: {
        showStatus(isAuthenticated: null | boolean): string {
            if (isAuthenticated === null) {
                return 'Not Accepted'
            } else if (isAuthenticated === false) {
               return 'Rejected' 
            }
            return 'Accepted'
        },

        closeRequest(): void {
            if (!confirm((this.closeRequestMessage))) {
                return;
            }

            axios.get(this.closeRequestUrl)
                .then(data => {
                    this.isRequestClosed = true
                    this.dataSource.close()
                })
                .catch((error: AxiosError) => {
                    alert(this.closeRequestFailedMessage)
                })
        },

        accept(request: ClientRequest, index: number) {
            Utils.disableButtons(index, [this.acceptBtnIdPrefix, this.rejectBtnIdPrefix])
            this.send(request, true)
        },


        reject(request: ClientRequest, index: number): void {
            Utils.disableButtons(index, [this.acceptBtnIdPrefix, this.rejectBtnIdPrefix])
            this.send(request, false)
        },


        send(request: ClientRequest, status: boolean): void {
            axios.post(this.postUrl, {
                requestClientId: request.requestClientId,
                status: status
            })
            .catch((error: AxiosError) => {
                alert(request.codename + this.mangeRequestFailedMessage);
            });
        }

    }
})
</script>

<style scoped lang="scss">

</style>