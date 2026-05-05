const BASE = '/api'

async function request(method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    credentials: 'include',
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 204) return null

  const data = await res.json().catch(() => null)

  if (!res.ok) {
    const message = data?.error || `HTTP ${res.status}`
    throw new Error(message)
  }

  return data
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  delete: (path) => request('DELETE', path),
}

export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
}

export const employeeApi = {
  list: () => api.get('/employees'),
  get: (id) => api.get(`/employees/${id}`),
  create: (data) => api.post('/employees', data),
  update: (id, data) => api.put(`/employees/${id}`, data),
  deactivate: (id) => api.post(`/employees/${id}/deactivate`),
  reactivate: (id) => api.post(`/employees/${id}/reactivate`),
  assignRoles: (id, roleIds) => api.put(`/employees/${id}/roles`, { roleIds }),
  roles: () => api.get('/employees/available-roles'),
}

export const departmentApi = {
  list: () => api.get('/departments'),
}

const toLocalIsoDate = (date) => {
  const d = new Date(date)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

const isoWeek = (date) => toLocalIsoDate(date)

export const shiftApi = {
  list: (weekStart) => api.get(`/shifts?weekStart=${isoWeek(weekStart)}`),
  listMine: (weekStart) => api.get(`/shifts/my?weekStart=${isoWeek(weekStart)}`),
  listPublished: (weekStart) => api.get(`/shifts/all-published?weekStart=${isoWeek(weekStart)}`),
  get: (id) => api.get(`/shifts/${id}`),
  create: (data) => api.post('/shifts', data),
  update: (id, data) => api.put(`/shifts/${id}`, data),
  delete: (id) => api.delete(`/shifts/${id}`),
  publish: (id, force = false) => api.post(`/shifts/${id}/publish?force=${force}`),
  publishWeek: (weekStart, force = false) => api.post(`/shifts/publish-week?weekStart=${isoWeek(weekStart)}&force=${force}`),
  unpublish: (id) => api.post(`/shifts/${id}/unpublish`),
  setCoverage: (id, data) => api.post(`/shifts/${id}/coverage`, data),
  assign: (shiftId, data) => api.post(`/shifts/${shiftId}/assignments`, data),
  unassign: (shiftId, assignmentId) => api.delete(`/shifts/${shiftId}/assignments/${assignmentId}`),
}

export const notificationApi = {
  list: () => api.get('/notifications'),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.post(`/notifications/${id}/read`),
  markAllRead: () => api.post('/notifications/read-all'),
}

export const timeOffApi = {
  request: (data) => api.post('/time-off', data),
  getMy: () => api.get('/time-off/my'),
  getPending: () => api.get('/time-off/pending'),
  approve: (id) => api.post(`/time-off/${id}/approve`),
  reject: (id) => api.post(`/time-off/${id}/reject`),
}

export const swapApi = {
  request: (data) => api.post('/swap-requests', data),
  getMy: () => api.get('/swap-requests/my'),
  getPending: () => api.get('/swap-requests/pending'),
  approve: (id) => api.post(`/swap-requests/${id}/approve`),
  reject: (id) => api.post(`/swap-requests/${id}/reject`),
}

export const reportApi = {
  hours: (weekStart) => api.get(`/reports/hours?weekStart=${isoWeek(weekStart)}`),
}

export const availabilityApi = {
  get: (employeeId) => api.get(`/employees/${employeeId}/availability`),
  set: (employeeId, windows) => api.put(`/employees/${employeeId}/availability`, { windows }),
}
