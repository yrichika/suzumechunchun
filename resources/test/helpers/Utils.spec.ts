import Utils from '@app/helpers/Utils'
import Random from '@test/testhelpers/Random'

describe('Utils' , () => {

  let docBaseElement: HTMLDivElement


  /**
   * image of html tags
   * <body>
   *   <div>
   *     <any-testing-element></any-testing-element>
   *   </div>
   * </body>
   */
  beforeEach(() => {
    document.body.innerHTML = ''
    docBaseElement = document.createElement('div')
    docBaseElement.setAttribute('id' , 'main')
    document.body.appendChild(docBaseElement)
  })

  test('copyToClipboard should copy selected text to clipboard', () => {
    document.execCommand = jest.fn()

    const inputText = document.createElement('input')
    const copyFieldTagId = Random.string(5)
    inputText.setAttribute('type', 'text')
    inputText.setAttribute('id', copyFieldTagId)
    docBaseElement.appendChild(inputText)
    const copyField = document.getElementById(copyFieldTagId) as HTMLInputElement
    copyField.value = Random.string(5)

    Utils.copyToClipboard(copyFieldTagId)

    expect(document.execCommand).toHaveBeenCalledWith("copy");

  })

  test('copyToClipboard should not call execCommand for copy', () => {
    document.execCommand = jest.fn()

    const inputText = document.createElement('input')
    const copyFieldTagId = Random.string(5)
    inputText.setAttribute('type', 'text')
    inputText.setAttribute('id', copyFieldTagId)
    docBaseElement.appendChild(inputText)
    const copyField = document.getElementById(copyFieldTagId) as HTMLInputElement
    copyField.value = Random.string(5)

    const wrongTagId = Random.string(6)

    Utils.copyToClipboard(wrongTagId)

    expect(document.execCommand).not.toHaveBeenCalledWith("copy");

  })

  test('breakLines should replace line feed code to br tag', () => {
    const result1 = Utils.breakLines('aaa\nbbb')
    expect(result1).toMatch('aaa<br>bbb')

    const result2 = Utils.breakLines('aaa\rbbb')
    expect(result2).toMatch('aaa<br>bbb')

    const resultWin = Utils.breakLines('aaa\r\nbbb')
    expect(resultWin).toMatch('aaa<br>bbb')
  })

  test('htmlspecialcharsJs should convert & to &amp; within text', () => {
    const input = 'abc&abc'
    const expected = 'abc&amp;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })


  test('htmlspecialcharsJs should convert " to &quot; within text', () => {
    const input = 'abc"abc'
    const expected = 'abc&quot;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })


  test('htmlspecialcharsJs should convert \' to &apos; within text', () => {
    const input = 'abc\'abc'
    const expected = 'abc&apos;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })


  test('htmlspecialcharsJs should convert < to &lt; within text', () => {
    const input = 'abc<abc'
    const expected = 'abc&lt;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })


  test('htmlspecialcharsJs should convert > to &gt; within text', () => {
    const input = 'abc>abc'
    const expected = 'abc&gt;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })


  test('htmlspecialcharsJs should convert multiple special chars at once', () => {
    const input = 'abc<>abc'
    const expected = 'abc&lt;&gt;abc'
    const result = Utils.htmlspecialcharsJs(input)
    expect(result).toBe(expected)
  })



  test('disableButtons should disable sequential buttons with prefixes', () => {
    const buttonA1 = document.createElement('button') as HTMLButtonElement
    buttonA1.setAttribute('id', 'test-a-1')
    const buttonA2 = document.createElement('button') as HTMLButtonElement
    buttonA2.setAttribute('id', 'test-a-2')
    const buttonB1 = document.createElement('button') as HTMLButtonElement
    buttonB1.setAttribute('id', 'test-b-1')
    const buttonB2 = document.createElement('button') as HTMLButtonElement
    buttonB2.setAttribute('id', 'test-b-2')

    docBaseElement.appendChild(buttonA1)
    docBaseElement.appendChild(buttonA2)
    docBaseElement.appendChild(buttonB1)
    docBaseElement.appendChild(buttonB2)

    for (let index = 1; index <= 2; index++) {
      Utils.disableButtons(index, ['test-a-', 'test-b-'])
    }

    expect(buttonA1.disabled).toBeTruthy()
    expect(buttonA2.disabled).toBeTruthy()
    expect(buttonB1.disabled).toBeTruthy()
    expect(buttonB2.disabled).toBeTruthy()

  })


  test('hideTips should hide all tips with specified selector', () => {
    const className = 'tip'
    const selector = '.' + className
    const p1 = document.createElement('p') as HTMLParagraphElement
    p1.setAttribute('class', className)
    const p2 = document.createElement('p') as HTMLParagraphElement
    p2.setAttribute('class', className)
    docBaseElement.appendChild(p1)
    docBaseElement.appendChild(p2)

    Utils.hideTips(selector)

    expect(p1.style.display).toBe('none')
    expect(p2.style.display).toBe('none')
  })


  test('toggleVisibilityBySelector should toggle css display none property', () => {
    const className = 'tip'
    const selector = '.' + className
    const p1 = document.createElement('p') as HTMLParagraphElement
    p1.setAttribute('class', className)
    const p2 = document.createElement('p') as HTMLParagraphElement
    p2.setAttribute('class', className)
    const paragraphElements = [p1, p2]
    paragraphElements.forEach(paragraph => docBaseElement.appendChild(paragraph))

    const eventStub: any = {
      preventDefault: jest.fn()
    }

    Utils.toggleVisibilityBySelector(eventStub, selector)

    paragraphElements.forEach(paragraph => {
      expect(paragraph.style.display).toBe('none')
    })

    Utils.toggleVisibilityBySelector(eventStub, selector)

    paragraphElements.forEach(paragraph => {
      expect(paragraph.style.display).toBe('')
    })

  })
})
