import request from './request'

export interface GenerationPoetry {
  id?: number
  surname: string
  branchName: string
  title: string
  generationSequence: string
  interpretation: string
  startGeneration: number
  enabled: boolean
  creatorId?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// 字辈诗API
export const generationPoetryApi = {
  // 分页查询字辈诗
  getPage: (params: {
    page: number
    pageSize: number
    surname?: string
    branch?: string
  }) => {
    return request.get<PageResult<GenerationPoetry>>('/generation-poetry', { params })
  },

  // 获取所有启用的字辈诗
  getEnabledList: () => {
    return request.get<GenerationPoetry[]>('/generation-poetry/enabled')
  },

  // 根据姓氏获取字辈诗
  getBySurname: (surname: string) => {
    return request.get<GenerationPoetry[]>(`/generation-poetry/by-surname/${surname}`)
  },

  // 获取字辈诗详情
  getById: (id: number) => {
    return request.get<GenerationPoetry>(`/generation-poetry/${id}`)
  },

  // 创建字辈诗
  create: (data: GenerationPoetry) => {
    return request.post<GenerationPoetry>('/generation-poetry', data)
  },

  // 更新字辈诗
  update: (id: number, data: GenerationPoetry) => {
    return request.put<GenerationPoetry>(`/generation-poetry/${id}`, data)
  },

  // 删除字辈诗
  delete: (id: number) => {
    return request.delete(`/generation-poetry/${id}`)
  },

  // 启用/禁用字辈诗
  toggleStatus: (id: number, enabled: boolean) => {
    return request.put(`/generation-poetry/${id}/enable`, {}, {
      params: { enabled }
    })
  }
}