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
  get:    (path)        => request('GET',    path),
  post:   (path, body)  => request('POST',   path, body),
  put:    (path, body)  => request('PUT',    path, body),
  delete: (path)        => request('DELETE', path),
}

export const authApi = {
  login:  (email, password) => api.post('/auth/login', { email, password }),
  logout: ()                => api.post('/auth/logout'),
  me:     ()                => api.get('/auth/me'),
}

export const employeeApi = {
  list:       ()             => api.get('/employees'),
  get:        (id)           => api.get(`/employees/${id}`),
  create:     (data)         => api.post('/employees', data),
  update:     (id, data)     => api.put(`/employees/${id}`, data),
  deactivate: (id)           => api.post(`/employees/${id}/deactivate`),
  reactivate: (id)           => api.post(`/employees/${id}/reactivate`),
  assignRoles:(id, roleIds)  => api.put(`/employees/${id}/roles`, { roleIds }),
  roles:      ()             => api.get('/employees/available-roles'),
}
