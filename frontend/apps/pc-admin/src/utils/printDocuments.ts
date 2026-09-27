/**
 * 多张单据合并成一次打印。
 *
 * 合并是**客户端本分**：后端没有「多单据一次打印」的能力，`/documents/{pageCode}/{id}/render`
 * 一次只渲染一张。所以这里逐张取 HTML（挑模板 / 取数 / 渲染都在服务端），再拼成一份文档发给打印机。
 *
 * ⚠️ 合并时**不能把整份渲染结果塞进 `<body>`**：每张结果都是完整 HTML（含自己的 `<style>`
 * 与 `@page`），文档套文档会让内层 `<head>/<style>` 被浏览器丢弃，模板样式与纸张尺寸随之失效。
 * 这里抽出各自 body 内容拼接，样式取第一份（同一 pageCode 的模板，引擎 CSS 一致）。
 */
import { message } from 'ant-design-vue'
import { printingApi } from '@/api/printing'

/** 取出完整 HTML 文档里的 <style> 块（合并多份文档时集中放 head） */
export function extractStyles(html: string): string {
  return (html.match(/<style[\s\S]*?<\/style>/gi) || []).join('\n')
}

/** 取出 <body> 内部内容；没有 body 标记就原样返回 */
export function extractBody(html: string): string {
  const m = html.match(/<body[^>]*>([\s\S]*)<\/body>/i)
  return m ? m[1] : html
}

/**
 * 连续打印若干张单据（按数组顺序）。
 *
 * @param pageCode 页面编码 —— 必须有后端 PrintDataProvider 与已发布模板，否则取不到 HTML
 * @param ids      单据主键
 * @param docTitle 提示文案里的单据名（如「销售订单」）
 * @returns 是否真的发出了打印 —— 调用方据此决定要不要回写打印次数、刷新列表
 */
export async function printDocuments(
  pageCode: string,
  ids: Array<number | string>,
  docTitle = '单据',
): Promise<boolean> {
  const parts: string[] = []
  for (const id of ids) {
    const res: any = await printingApi.renderDocument(pageCode, id)
    const html = res?.data?.html || res?.html || ''
    if (html) parts.push(html)
  }
  if (!parts.length) {
    message.warning('没有可打印的内容：请确认单据存在，且「打印模板」里有该页面已发布的模板')
    return false
  }

  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  const doc = iframe.contentDocument || iframe.contentWindow?.document
  if (!doc) {
    message.error('无法创建打印窗口')
    document.body.removeChild(iframe)
    return false
  }
  doc.open()
  doc.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${docTitle}打印</title>
    ${extractStyles(parts[0])}
    <style>
      body{margin:0;font-family:SimSun,serif;font-size:12px}
      .merged-doc{page-break-after:always}
      .merged-doc:last-child{page-break-after:auto}
    </style></head><body>
    ${parts.map(h => `<div class="merged-doc">${extractBody(h)}</div>`).join('')}
    </body></html>`)
  doc.close()
  await new Promise(r => setTimeout(r, 300))
  iframe.contentWindow?.focus()
  iframe.contentWindow?.print()
  setTimeout(() => document.body.removeChild(iframe), 1000)
  message.success(`已发送 ${parts.length} 张${docTitle}到打印机`)
  return true
}
