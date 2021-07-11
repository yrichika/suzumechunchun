import { mount } from '@vue/test-utils'
import { VueFlexTypeWrapper } from '@test/testhelpers/types/VueFlexTypeWrapper'
import Random from '@test/testhelpers/Random'
import Chat from '@app/components/Chat.vue'
import { colors } from '@app/components/colors'
import RegExpHelper from '@test/testhelpers/RegExpHelper'
import ChatMessage from '@app/types/ChatMessage'
import storageMock from '@test/testhelpers/testdoubles/mocks/storageMock'
// https://github.com/romgain/jest-websocket-mock
import WS from "jest-websocket-mock";
import CryptoJS from 'crypto-js'
import _ from 'lodash'

describe('Chat' , () => {
  let wrapper: VueFlexTypeWrapper
  let webSocketServer: WS

  const props = {
    codename: Random.string(5),
    secretKey: Random.string(5),
    webSocketUrl: 'ws://websocket',
    isHost: true,
    colorIndex: Random.int(0, 34)
  };

  const consoleErrorBackUp = console.error

  // sessionStorage mock
  const sessionStorageMock = new storageMock()
  Object.defineProperty(window, 'sessionStorage', {
    value: sessionStorageMock
  });
  const dateNowBackUp = Date.now

  beforeAll(() => {
    jest.spyOn(window, 'alert').mockImplementation(() => {})
  })

  beforeEach(() => {
    jest.useRealTimers() // IMPORTANT!!
    // create webSocketServer before vue instance
    webSocketServer = new WS(props.webSocketUrl)
    wrapper = mount(Chat, {
      propsData: props
    })
  })

  afterEach(() => {
    jest.clearAllMocks()
    sessionStorageMock.clear()
    webSocketServer.close()
    console.error = consoleErrorBackUp
    Date.now = dateNowBackUp
  })


  test('it should set properties and data properly', () => {
    expect(wrapper.props().codename).toBe(props.codename)
    expect(wrapper.props().secretKey).toBe(props.secretKey)
    expect(wrapper.props().webSocketUrl).toBe(props.webSocketUrl)
    expect(wrapper.props().isHost).toBe(props.isHost)

    expect(colors.includes(wrapper.vm.color)).toBe(true)
    const nameColor = RegExpHelper.eitherRegex('text-white', 'text-black')
    expect(wrapper.vm.nameTextColor).toMatch(nameColor)
  })


  test('it should abort if no secret key provided', () => {
    // todo: not implemented yet!
  })


  test('setInterval should set eraseMessage method at create hook', () => {
    jest.useFakeTimers()
    // WARNING! must instantiate after jest.useFakeTimers()
    const anotherWrapper: VueFlexTypeWrapper = mount(Chat, {propsData: props})

    expect(setInterval).toHaveBeenCalledWith(anotherWrapper.vm.eraseMessage, anotherWrapper.props().eraseMessageExecuteIntervalInMilSec)
  })


  test('websocket onopen: it should send ping message every x seconds', async () => {
    jest.useFakeTimers()

    await webSocketServer.connected
    expect(setInterval).toHaveBeenCalledWith(wrapper.vm.ping, wrapper.props().pingIntervalInMilSec);
  })


  test('websocket onclose: it should cancel sending ping', async () => {
    jest.useFakeTimers()

    await webSocketServer.connected
    webSocketServer.close()
    expect(clearInterval).toHaveBeenCalledWith(expect.any(Number))

  })


  test('websocket should show alert if on error', async () => {
    console.error = jest.fn()
    await webSocketServer.connected
    webSocketServer.error()

    expect(window.alert).toHaveBeenCalledTimes(1)
    expect(console.error).toHaveBeenCalledTimes(1)
  })


  test('websocket should add received message to receivedMessages if message received from the server', async () => {
    await webSocketServer.connected
    const chatMessage = createSingleFakeChatMessage()
    const encMessage = CryptoJS.AES.encrypt(JSON.stringify(chatMessage), props.secretKey)
    webSocketServer.send(encMessage)

    expect(wrapper.vm.receivedMessages[0]).toEqual(chatMessage)
  })


  test('websocket should also add received message to sessionStorage if isHost true', async () => {
    await webSocketServer.connected
    const chatMessage = createSingleFakeChatMessage()
    const encMessage = CryptoJS.AES.encrypt(JSON.stringify(chatMessage), props.secretKey)
    webSocketServer.send(encMessage)

    const savedMessage = getParsedAndDecryptedSessionData(wrapper)[0]
    expect(savedMessage).toEqual(chatMessage)
  })


  test('websocket should not add received message to sessionStorage if isHost false', async () => {
    // creating completely different websocket connection from beforeEach
    webSocketServer.close()
    const clonedProps = _.cloneDeep(props)
    const clientProps = Object.assign(clonedProps, {
      webSocketUrl: 'ws://client-websocket',
      sessionName: 'client-session',
      isHost: false
    })
    webSocketServer = new WS(clientProps.webSocketUrl)
    const clientWrapper = mount(Chat, { propsData: clientProps })
    await webSocketServer.connected

    const chatMessage = createSingleFakeChatMessage()
    const encMessage = CryptoJS.AES.encrypt(JSON.stringify(chatMessage), props.secretKey)
    webSocketServer.send(encMessage)

    const savedMessagesString = window.sessionStorage.getItem(clientWrapper.props().sessionName)!
    expect(savedMessagesString).toBeNull()
  })


  test('addToMessagesLimitTo should add new message to receivedMessages', () => {
    const messageString = createSingleFakeChatMessage()
    wrapper.vm.receivedMessages = []
    wrapper.vm.addToMessagesLimitTo(messageString, 10, Date.now())

    expect(wrapper.vm.receivedMessages[0]).toEqual(messageString)
  })


  test('addToMessagesLimitTo should delete head data and append new data if array is over limit length and update first element timestamp', () => {
    const originalMessages = createFakeChatMessages(3)

    const newMessage = createSingleFakeChatMessage()
    wrapper.vm.receivedMessages = _.cloneDeep(originalMessages)
    const tickMilSec = 1000
    distortCurrentTimeTo(tickMilSec)
    wrapper.vm.addToMessagesLimitTo(newMessage, 3, Date.now())
    
    expect(wrapper.vm.receivedMessages.length).toBe(3)
    expect(wrapper.vm.receivedMessages[0].name).toBe(originalMessages[1].name)
    expect(wrapper.vm.receivedMessages[0].message).toBe(originalMessages[1].message)
    expect(wrapper.vm.receivedMessages[0].color).toBe(originalMessages[1].color)
    expect(wrapper.vm.receivedMessages[0].timestamp).toBeGreaterThanOrEqual(originalMessages[1].timestamp + tickMilSec)

    expect(wrapper.vm.receivedMessages[1]).toEqual(originalMessages[2])
    expect(wrapper.vm.receivedMessages[2]).toEqual(newMessage)
  })


  test('addToSessionLimitTo should add new message to storedEncryptedMessages and sessionStorage', () => {
    setChatMessages(wrapper)
    const newMessage: ChatMessage = createSingleFakeChatMessage()
    const jsoned = JSON.stringify(newMessage)
    const encMessage = CryptoJS.AES.encrypt(jsoned, wrapper.vm.secretKey).toString();

    wrapper.vm.addToSessionLimitTo(encMessage, 10, Date.now())

    const storedMessage = _.last(wrapper.vm.storedEncryptedMessages)
    expect(wrapper.vm.decrypt(storedMessage)).toEqual(newMessage)
  })


  test('addToSessionLimitTo should delete head data and append new data if array is over limit length and update first element timestamp', () => {
    const maxNum = 3
    const created = setChatMessages(wrapper, maxNum)
    const newMessage: ChatMessage = createSingleFakeChatMessage()
    const jsoned = JSON.stringify(newMessage)
    const encMessage = CryptoJS.AES.encrypt(jsoned, wrapper.vm.secretKey).toString();

    const tickMilSec = 1000
    distortCurrentTimeTo(tickMilSec)
    wrapper.vm.addToSessionLimitTo(encMessage, maxNum, Date.now())

    expect(wrapper.vm.storedEncryptedMessages.length).toBe(maxNum)
    const decryptedMessages = wrapper.vm.storedEncryptedMessages.map((message: string) => {
      return wrapper.vm.decrypt(message)
    })
    expect(decryptedMessages[0].name).toBe(created[1].name)
    expect(decryptedMessages[0].message).toBe(created[1].message)
    expect(decryptedMessages[0].color).toBe(created[1].color)
    expect(decryptedMessages[0].timestamp).toBeGreaterThanOrEqual(created[1].timestamp + tickMilSec)

    expect(decryptedMessages[1]).toEqual(created[2])
    expect(decryptedMessages[2]).toEqual(newMessage)
  })


  test('setHeadSessionStoredMessageTimestamp should set timestamp of first item of storedEncryptedMessage and sessionStorage', () => {
    setChatMessages(wrapper)
    const timestamp = Random.int(1, 1000)
    wrapper.vm.setHeadSessionStoredMessageTimestamp(timestamp)

    const decryptedMessage: ChatMessage = wrapper.vm.decrypt(wrapper.vm.storedEncryptedMessages[0])
    expect(decryptedMessage.timestamp).toBe(timestamp)
    const sessionStoredMessages = getParsedAndDecryptedSessionData(wrapper)
    expect(sessionStoredMessages[0].timestamp).toBe(timestamp)
  })


  test('encrypt should encrypt sending message and decrypt should decrypt the encrypted string to json', () => {
    const sendingMessage = Random.string(5)
    wrapper.vm.sendingMessage = sendingMessage
    const timestamp = Date.now()
    const encryptedMessage = wrapper.vm.encrypt(timestamp)
    expect(encryptedMessage).not.toMatch(sendingMessage) // not necessary, but just checking

    const decryptedMessage: ChatMessage = wrapper.vm.decrypt(encryptedMessage)

    expect(decryptedMessage.name).toBe(wrapper.props().codename)
    expect(decryptedMessage.message).toBe(sendingMessage)
    expect(decryptedMessage.color).toBe(wrapper.vm.color)
    expect(decryptedMessage.timestamp).toBe(timestamp)
    
  })


  test('save should save given string to sessionStorage', () => {
    const encryptedMessage = Random.string(5)
    wrapper.vm.save(encryptedMessage, Date.now())

    const savedMessagesString = window.sessionStorage.getItem(wrapper.props().sessionName)!
    const savedMessagesArray: Array<string> = JSON.parse(savedMessagesString)
    expect(savedMessagesArray[0]).toBe(encryptedMessage)
  })


  test('getSavedMessage should return all encrypted stored messages as array', () => {
    const encryptedMessage = Random.string(5)
    wrapper.vm.save(encryptedMessage, Date.now())

    const result = wrapper.vm.getSavedMessages()
    expect(result[0]).toBe(encryptedMessage)
  })


  test('getSavedMessage should return empty array if no message is stored', () => {
    const result = wrapper.vm.getSavedMessages()
    expect(result).toEqual([])
  })


  test('eraseMessage should delete one oldest element in receivedMessages if eraseMessageOlderThanMilSec mill seconds passed', () => {
    const created = setChatMessages(wrapper)

    distortCurrentTimeTo(wrapper.vm.eraseMessageOlderThanMilSec)
    wrapper.vm.eraseMessage()

    expect(wrapper.vm.receivedMessages.length).toBe(created.length - 1)
    for (let i = 0; i < (created.length - 1); i++) {
      if (i !== 0) {
        expect(wrapper.vm.receivedMessages[i]).toEqual(created[i + 1])
      } else {
        // Do not compare timestamp of the first element.
        expect(wrapper.vm.receivedMessages[i].name).toBe(created[i + 1].name)
        expect(wrapper.vm.receivedMessages[i].message).toBe(created[i + 1].message)
        expect(wrapper.vm.receivedMessages[i].color).toBe(created[i + 1].color)
      }
    }
  })


  test('eraseMessage should not delete any elements in receivedMessages if eraseMessageOlderThanMilSec mill seconds have not passed', () => {
    const created = setChatMessages(wrapper)
    
    wrapper.vm.eraseMessage()

    expect(wrapper.vm.receivedMessages.length).toBe(created.length)
    for (let i = 0; i < created.length; i++) {
      expect(wrapper.vm.receivedMessages[i]).toEqual(created[i])
    }
  })


  test('eraseMessage should delete one oldest element in sessionStorage if eraseMessageOlderThanMilSec mill seconds have passed', () => {
    const created = setChatMessages(wrapper)

    distortCurrentTimeTo(wrapper.vm.eraseMessageOlderThanMilSec)

    wrapper.vm.eraseMessage()

    const sessionStoredMessages = getParsedAndDecryptedSessionData(wrapper)

    expect(sessionStoredMessages.length).toBe(created.length - 1)
    for(let i = 0; i < (created.length - 1); i++) {
      expect(sessionStoredMessages[i].name).toBe(created[i + 1].name)
      expect(sessionStoredMessages[i].message).toBe(created[i + 1].message)
      expect(sessionStoredMessages[i].color).toBe(created[i + 1].color)
      // Do not assert timestamp property. Timestamp property of the first element in sessionStorage is changed.
    }
  })


  test('eraseMessage should not delete any elements in sessionStorage if eraseMessageOlderThanMilSec mill seconds have not passed', () => {
    const created = setChatMessages(wrapper)

    wrapper.vm.eraseMessage()

    const sessionStoredMessages = getParsedAndDecryptedSessionData(wrapper)

    expect(sessionStoredMessages.length).toBe(created.length)
    for(let i = 0; i < created.length; i++) {
      expect(sessionStoredMessages[i].name).toBe(created[i].name)
      expect(sessionStoredMessages[i].message).toBe(created[i].message)
      expect(sessionStoredMessages[i].color).toBe(created[i].color)
      // Do not assert timestamp property. Timestamp property of the first element in sessionStorage is changed.
    }
  })



  test('shiftMessage should shift receivedMessages and storedEncryptedMessages', () => {
    const createdMessages = setChatMessages(wrapper)

    wrapper.vm.shiftMessage()

    expect(wrapper.vm.receivedMessages.length).toBe(createdMessages.length - 1)
    expect(wrapper.vm.storedEncryptedMessages.length).toBe(createdMessages.length - 1)
    for (let i = 0; i < createdMessages.length - 1; i++) {
      if (i !== 0) {
        expect(wrapper.vm.receivedMessages[i]).toEqual(createdMessages[i + 1])
        const decrypted = wrapper.vm.decrypt(wrapper.vm.storedEncryptedMessages[i])
        expect(decrypted).toEqual(createdMessages[i + 1])
      } else {
        expect(wrapper.vm.receivedMessages[i].name).toBe(createdMessages[i + 1].name)
        expect(wrapper.vm.receivedMessages[i].message).toBe(createdMessages[i + 1].message)
        expect(wrapper.vm.receivedMessages[i].color).toBe(createdMessages[i + 1].color)

        const decrypted = wrapper.vm.decrypt(wrapper.vm.storedEncryptedMessages[i])
        expect(decrypted.name).toBe(createdMessages[i + 1].name)
        expect(decrypted.message).toBe(createdMessages[i + 1].message)
        expect(decrypted.color).toBe(createdMessages[i + 1].color)
      }
    }
  })


  test('shiftMessage should return deleted message', () => {
    const createdMessages = setChatMessages(wrapper)
    const result: ChatMessage = wrapper.vm.shiftMessage()
    expect(result).toEqual(createdMessages[0])
  })


  test('setHeadMessageTimestampToCurrent should update timestamp of the first chat message in messages to current time', () => {
    const createdMessages = setChatMessages(wrapper)
    const tickMilSec = 1000
    distortCurrentTimeTo(tickMilSec)

    wrapper.vm.setHeadMessageTimestampToCurrent()

    expect(wrapper.vm.receivedMessages[0].timestamp).toBeGreaterThanOrEqual(createdMessages[0].timestamp + tickMilSec)
  })


  test('setHeadMessageTimestampToCurrent should update timestamp of the first chat message in encrypted session messages to current time', () => {

    const createdMessages = setChatMessages(wrapper)
    const tickMilSec = 1000
    distortCurrentTimeTo(tickMilSec)

    wrapper.vm.setHeadMessageTimestampToCurrent()

    const result = getParsedAndDecryptedSessionData(wrapper)[0]

    expect(result.timestamp).toBeGreaterThanOrEqual(createdMessages[0].timestamp + tickMilSec)
  })


  test('setHeadMessageTimestampToCurrent should do nothing if receivedMessages is empty', () => {
    wrapper.vm.receivedMessages = []
    wrapper.vm.storedEncryptedMessages = []
    window.sessionStorage.setItem(wrapper.props().sessionName, JSON.stringify([]))
    const tickMilSec = 1000
    distortCurrentTimeTo(tickMilSec)

    wrapper.vm.setHeadMessageTimestampToCurrent()
    
    expect(wrapper.vm.receivedMessages).toEqual([])
    expect(wrapper.vm.storedEncryptedMessages).toEqual([])
    expect(window.sessionStorage.getItem(wrapper.props().sessionName)).toBe('[]')
  })


  test('showMessage should add message to receivedMessages', () => {
    const numMessages = Random.int(1, 5)
    const decryptedMessages = createFakeChatMessages(numMessages)
    
    decryptedMessages.forEach(message => {
      wrapper.vm.showMessage(message, Date.now())
    })

    wrapper.vm.receivedMessages.forEach((message: ChatMessage, key: number) => {
      expect(message.name).toBe(decryptedMessages[key].name)
      expect(message.message).toBe(decryptedMessages[key].message)
      expect(message.color).toBe(decryptedMessages[key].color)
    })
    
  })


  test('sendMessage should call websocket send method and empty sendingMessage', async () => {
    await webSocketServer.connected
    wrapper.vm.sendingMessage = Random.string(5)

    wrapper.vm.sendMessage()

    expect(wrapper.vm.sendingMessage).toBe('')
    // encrypted: always different string.
    await webSocketServer.nextMessage.then(message => {
      const anyStringRegExp = new RegExp('.{10,}') // encrypted strings are always more than 10 chars
      expect(message).toMatch(anyStringRegExp)
    })

  })


  test('sendShortcut: it should send message if shift + enter keys are pressed down', async () => {
    await webSocketServer.connected
    wrapper.vm.sendingMessage = Random.string(5)
    const preventDefaultMock = jest.fn()
    wrapper.find('#message-input').trigger('keydown', {
      shiftKey: true,
      key: 'Enter',
      preventDefault: preventDefaultMock
    })

    expect(wrapper.vm.sendingMessage).toBe('')
    expect(preventDefaultMock).toHaveBeenCalledTimes(1)
    await webSocketServer.nextMessage.then(message => {
      const anyStringRegExp = new RegExp('.{10,}')
      expect(message).toMatch(anyStringRegExp)
    })
  })


  test('sendShortcut: it should not send message if only the enter key is pressed down', async () => {
    await webSocketServer.connected
    const message = Random.string(5)
    wrapper.vm.sendingMessage = message
    const preventDefaultMock = jest.fn()
    wrapper.find('#message-input').trigger('keydown', {
      shiftKey: false,
      key: 'Enter',
      preventDefault: preventDefaultMock
    })

    expect(wrapper.vm.sendingMessage).toBe(message)
    expect(preventDefaultMock).not.toHaveBeenCalled()
  })


  test('textColor should return "text-white" if background color is darker than 400', () => {
    const digit = Random.int(5, 9).toString()
    const color = 'green-' + digit + '00'
    const result = wrapper.vm.textColor(color)

    expect(result).toBe('text-white')
  })


  test('textColor should return "text-black" if background color is lighter than or eq 400', () => {
    const digit = Random.int(2, 4).toString()
    const color = 'green-' + digit + '00'
    const result = wrapper.vm.textColor(color)

    expect(result).toBe('text-black')
  })

  // ----------------------------------------

  function setChatMessages(vueWrapper: VueFlexTypeWrapper, howMany: number = 0): ChatMessage[] {
    const numMessages = (howMany != 0) ? howMany : Random.int(1, 5)
    const chatMessages = createFakeChatMessages(numMessages)
    vueWrapper.vm.receivedMessages = chatMessages
    // encrypting original messages to store in session storage.   
    const encryptedMessages = encryptChatMessages(chatMessages)
    vueWrapper.vm.storedEncryptedMessages = _.cloneDeep(encryptedMessages)
    window.sessionStorage.setItem(wrapper.props().sessionName, JSON.stringify(wrapper.vm.storedEncryptedMessages))

    return _.cloneDeep(chatMessages)
  }
  

  function encryptChatMessages(chatMessages: ChatMessage[]): string[] {
    return chatMessages.map(message => {
      const jsonedMessage = JSON.stringify(message)
      return CryptoJS.AES.encrypt(jsonedMessage, wrapper.props().secretKey).toString();
    })
  }


  function getParsedAndDecryptedSessionData(vueWrapper: VueFlexTypeWrapper): ChatMessage[] {
    const savedMessagesString = window.sessionStorage.getItem(vueWrapper.props().sessionName)!
    const savedMessagesArray: Array<string> = JSON.parse(savedMessagesString)
    return savedMessagesArray.map(encMessages => {
      return vueWrapper.vm.decrypt(encMessages)
    })
  }


  function distortCurrentTimeTo(millSec: number) {
    Date.now = jest.fn(() => dateNowBackUp() + millSec)
  }


  function createFakeChatMessages(howMany: number): Array<ChatMessage> {
    const chatMessages = Array<ChatMessage>()
    for (let i = 0; i < howMany; i++) {
      const fakeMessage = createSingleFakeChatMessage()
      chatMessages.push(fakeMessage)
    }
    return chatMessages
  }


  function createSingleFakeChatMessage(): ChatMessage {
    return {
      name: Random.string(5),
      message: Random.string(5),
      color: Random.string(3),
      timestamp: Date.now()
    }
  }
})
