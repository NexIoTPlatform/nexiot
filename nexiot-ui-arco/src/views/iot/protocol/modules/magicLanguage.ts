import type { MagicFunction } from '../api/debugger'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import 'monaco-editor/esm/vs/editor/browser/coreCommands.js'
import 'monaco-editor/esm/vs/editor/browser/widget/codeEditor/codeEditorWidget.js'
import 'monaco-editor/esm/vs/editor/browser/widget/diffEditor/diffEditor.contribution.js'
import 'monaco-editor/esm/vs/editor/contrib/bracketMatching/browser/bracketMatching.js'
import 'monaco-editor/esm/vs/editor/contrib/clipboard/browser/clipboard.js'
import 'monaco-editor/esm/vs/editor/contrib/comment/browser/comment.js'
import 'monaco-editor/esm/vs/editor/contrib/contextmenu/browser/contextmenu.js'
import 'monaco-editor/esm/vs/editor/contrib/find/browser/findController.js'
import 'monaco-editor/esm/vs/editor/contrib/folding/browser/folding.js'
import 'monaco-editor/esm/vs/editor/contrib/hover/browser/hoverContribution.js'
import 'monaco-editor/esm/vs/editor/contrib/indentation/browser/indentation.js'
import 'monaco-editor/esm/vs/editor/contrib/parameterHints/browser/parameterHints.js'
import 'monaco-editor/esm/vs/editor/contrib/snippet/browser/snippetController2.js'
import 'monaco-editor/esm/vs/editor/contrib/suggest/browser/suggestController.js'
import 'monaco-editor/esm/vs/editor/contrib/tokenization/browser/tokenization.js'
import 'monaco-editor/esm/vs/editor/contrib/wordOperations/browser/wordOperations.js'
import 'monaco-editor/esm/vs/editor/contrib/linesOperations/browser/linesOperations.js'

let registered = false
export function setupMagicLanguage() {
  if (registered) return
  registered = true
  const environment = globalThis as typeof globalThis & { MonacoEnvironment: monaco.Environment }
  environment.MonacoEnvironment = { getWorker: () => new EditorWorker() }
  monaco.editor.defineTheme('nexiot-magic-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [],
    colors: { 'editor.background': '#1e1e1e', 'editorLineNumber.foreground': '#858585', 'editor.lineHighlightBackground': '#2a2d2e', 'editor.selectionBackground': '#264f78', 'editorGutter.background': '#1e1e1e' }
  })
  monaco.languages.register({ id: 'nexiot-magic' })
  monaco.languages.setLanguageConfiguration('nexiot-magic', {
    comments: { lineComment: '//', blockComment: ['/*', '*/'] },
    brackets: [['{', '}'], ['[', ']'], ['(', ')']],
    autoClosingPairs: [{ open: '{', close: '}' }, { open: '[', close: ']' }, { open: '(', close: ')' }, { open: '"', close: '"' }, { open: '\'', close: '\'' }]
  })
  monaco.languages.setMonarchTokensProvider('nexiot-magic', {
    keywords: ['var', 'let', 'const', 'return', 'if', 'else', 'for', 'while', 'in', 'import', 'as', 'new', 'try', 'catch', 'finally', 'throw', 'break', 'continue', 'function', 'true', 'false', 'null', 'async', 'await', 'select', 'from', 'where'],
    tokenizer: {
      root: [
        [/\/\*/, 'comment', '@comment'],
        [/\/\/.*$/, 'comment'],
        [/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/, 'string'],
        [/\b\d+(?:\.\d+)?[fdl]?\b/i, 'number'],
        [/[a-z_$][\w$]*/i, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],
        [/[{}()[\]]/, '@brackets'],
        [/[+\-*/%=!<>?:&|]+/, 'operator']
      ],
      comment: [[/[^/*]+/, 'comment'], [/\*\//, 'comment', '@pop'], [/[/*]/, 'comment']]
    }
  })
}

function docs(fn: MagicFunction) {
  return `${fn.description}\n\n${fn.params.map((p) => `- **${p.name}** (${p.type})：${p.description || '参数'}`).join('\n')}\n\n返回：${fn.returnType} · ${fn.group}`
}

// 识别嵌套调用的当前参数，跳过字符串、注释内的括号和逗号。
function currentCall(text: string) {
  const clean = text.replace(/\/\*[\s\S]*?\*\/|\/\/[^\n]*|"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/g, (m) => ' '.repeat(m.length))
  const stack: { name: string, argument: number, kind: string }[] = []
  for (let i = 0; i < clean.length; i++) {
    const c = clean[i]
    if ('([{'.includes(c)) stack.push({ name: c === '(' ? (clean.slice(0, i).match(/([\w$]+)\s*$/)?.[1] || '') : '', argument: 0, kind: c })
    else if (')]}'.includes(c)) stack.pop()
    else if (c === ',' && stack.length) stack[stack.length - 1].argument++
  }
  return [...stack].reverse().find((frame) => frame.kind === '(' && frame.name)
}

export function registerMagicIntelligence(model: monaco.editor.ITextModel, catalog: () => MagicFunction[]) {
  const matches = (candidate: monaco.editor.ITextModel) => candidate.uri.toString() === model.uri.toString()
  return [
    monaco.languages.registerCompletionItemProvider('nexiot-magic', {
      triggerCharacters: ['_'],
      provideCompletionItems(candidate, position) {
        if (!matches(candidate)) return { suggestions: [] }
        const word = candidate.getWordUntilPosition(position)
        const range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn)
        const prefix = word.word.toLowerCase()
        // 首字母不弹出长列表；incomplete 让 Monaco 在第二个字母后重新请求。
        if (prefix.length === 1) return { suggestions: [], incomplete: true }
        const functions = catalog().filter((fn) => fn.name.toLowerCase().startsWith(prefix))
        return { incomplete: true, suggestions: functions.map((fn) => ({
          label: { label: fn.name, description: fn.description || fn.group, detail: `(${fn.params.map((p) => `${p.type} ${p.name}`).join(', ')})` },
          kind: monaco.languages.CompletionItemKind.Function,
          detail: `${fn.signature} → ${fn.returnType}`,
          documentation: { value: docs(fn) },
          insertText: `${fn.name}(${fn.params.map((p, i) => `\${${i + 1}:${p.name}}`).join(', ')})`,
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          tags: fn.deprecated ? [monaco.languages.CompletionItemTag.Deprecated] : [],
          range
        })) }
      }
    }),
    monaco.languages.registerHoverProvider('nexiot-magic', {
      provideHover(candidate, position) {
        if (!matches(candidate)) return null
        const word = candidate.getWordAtPosition(position)
        const functions = catalog().filter((fn) => fn.name === word?.word)
        if (!functions.length) return null
        return { contents: functions.flatMap((fn) => [{ value: `\`\`\`java\n${fn.returnType} ${fn.signature}\n\`\`\`` }, { value: docs(fn) }]) }
      }
    }),
    monaco.languages.registerSignatureHelpProvider('nexiot-magic', {
      signatureHelpTriggerCharacters: ['(', ','],
      provideSignatureHelp(candidate, position) {
        if (!matches(candidate)) return null
        const call = currentCall(candidate.getValueInRange(new monaco.Range(1, 1, position.lineNumber, position.column)))
        const functions = catalog().filter((fn) => fn.name === call?.name)
        if (!call || !functions.length) return null
        return {
          value: {
            signatures: functions.map((fn) => ({
              label: `${fn.name}(${fn.params.map((p) => `${p.name}: ${p.type}`).join(', ')}) → ${fn.returnType}`,
              documentation: fn.description,
              parameters: fn.params.map((p) => ({ label: `${p.name}: ${p.type}`, documentation: p.description }))
            })),
            activeSignature: Math.max(0, functions.findIndex((fn) => fn.params.length > call.argument)),
            activeParameter: call.argument
          },
          dispose() {}
        }
      }
    })
  ]
}
export { monaco }
