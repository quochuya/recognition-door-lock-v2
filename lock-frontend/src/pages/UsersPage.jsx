import React, { useEffect, useState } from "react";
import { UserPlus, Ban, Check, Pencil, Trash2, X } from "lucide-react";
import api from "../api/client";

const ROLES = ["ADMIN", "STAFF", "RESIDENT", "GUEST", "SECURITY"];

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [showAdd, setShowAdd] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form, setForm] = useState({ fullName: "", email: "", password: "", phone: "", role: "RESIDENT" });

  const load = () => api.get("/api/users").then(({ data }) => setUsers(data));
  useEffect(() => { load(); }, []);

  const submit = async (e) => {
    e.preventDefault();
    await api.post("/api/users", form);
    setShowAdd(false);
    setForm({ fullName: "", email: "", password: "", phone: "", role: "RESIDENT" });
    load();
  };

  const deactivate = async (id) => { await api.post(`/api/users/${id}/deactivate`); load(); };
  const reactivate = async (id) => { await api.post(`/api/users/${id}/reactivate`); load(); };
  const remove = async (id) => {
    if (!confirm("Xoá vĩnh viễn user này? Chỉ xoá được nếu chưa có thẻ/lịch sử liên quan.")) return;
    try {
      await api.delete(`/api/users/${id}`);
      load();
    } catch (e) {
      alert(e.response?.data?.message || "Không xoá được — thử vô hiệu hoá thay vì xoá cứng");
    }
  };

  const saveEdit = async (data) => {
    await api.put(`/api/users/${editingUser.id}`, data);
    setEditingUser(null);
    load();
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <button onClick={() => setShowAdd(true)} className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium">
          <UserPlus size={16} /> Thêm người dùng
        </button>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
              <th className="text-left font-semibold px-5 py-3">Họ tên</th>
              <th className="text-left font-semibold px-5 py-3">Email</th>
              <th className="text-left font-semibold px-5 py-3">Vai trò</th>
              <th className="text-left font-semibold px-5 py-3">Trạng thái</th>
              <th className="text-left font-semibold px-5 py-3">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-t border-slate-100">
                <td className="px-5 py-3 font-medium text-slate-700">{u.fullName}</td>
                <td className="px-5 py-3 text-slate-600">{u.email}</td>
                <td className="px-5 py-3 text-slate-600">{u.role}</td>
                <td className="px-5 py-3">
                  {u.active ? (
                    <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium">Hoạt động</span>
                  ) : (
                    <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-medium">Đã khoá</span>
                  )}
                </td>
                <td className="px-5 py-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    {u.active ? (
                      <button onClick={() => deactivate(u.id)} className="text-xs text-rose-500 font-medium flex items-center gap-1"><Ban size={13} /> Vô hiệu hoá</button>
                    ) : (
                      <button onClick={() => reactivate(u.id)} className="text-xs text-emerald-600 font-medium flex items-center gap-1"><Check size={13} /> Kích hoạt lại</button>
                    )}
                    <button onClick={() => setEditingUser(u)} className="text-xs text-slate-500 font-medium flex items-center gap-1"><Pencil size={12} /> Sửa</button>
                    <button onClick={() => remove(u.id)} className="text-xs text-slate-400 hover:text-rose-500 font-medium flex items-center gap-1"><Trash2 size={13} /> Xoá</button>
                  </div>
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-16 text-center text-slate-400">Chưa có người dùng nào</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {showAdd && (
        <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={() => setShowAdd(false)}>
          <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-lg font-semibold">Thêm người dùng</h3>
              <button type="button" onClick={() => setShowAdd(false)} className="text-slate-400"><X size={18} /></button>
            </div>
            <input required placeholder="Họ tên" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
            <input required type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
            <input required type="password" placeholder="Mật khẩu" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
            <input placeholder="Số điện thoại" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none">
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <button type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium">Tạo tài khoản</button>
          </form>
        </div>
      )}

      {editingUser && (
        <EditUserModal user={editingUser} onClose={() => setEditingUser(null)} onSave={saveEdit} />
      )}
    </div>
  );
}

function EditUserModal({ user, onClose, onSave }) {
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone || "");
  const [role, setRole] = useState(user.role);
  const [active, setActive] = useState(user.active);

  const submit = async (e) => {
    e.preventDefault();
    await onSave({ fullName, phone, role, active });
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold">Sửa người dùng</h3>
          <button type="button" onClick={onClose} className="text-slate-400"><X size={18} /></button>
        </div>
        <input required placeholder="Họ tên" value={fullName} onChange={(e) => setFullName(e.target.value)} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
        <input placeholder="Số điện thoại" value={phone} onChange={(e) => setPhone(e.target.value)} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
        <select value={role} onChange={(e) => setRole(e.target.value)} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none">
          {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Tài khoản đang hoạt động
        </label>
        <button type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium">Lưu</button>
      </form>
    </div>
  );
}
