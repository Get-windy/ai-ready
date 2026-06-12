import request from '@/utils/request'

export const dmsApi = {
  // Rider
  getRiderInfo: (userId: number) => request.get(`/api/dms/rider/${userId}`),
  updateLocation: (data: { riderId: number; lat: number; lng: number }) => request.post('/api/dms/rider/location', data),
  updateStatus: (id: number, status: number) => request.post(`/api/dms/rider/${id}/status`, { status }),

  // Vehicle binding
  getActiveBinding: (riderId: number) => request.get(`/api/dms/verification/binding/active/rider/${riderId}`),
  createInspection: (data: any) => request.post('/api/dms/verification/inspection', data),
  bindVehicle: (data: any) => request.post('/api/dms/verification/bind', data),
  handover: (id: number, data: any) => request.post(`/api/dms/verification/${id}/handover`, data),

  // Tasks
  getMyTasks: (riderId: number, params: any) => request.get('/api/dms/task/page', { params: { ...params, riderId } }),
  updateTaskStatus: (id: number, status: number) => request.put(`/api/dms/task/${id}/status`, null, { params: { status } }),

  // Sign & Payment
  submitSign: (data: any) => request.post('/api/dms/sign/submit', data),
  confirmPayment: (data: any) => request.post('/api/dms/payment/confirm', data),

  // Tracking
  reportLocation: (data: { taskId: number; riderId: number; lat: number; lng: number; timestamp?: string }) => request.post('/api/dms/tracking/report', data),
}
