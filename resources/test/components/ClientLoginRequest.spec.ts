import { mount } from '@vue/test-utils'
import { VueFlexTypeWrapper } from '@test/testhelpers/types/VueFlexTypeWrapper'
import Random from '@test/testhelpers/Random'
import ClientLoginRequest from '@app/components/ClientLoginRequest.vue'
import moxios from 'moxios'
import RegExpHelper from '@test/testhelpers/RegExpHelper'


describe('ClientLoginRequest' , () => {
  let wrapper: VueFlexTypeWrapper

  // TODO: Create a factory class for fake language Map.
  const en = new Map([
    ['chat-closed', Random.string(5)],
    ['send-auth', Random.string(5)],
    ['accepted', Random.string(5)],
    ['rejected', Random.string(5)],
    ['waiting-for-authentication', Random.string(5)]
  ]);
  const ja = new Map([
    ['chat-closed', Random.string(5)],
    ['send-auth', Random.string(5)],
    ['accepted', Random.string(5)],
    ['rejected', Random.string(5)],
    ['waiting-for-authentication', Random.string(5)]
  ]);
  const props = {
    sseUrl: '/sse',
    requestUrl: '/request',
    chatUrl: '/chat/',
    enMap: en,
    jaMap: ja
  };

  // mock EventSource
  (global as any).EventSource = jest.fn().mockImplementation(() => {
    return {
      close: jest.fn()
    }
  })

  beforeAll(() => {
    jest.spyOn(window, 'alert').mockImplementation(() => {})
  })

  beforeEach(() => {
    wrapper = mount(ClientLoginRequest, {
      propsData: props
    })
    moxios.install()
  })

  afterEach(() => {
    moxios.uninstall()
    jest.clearAllMocks()
  })

  test('it should set properties properly', () => {
    expect(wrapper.props().sseUrl).toBe(props.sseUrl)
    expect(wrapper.props().requestUrl).toBe(props.requestUrl)
    expect(wrapper.props().chatUrl).toBe(props.chatUrl)
  })

  test('authenticatedUrl should output concatenated string with chat url', () => {
    const token = Random.string(5)
    const expected = props.chatUrl + token
    expect(wrapper.vm.authenticatedUrl(token)).toBe(expected)
  })


  test('send should start EventSource and wait for messages', async (done) => {
    moxios.stubRequest(props.requestUrl, {
      status: 200,
      response: { message: 'ok' }
    })
    expect(wrapper.vm.eventSource).toBeNull()

    wrapper.find('#codename').setValue(Random.string(5))
    wrapper.find('#passphrase').setValue(Random.string(5))
    wrapper.find('#submit-button').trigger('click')

    moxios.wait(() => {
      expect(EventSource).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.eventSource.onmessage).toBeTruthy()
      done()
    })
  })


  test('send should show alert if receives closed status message', async (done) => {
    moxios.stubRequest(props.requestUrl, {
      status: 200,
      response: { message: wrapper.vm.closedRequestStatusString }
    })

    wrapper.find('#codename').setValue(Random.string(5))
    wrapper.find('#passphrase').setValue(Random.string(5))
    wrapper.find('#submit-button').trigger('click')

    moxios.wait(() => {
      expect(EventSource).not.toHaveBeenCalled()
      expect(wrapper.vm.eventSource).toBeNull()

      expect(window.alert).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.isClosed).toBe(true)
      expect(wrapper.vm.isWaitingForAuthentication).toBe(false)
      done()
    })
  })



  test('send should show alert if response has an error status', async (done) => {
    moxios.stubRequest(props.requestUrl, {
      status: 400,
      response: { message: 'bad request' }
    })

    wrapper.find('#codename').setValue(Random.string(5))
    wrapper.find('#passphrase').setValue(Random.string(5))
    wrapper.find('#submit-button').trigger('click')

    moxios.wait(() => {
      expect(EventSource).not.toHaveBeenCalled()
      expect(wrapper.vm.eventSource).toBeNull()

      expect(wrapper.vm.codename).toBe('')
      expect(wrapper.vm.passphrase).toBe('')
      expect(wrapper.vm.isWaitingForAuthentication).toBe(false)
      done()
    })
  })


  test('sseMessageEvent should set received data to the clientChannel property', () => {
    expect(wrapper.vm.clientChannel).toBe('')
    const fakeToken = Random.string(5)
    // const fakeMessageEvent = {
    //   data: JSON.stringify({clientChannel: fakeToken})
    // }
    const fakeMessageEvent = new MessageEvent('any', {
      data: JSON.stringify({clientChannel: fakeToken })
    })

    wrapper.vm.sseMessageEvent(fakeMessageEvent)

    expect(wrapper.vm.clientChannel).toBe(fakeToken)
  })


  test('watch.clientChannel: event source should be closed when clientChannel receives a token from server', async () => {
    wrapper.vm.eventSource = new EventSource(props.sseUrl)

    wrapper.vm.clientChannel = 'fakeToken'
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.eventSource.close).toHaveBeenCalledTimes(1)
  })





  test('errorChatClosedMessage should give either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('chat-closed')!, ja.get('chat-closed')!)
    expect(wrapper.vm.errorChatClosedMessage).toMatch(expected)
  })


  test('defaultMessage should give either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('send-auth')!, ja.get('send-auth')!)
    expect(wrapper.vm.defaultMessage).toMatch(expected)
  })


  test('acceptedMessage should give either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('accepted')!, ja.get('accepted')!)
    expect(wrapper.vm.acceptedMessage).toMatch(expected)
  })


  test('rejectedMessage should give either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('rejected')!, ja.get('rejected')!)
    expect(wrapper.vm.rejectedMessage).toMatch(expected)
  })


  test('waitingMessage should give either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('waiting-for-authentication')!, ja.get('waiting-for-authentication')!)
    expect(wrapper.vm.waitingMessage).toMatch(expected)
  })

})
