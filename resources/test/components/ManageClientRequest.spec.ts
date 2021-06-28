import { mount } from '@vue/test-utils'
import { VueFlexTypeWrapper } from '@test/testhelpers/types/VueFlexTypeWrapper'
import Random from '@test/testhelpers/Random'
import ManageClientRequest from '@app/components/ManageClientRequest.vue'
import moxios from 'moxios'
import RegExpHelper from '@test/testhelpers/RegExpHelper'
import ClientRequest from '@app/types/ClientRequest'
import Utils from '@app/helpers/Utils'

describe('ManageClientRequest' , () => {
  let wrapper: VueFlexTypeWrapper
  
  // TODO: Create a factory class for fake language Map.
  const en = new Map([
    ['manage-request-failed', Random.string(5)],
    ['confirm-closing-request', Random.string(5)],
    ['closing-request-failed', Random.string(5)]
  ]);
  const ja = new Map([
    ['manage-request-failed', Random.string(5)],
    ['confirm-closing-request', Random.string(5)],
    ['closing-request-failed', Random.string(5)]
  ]);
  const props = {
    sseUrl: '/sse',
    postUrl: '/post',
    closeRequestUrl: '/close',
    enMap: en,
    jaMap: ja
  };

  const clientRequest: ClientRequest = {
    requestClientId: Random.string(5),
    codename: Random.string(5),
    passphrase: Random.string(5),
    isAuthenticated: false
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
    wrapper = mount(ManageClientRequest, {
      propsData: props
    })
    moxios.install()

  })

  afterEach(() => {
    moxios.uninstall()
    jest.clearAllMocks()
  })

  test('it should set properties and data properly', () => {
    expect(wrapper.props().sseUrl).toBe(props.sseUrl)
    expect(wrapper.props().postUrl).toBe(props.postUrl)
    expect(wrapper.props().closeRequestUrl).toBe(props.closeRequestUrl)

    expect(wrapper.vm.dataSource.onmessage).toBeTruthy()
    expect(wrapper.vm.dataSource.onerror).toBeTruthy()
  })

  test('showStatus should return accepted string when the parameter is true', () => {
    const result = wrapper.vm.showStatus(true)
    expect(result).toBe('Accepted')
  })


  test('showStatus should return rejected string when the parameter is false', () => {
    const result = wrapper.vm.showStatus(false)
    expect(result).toBe('Rejected')
  })


  test('showStatus should return not accepted string when the parameter is null', () => {
    const result = wrapper.vm.showStatus(null)
    expect(result).toBe('Not Accepted')
  })


  test('closeRequest should turn isRequestClosed true and close EventSource', async (done) => {
    jest.spyOn(window, 'confirm').mockImplementation(() => true)
    moxios.stubRequest(props.closeRequestUrl, {
      status: 200,
      response: { message: 'ok' }
    })

    wrapper.find('#close-request-button').trigger('click')

    moxios.wait(() => {
      expect(wrapper.vm.isRequestClosed).toBe(true)
      expect(wrapper.vm.dataSource.close).toHaveBeenCalledTimes(1)
      done()
    })
  })


  test('closeRequest should do nothing if false selected on window confirmation', async (done) => {
    jest.spyOn(window, 'confirm').mockImplementation(() => false)
    moxios.stubRequest(props.closeRequestUrl, {
      status: 200,
      response: { message: 'ok' }
    })
    wrapper.find('#close-request-button').trigger('click')

    moxios.wait(() => {
      expect(wrapper.vm.isRequestClosed).toBe(false)
      expect(wrapper.vm.dataSource.close).not.toHaveBeenCalled()
      done()
    })
  })


  test('closeRequest should show alert message if response has error status', async (done) => {
    jest.spyOn(window, 'confirm').mockImplementation(() => true)
    moxios.stubRequest(props.closeRequestUrl, {
      status: 500,
      response: { message: 'error' }
    })

    wrapper.find('#close-request-button').trigger('click')

    moxios.wait(() => {
      expect(wrapper.vm.isRequestClosed).toBe(false)
      expect(wrapper.vm.dataSource.close).not.toHaveBeenCalled()

      expect(window.alert).toHaveBeenCalledTimes(1)
      done()
    })
  })

  test.skip('undefined when send succeeds', async (done) => {
    moxios.stubRequest(props.postUrl, {
      status: 200,
      response: { message: 'ok' }
    })

    moxios.wait(() => {
      //
      done()
    })
  })

  test('send should show alert message when response has error status', async (done) => {
    moxios.stubRequest(props.postUrl, {
      status: 400,
      response: { message: 'error' }
    })

    wrapper.vm.send(clientRequest, true)

    moxios.wait(() => {
      expect(window.alert).toHaveBeenCalledTimes(1)
      done()
    })
  })


  test('accept should call Utils.disableButtons', () => {
    Utils.disableButtons = jest.fn()
    wrapper.vm.accept(clientRequest, 1)
    expect(Utils.disableButtons).toHaveBeenCalledTimes(1)
  })


  test('reject should call Utils.disableButtons', () => {
    Utils.disableButtons = jest.fn()
    wrapper.vm.reject(clientRequest, 1)
    expect(Utils.disableButtons).toHaveBeenCalledTimes(1)
  })




  test('closeRequestMessage should return either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('confirm-closing-request')!, ja.get('confirm-closing-request')!)
    expect(wrapper.vm.closeRequestMessage).toMatch(expected)
  })


  test('closeRequestFailedMessage should return either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('closing-request-failed')!, ja.get('closing-request-failed')!)
    expect(wrapper.vm.closeRequestFailedMessage).toMatch(expected)
  })


  test('mangeRequestFailedMessage should return either english or japanese message', () => {
    const expected = RegExpHelper.eitherRegex(en.get('manage-request-failed')!, ja.get('manage-request-failed')!)
    expect(wrapper.vm.mangeRequestFailedMessage).toMatch(expected)
  })

})
