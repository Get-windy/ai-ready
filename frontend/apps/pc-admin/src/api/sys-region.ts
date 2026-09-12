import request from '@/utils/request'

/** 行政区划（省 / 市 / 区县） */
export interface SysRegion {
  id: number
  code: string
  name: string
  parentCode?: string | null
  regionLevel: number
  sortOrder?: number
  children?: SysRegion[]
}

/** 省市区三级树的内存缓存（同一会话只请求一次；行政区划为低频变更的系统级数据） */
let treeCache: Promise<SysRegion[]> | null = null

export const sysRegionApi = {
  /** 按上级代码查询下级（懒加载场景） */
  children(parentCode?: string): Promise<SysRegion[]> {
    return request.get('/sys/region/children', { params: { parentCode } })
  },

  /** 省市区三级树（带会话内缓存） */
  tree(force = false): Promise<SysRegion[]> {
    if (force || !treeCache) {
      treeCache = request.get('/sys/region/tree')
        .then((res: any) => (Array.isArray(res) ? res : (res?.data || [])) as SysRegion[])
        .catch((e: any) => {
          treeCache = null
          throw e
        })
    }
    return treeCache
  },

  /** 清空缓存（行政区划数据更新后调用） */
  clearCache() {
    treeCache = null
  },
}
