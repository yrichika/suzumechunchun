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
    const anotherWrapper = mount(Chat, {propsData: props})

    // this does not work. Don't know why.
    // expect(setInterval).toHaveBeenCalledWith(wrapper.vm.eraseMessage, wrapper.props().eraseMessageIntervalInMilSec)
    // FIXME: this is just a workaround
    expect((setInterval as any).mock.calls[0][0].name).toMatch('eraseMessage')

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
    
    expect(wrapper.vm.receivedMessages[0].name).toBe(chatMessage.name)
    expect(wrapper.vm.receivedMessages[0].message).toBe(chatMessage.message)
    expect(wrapper.vm.receivedMessages[0].color).toBe(chatMessage.color)
  })


  test('websocket should also add received message to sessionStorage if isHost true', async () => {
    await webSocketServer.connected
    const chatMessage = createSingleFakeChatMessage()
    const encMessage = CryptoJS.AES.encrypt(JSON.stringify(chatMessage), props.secretKey)
    webSocketServer.send(encMessage)

    const savedMessagesString = window.sessionStorage.getItem(wrapper.props().sessionName)!
    const savedMessagesArray: Array<string> = JSON.parse(savedMessagesString)
    const bytes = CryptoJS.AES.decrypt(savedMessagesArray[0], props.secretKey)
    const decryptedString = bytes.toString(CryptoJS.enc.Utf8)
    const savedMessage = JSON.parse(decryptedString)
    expect(savedMessage.name).toBe(chatMessage.name)
    expect(savedMessage.message).toBe(chatMessage.message)
    expect(savedMessage.color).toBe(chatMessage.color)


  })


  test('websocket should not add received message to sessionStorage if isHost false', async () => {
    // creating completely different websocket connection from beforeEach
    webSocketServer.close()
    const clonedProps = JSON.parse(JSON.stringify(props))
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


  test('addToMessagesLimitTo should add new message to the parameter Array', () => {
    const messages: Array<string> = []
    const messageString = Random.string(5)
    wrapper.vm.addToMessagesLimitTo(messages, messageString, 10)

    expect(messages[0]).toBe(messageString)
  })

  // REFACTOR:
  test('addToMessagesLimitTo should delete head data and append new data if array is over limit length', () => {
    const originalMessages: Array<string> = [
      Random.string(5),
      Random.string(5),
      Random.string(5)
    ]
    const newMessage = Random.string(5)
    const clonedMessages = JSON.parse(JSON.stringify(originalMessages))

    wrapper.vm.addToMessagesLimitTo(clonedMessages, newMessage, 3)

    expect(clonedMessages[0]).toBe(originalMessages[1])
    expect(clonedMessages[1]).toBe(originalMessages[2])
    expect(clonedMessages[2]).toBe(newMessage)
  })


  test('encrypt should encrypt sending message and decrypt should decrypt the encrypted string to json', () => {
    const sendingMessage = Random.string(5)
    wrapper.vm.sendingMessage = sendingMessage

    const encryptedMessage = wrapper.vm.encrypt()
    expect(encryptedMessage).not.toMatch(sendingMessage) // not necessary, but just checking

    const decryptedMessage: ChatMessage = wrapper.vm.decrypt(encryptedMessage)

    expect(decryptedMessage.name).toBe(wrapper.props().codename)
    expect(decryptedMessage.message).toBe(sendingMessage)
    expect(decryptedMessage.color).toBe(wrapper.vm.color)
    
  })


  test('save should save given string to sessionStorage', () => {
    const encryptedMessage = Random.string(5)
    wrapper.vm.save(encryptedMessage)

    const savedMessagesString = window.sessionStorage.getItem(wrapper.props().sessionName)!
    const savedMessagesArray: Array<string> = JSON.parse(savedMessagesString)
    expect(savedMessagesArray[0]).toBe(encryptedMessage)
  })


  test('getSavedMessage should return all encrypted stored messages as array', () => {
    const encryptedMessage = Random.string(5)
    wrapper.vm.save(encryptedMessage)

    const result = wrapper.vm.getSavedMessages()
    expect(result[0]).toBe(encryptedMessage)
  })


  test('getSavedMessage should return empty array if no message is stored', () => {
    const result = wrapper.vm.getSavedMessages()
    expect(result).toEqual([])
  })

  test('eraseMessage should delete one oldest element in receivedMessages', () => {
    const numMessages = Random.int(1, 5)
    const chatMessages = createFakeChatMessages(numMessages)
    const clonedMessages = JSON.parse(JSON.stringify(chatMessages))
    wrapper.vm.receivedMessages = chatMessages

    wrapper.vm.eraseMessage()

    expect(wrapper.vm.receivedMessages.length).toBe(numMessages - 1)
    for (let i = 0; i < (numMessages - 1); i++) {
      expect(chatMessages[i].name).toBe(clonedMessages[i + 1].name)
      expect(chatMessages[i].message).toBe(clonedMessages[i + 1].message)
      expect(chatMessages[i].color).toBe(clonedMessages[i + 1].color)
    }
  })



  test('eraseMessage should delete one oldest element in sessionStorage', () => {
    const numMessages = Random.int(1, 5)
    const chatMessages = createFakeChatMessages(numMessages)
    const clonedMessages = JSON.parse(JSON.stringify(chatMessages))
    wrapper.vm.receivedMessages = JSON.parse(JSON.stringify(chatMessages))
    wrapper.vm.storedEncryptedMessages = JSON.parse(JSON.stringify(chatMessages))

    wrapper.vm.eraseMessage()

    const sessionDataString = window.sessionStorage.getItem(wrapper.props().sessionName)!
    const sessionMessageArray = JSON.parse(sessionDataString)

    expect(sessionMessageArray.length).toBe(clonedMessages.length - 1)
    for(let i = 0; i < (numMessages - 1); i++) {
      expect(sessionMessageArray[i].name).toBe(clonedMessages[i + 1].name)
      expect(sessionMessageArray[i].message).toBe(clonedMessages[i + 1].message)
      expect(sessionMessageArray[i].color).toBe(clonedMessages[i + 1].color)
    }

  })

  test('showMessage should add message to receivedMessages', () => {
    const numMessages = Random.int(1, 5)
    const decryptedMessages = createFakeChatMessages(numMessages)
    
    decryptedMessages.forEach(message => {
      wrapper.vm.showMessage(message)
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
      color: Random.string(3)
    }
  }
})
