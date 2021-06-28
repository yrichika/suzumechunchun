<template>
    <div class="grid grid-rows-2 md:grid-rows-1 grid-cols-1 md:grid-cols-2 gap-4">

        <div class="justify-center order-last md:order-first mx-2">
            <hr class="mx-2 my-2 md:invisible">
            <p class="text-lg ml-2 mb-2">
                <span
                class="border rounded-full px-2"
                :class="'bg-' + color + ' ' + nameTextColor"
                >
                    {{ codename }}
                </span>
            </p>
            <textarea type="text"
            id="message-input"
            class="shadow border border-blue-600 rounded w-full px-2"
            v-model="sendingMessage"
            @keydown="sendShortcut"
            ></textarea>
            <ul class="mt-4">
                <li class="flex justify-center">
                    <button @click="sendMessage()" class="lang-send-button btn btn-blue">送信</button>
                </li>
                <li class="flex justify-center">
                    <div class="lang-send-button-message text-sm text-gray-400 scc-tip"></div>
                </li>
            </ul>
        </div>
        <div class="justify-center mx-2">
            <div class="flex justify-center">
                <span class="text-lg lang-chat-label"></span>
            </div>
            <hr class="my-2 border-2">
            <transition-group name="fade" tag="p">
            <!-- `transition-group` manages transition positions in array by `:key`. `array.length - index` is to apply the transition(fade-out) at the top of the elements -->
            <div v-for="(message, index) in receivedMessages" :key="receivedMessages.length - index">
                <p v-if="(message.name === codename)" class="flex justify-start mb-2">
                    <span
                    class="rounded-full px-2 self-start"
                    :class="'bg-' + color + ' ' + nameTextColor"
                    >
                        {{ message.name }}
                    </span>
                    <span class="mr-2 self-start" :class="'text-' + color">&gt;</span>
                    <span
                    class="rounded border shadow px-2"
                    :class="'border-' + color"
                    v-html="breakLines(message.message)"></span>
                </p>
                <p v-else class="flex justify-end mb-2">
                    <span
                    class="rounded border shadow px-2"
                    :class="'border-' + message.color"
                    v-html="breakLines(message.message)"></span>
                    <span
                    class="ml-2 self-end"
                    :class="'text-' + message.color"
                    >
                    &lt;</span>
                    <span
                    class="rounded-full px-2 self-end"
                    :class="'bg-' + message.color + ' ' + textColor(message.color)"
                    >{{ message.name }}</span>
                </p>
            </div>
            </transition-group>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import CryptoJS from 'crypto-js'
import ChatMessage from '@app/types/ChatMessage'
import Utils from '@app/helpers/Utils'
import { colors } from './colors'
// `en` and `ja` are injected at .html file load
declare const en: Map<string, string>
declare const ja: Map<string, string>

export default Vue.extend({
    name: 'Chat',
    data() {
        return {
            connection: new WebSocket(this.webSocketUrl),
            pingPong: 0,
            receivedMessages: Array<ChatMessage>(),
            sendingMessage: '',
            storedEncryptedMessages: Array<string>(),
            color: 'green-500',
            nameTextColor: 'text-black',
        }
    },
    props: {
        codename: {
            type: String,
            default: 'Host'
        },
        secretKey: String,
        webSocketUrl: String,
        // memo: just write `is-host` in client html. no need to write `:is-host="true"`
        isHost: {
            type: Boolean,
            default: false
        },
        sessionName: {
            type: String,
            default: 'suzume-chunchun-messages'
        },
        colorIndex: {
            type: Number,
            default: 0
        },
        pingIntervalInMilSec: {
            type: Number,
            default: 30000,
        },
        eraseMessageIntervalInMilSec: {
            type: Number,
            default: 20000
        },
        numMaxMessages: {
            type: Number,
            default: 10
        },
        pingMessage: {
            type: String,
            default: '__ping__'
        }
    },

    created() {
        // TODO: add: abort if this.secretKey is missing
        
        this.alertClientBeforeLeave();
        this.color = colors[this.colorIndex]
        this.nameTextColor = this.textColor(this.color)
        window.setInterval(this.eraseMessage, this.eraseMessageIntervalInMilSec)

        this.connection.onopen = () => {
            // TYPESCRIPT: must prepend `window.` to `setInterval()`. Otherwise, it's gonna use NodeJS's setInterval.
            this.pingPong = window.setInterval(this.ping, this.pingIntervalInMilSec);
        };

        this.connection.onmessage = event => {
            if (this.isHost) {
                this.save(event.data);
            }
            const decryptedMessage = this.decrypt(event.data);
            this.showMessage(decryptedMessage);
        };

        this.connection.onclose = event => {
            clearInterval(this.pingPong);
        };

        this.connection.onerror = event => {
            console.error("websocket error", event);
            alert('This chat connection is closed! Any Messages will not be sent to other members!')
        }
    },

    mounted() {
        if (this.isHost) {
            this.storedEncryptedMessages = this.getSavedMessages();
            for (let i = 0; i < this.storedEncryptedMessages.length; i++) {
                const decryptedMessage = this.decrypt(this.storedEncryptedMessages[i]);
                this.showMessage(decryptedMessage);
            }
        }
    },


    methods: {
        eraseMessage(): void {
            if (this.receivedMessages.length > 0) {
                this.receivedMessages.shift()
                this.storedEncryptedMessages.shift()
                sessionStorage.setItem(this.sessionName, JSON.stringify(this.storedEncryptedMessages));
            }
        },

        getSavedMessages(): Array<string> {
            const storedDataString = sessionStorage.getItem(this.sessionName)
            if (storedDataString == null) {
                return []
            }
            return JSON.parse(storedDataString)
        },

        /**
         * text-white, text-black are Tailwind's classes
         */
        textColor(bgColor: string): string {
            if (bgColor.match(/[5|6|7|8|9]00/)) {
                return 'text-white'
            }
            return 'text-black'
        },

        save(data: string): void {
            this.addToMessagesLimitTo(this.storedEncryptedMessages, data, this.numMaxMessages)
            sessionStorage.setItem(this.sessionName, JSON.stringify(this.storedEncryptedMessages));
        },

        showMessage(decryptedMessage: ChatMessage): void {
            this.addToMessagesLimitTo(this.receivedMessages, decryptedMessage, this.numMaxMessages)
        },

        /**
         * this method is to change `messages` parameter value.
         */
        addToMessagesLimitTo(messages: Array<any>, data: any, howMany: number): void {
            if (messages.length >= howMany) {
                messages.shift();
            }
            messages.push(data);
        },

        decrypt(encMessage: string): ChatMessage {
            const bytes = CryptoJS.AES.decrypt(encMessage, this.secretKey)
            const decrypted = bytes.toString(CryptoJS.enc.Utf8)
            return JSON.parse(decrypted)
        },


        encrypt(): string {
            const name = this.codename
            const sanitizedMessage = Utils.htmlspecialcharsJs(this.sendingMessage)
            const message: ChatMessage = {
                name: name,
                message: sanitizedMessage,
                color: this.color
            }
            const jsonedMessage = JSON.stringify(message)
            return CryptoJS.AES.encrypt(jsonedMessage, this.secretKey).toString();
        },


        sendShortcut(event: KeyboardEvent): void {
            if (event.shiftKey && event.key == 'Enter') {
                event.preventDefault() // prevents extra line break in textarea
                this.sendMessage()
            }
        },

        sendMessage(): void {
            const encryptedMessage = this.encrypt()
            this.connection.send(encryptedMessage)
            this.sendingMessage = ''
        },

        // no test
        breakLines(message: string): string {
            return Utils.breakLines(message)
        },
        // no test
        ping(): void {
            this.connection.send(this.pingMessage)
        },

        // no test
        alertClientBeforeLeave(): void {
            if (!this.isHost) {
                // No process on onbeforeunload
                window.onbeforeunload = () => true;
            }
        }
    }
});
</script>

<style scoped lang="scss">

.fade-leave-active {
    transition: all .5s cubic-bezier(0.07,0.7,0.38,0.99);
}

.fade-leave-to {
    opacity: 0;
}

</style>