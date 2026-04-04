/**
 * 路由缓存状态管理
 */
import { defineStore } from 'pinia'

interface CachedView {
  name: string
  path: string
  title?: string
}

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [] as CachedView[],
    cachedViews: [] as string[],
    affixTags: [
      { name: 'Dashboard', path: '/dashboard', title: '工作台' }
    ] as CachedView[]
  }),

  actions: {
    /**
     * 添加访问过的视图
     */
    addVisitedView(view: CachedView) {
      if (this.visitedViews.some(v => v.path === view.path)) return
      this.visitedViews.push({ ...view })
    },

    /**
     * 添加缓存视图
     */
    addCachedView(name: string) {
      if (name && !this.cachedViews.includes(name)) {
        this.cachedViews.push(name)
      }
    },

    /**
     * 删除访问过的视图
     */
    delVisitedView(path: string) {
      this.visitedViews = this.visitedViews.filter(v => v.path !== path)
    },

    /**
     * 删除缓存视图
     */
    delCachedView(name: string) {
      this.cachedViews = this.cachedViews.filter(v => v !== name)
    },

    /**
     * 关闭其他标签
     */
    closeOtherTags(path: string) {
      this.visitedViews = this.visitedViews.filter(v => 
        v.path === path || this.affixTags.some(t => t.path === v.path)
      )
    },

    /**
     * 关闭左侧标签
     */
    closeLeftTags(path: string) {
      const index = this.visitedViews.findIndex(v => v.path === path)
      if (index > 0) {
        this.visitedViews = this.visitedViews.filter((v, i) => 
          i >= index || this.affixTags.some(t => t.path === v.path)
        )
      }
    },

    /**
     * 关闭右侧标签
     */
    closeRightTags(path: string) {
      const index = this.visitedViews.findIndex(v => v.path === path)
      if (index < this.visitedViews.length - 1) {
        this.visitedViews = this.visitedViews.filter((v, i) => 
          i <= index || this.affixTags.some(t => t.path === v.path)
        )
      }
    },

    /**
     * 关闭所有标签
     */
    closeAllTags() {
      this.visitedViews = [...this.affixTags]
      this.cachedViews = []
    },

    /**
     * 清理缓存（保持缓存数量在合理范围）
     */
    cleanupCache(maxCache: number = 15) {
      if (this.cachedViews.length > maxCache) {
        // 只保留最近访问的缓存
        const recentCached = this.cachedViews.slice(-maxCache)
        this.cachedViews = recentCached
      }
      
      // 清理已关闭的缓存
      const activeViewNames = this.visitedViews
        .filter(v => v.name)
        .map(v => v.name)
      
      this.cachedViews = this.cachedViews.filter(name => 
        activeViewNames.includes(name) || 
        this.affixTags.some(t => t.name === name)
      )
    },

    /**
     * 刷新指定视图（清除缓存）
     */
    refreshView(path: string) {
      const view = this.visitedViews.find(v => v.path === path)
      if (view?.name) {
        this.delCachedView(view.name)
      }
    },

    /**
     * 批量添加缓存视图
     */
    batchAddCachedViews(names: string[]) {
      names.forEach(name => {
        if (name && !this.cachedViews.includes(name)) {
          this.cachedViews.push(name)
        }
      })
    }
  },

  // 持久化配置
  persist: {
    key: 'ai-ready-tags-view',
    storage: localStorage,
    paths: ['visitedViews', 'cachedViews']
  }
})