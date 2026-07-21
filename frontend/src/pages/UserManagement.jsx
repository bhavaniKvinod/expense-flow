import { useEffect, useState } from 'react'
import client, { errorMessage } from '../api/client'
import { ROLES } from '../util/format'

const emptyForm = { name: '', email: '', password: '', role: 'EMPLOYEE', managerId: '' }

export default function UserManagement() {
  const [users, setUsers] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [creating, setCreating] = useState(false)

  function load() {
    client
      .get('/users')
      .then((res) => setUsers(res.data))
      .catch((err) => setError(errorMessage(err)))
  }

  useEffect(load, [])

  const managers = (users || []).filter((u) => u.role === 'MANAGER' || u.role === 'ADMIN')

  async function createUser(e) {
    e.preventDefault()
    setCreating(true)
    setError(null)
    setNotice(null)
    try {
      await client.post('/users', {
        ...form,
        managerId: form.managerId ? Number(form.managerId) : null,
      })
      setNotice(`User ${form.email} created.`)
      setForm(emptyForm)
      load()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setCreating(false)
    }
  }

  async function saveUser(u, changes) {
    setError(null)
    setNotice(null)
    try {
      await client.put(`/users/${u.id}`, {
        name: changes.name ?? u.name,
        role: changes.role ?? u.role,
        managerId: changes.managerId !== undefined ? changes.managerId : u.managerId,
        active: changes.active !== undefined ? changes.active : u.active,
      })
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Users</h1>
          <div className="subtitle">Create users, assign roles and managers, deactivate accounts.</div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      <div className="card">
        <h2>Add user</h2>
        <form onSubmit={createUser}>
          <div className="form-row">
            <label className="field">
              <span className="lbl">Name</span>
              <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </label>
            <label className="field">
              <span className="lbl">Email</span>
              <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label className="field">
              <span className="lbl">Password</span>
              <input type="text" required minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </label>
            <label className="field">
              <span className="lbl">Role</span>
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </label>
            <label className="field">
              <span className="lbl">Manager</span>
              <select value={form.managerId} onChange={(e) => setForm({ ...form, managerId: e.target.value })}>
                <option value="">— none —</option>
                {managers.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
            </label>
          </div>
          <button className="btn-primary" disabled={creating}>{creating ? 'Creating…' : 'Create user'}</button>
        </form>
      </div>

      <div className="card">
        <h2>All users</h2>
        {!users ? (
          <div className="center-note">Loading…</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Manager</th>
                <th>Active</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td className="muted">{u.email}</td>
                  <td>
                    <select value={u.role} onChange={(e) => saveUser(u, { role: e.target.value })}>
                      {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </td>
                  <td>
                    <select
                      value={u.managerId ?? ''}
                      onChange={(e) => saveUser(u, { managerId: e.target.value ? Number(e.target.value) : null })}
                    >
                      <option value="">— none —</option>
                      {managers.filter((m) => m.id !== u.id).map((m) => (
                        <option key={m.id} value={m.id}>{m.name}</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      style={{ width: 'auto' }}
                      checked={u.active}
                      onChange={(e) => saveUser(u, { active: e.target.checked })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
