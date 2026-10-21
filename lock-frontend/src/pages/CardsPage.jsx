import React, { useEffect, useState } from "react";
import { CreditCard, ShieldCheck, ShieldX, Ban, Check, Radio, Pencil, Trash2, X } from "lucide-react";
import api from "../api/client";
import { connectSocket, disconnectSocket } from "../api/socket";

const STATUS_META = {
  ACTIVE: { label: "Đang hoạt động", color: "text-emerald-600", bg: "bg-emerald-50" },
  BLOCKED: { label: "Đã khóa", color: "text-rose-600", bg: "bg-rose-50" },
  EXPIRING: { label: "Sắp hết hạn", color: "text-amber-600", bg: "bg-amber-50" },
};

export default function CardsPage() {
  const [cards, setCards] = useState([]);
  const [pending, setPending] = useState([]);
  const [enrollmentOn, setEnrollmentOn] = useState(false);
  const [assigningId, setAssigningId] = useState(null);
  const [userIdInput, setUserIdInput] = useState("");

  const loadCards = () => api.get("/api/cards").then(({ data }) => setCards(data));
  const loadPending = () => api.get("/api/cards/pending").then(({ data }) => setPending(data));
  const loadEnrollmentStatus = () => api.get("/api/cards/enrollment/status").then(({ data }) => setEnrollmentOn(data));

  useEffect(() => {
    loadCards();
    loadPending();
    loadEnrollmentStatus();
    connectSocket({ onCardEnrollment: () => loadPending() });
    return () => disconnectSocket();
  }, []);

  const toggleEnrollment = async () => {
    await api.post(`/api/cards/enrollment/${enrollmentOn ? "stop" : "start"}`);
    setEnrollmentOn((v) => !v);
    loadPending();
  };

  const assignPending = async (pendingId) => {
    if (!userIdInput) return;
    await api.post(`/api/cards/pending/${pendingId}/assign`, { userId: Number(userIdInput) });
    setAssigningId(null);
    setUserIdInput("");
    loadPending();
    loadCards();
  };

  const discardPending = async (pendingId) => {
    await api.post(`/api/cards/pending/${pendingId}/discard`);
    loadPending();
  };

  const revoke = async (id) => { await api.post(`/api/cards/${id}/revoke`); loadCards(); };
  const activate = async (id) => { await api.post(`/api/cards/${id}/activate`); loadCards(); };
  const remove = async (id) => {
    if (!confirm("Xoá vĩnh viễn thẻ này?")) return;
    await api.delete(`/api/cards/${id}`);
    loadCards();
  };

  const [editingCard, setEditingCard] = useState(null);
  const saveEdit = async (form) => {
    await api.put(`/api/cards/${editingCard.id}`, {
      ownerId: form.ownerId ? Number(form.ownerId) : null,
      expiresAt: form.expiresAt || null,
    });
    setEditingCard(null);
    loadCards();
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Radio size={18} className={enrollmentOn ? "text-rose-500 animate-pulse" : "text-slate-400"} />
          <div>
            <div className="text-sm font-medium text-slate-800">Chế độ đăng ký thẻ (one-touch)</div>
            <div className="text-xs text-slate-400">Bật lên rồi đưa thẻ mới lại gần đầu đọc — UID tự hiện bên dưới</div>
          </div>
        </div>
        <button
          onClick={toggleEnrollment}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${enrollmentOn ? "bg-rose-600 text-white" : "bg-blue-600 text-white"}`}
        >
          {enrollmentOn ? "Tắt chế độ" : "Bật chế độ"}
        </button>
      </div>

      {pending.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-2">
          <div className="text-sm font-semibold text-amber-700 mb-2">Thẻ đang chờ gán ({pending.length})</div>
          {pending.map((p) => (
            <div key={p.id} className="flex items-center justify-between bg-white rounded-lg p-3">
              <div>
                <div className="font-mono text-sm text-slate-700">{p.uid}</div>
                <div className="text-xs text-slate-400">Đầu đọc: {p.deviceCode}</div>
              </div>
              {assigningId === p.id ? (
                <div className="flex items-center gap-2">
                  <input
                    value={userIdInput}
                    onChange={(e) => setUserIdInput(e.target.value)}
                    placeholder="ID người dùng"
                    className="w-28 text-sm border border-slate-200 rounded-lg px-2 py-1.5 outline-none"
                  />
                  <button onClick={() => assignPending(p.id)} className="px-3 py-1.5 rounded-lg bg-blue-600 text-white text-xs">Gán</button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <button onClick={() => setAssigningId(p.id)} className="px-3 py-1.5 rounded-lg bg-blue-50 text-blue-600 text-xs font-medium">Gán user</button>
                  <button onClick={() => discardPending(p.id)} className="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-500 text-xs">Bỏ qua</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
              <th className="text-left font-semibold px-5 py-3">Chủ thẻ</th>
              <th className="text-left font-semibold px-5 py-3">UID</th>
              <th className="text-left font-semibold px-5 py-3">Khu vực</th>
              <th className="text-left font-semibold px-5 py-3">Lượt dùng</th>
              <th className="text-left font-semibold px-5 py-3">Trạng thái</th>
              <th className="text-left font-semibold px-5 py-3">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {cards.map((c) => {
              const meta = STATUS_META[c.status] || STATUS_META.ACTIVE;
              return (
                <tr key={c.id} className="border-t border-slate-100">
                  <td className="px-5 py-3">
                    <div className="font-medium text-slate-700">{c.ownerName}</div>
                    <div className="text-xs text-slate-400">{c.ownerRole}</div>
                  </td>
                  <td className="px-5 py-3 font-mono text-xs text-slate-600">{c.uid}</td>
                  <td className="px-5 py-3 text-slate-600">{c.roomLabel}</td>
                  <td className="px-5 py-3 text-slate-600">{c.accessCount} lượt</td>
                  <td className="px-5 py-3">
                    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${meta.bg} ${meta.color}`}>
                      {c.status === "BLOCKED" ? <ShieldX size={13} /> : <ShieldCheck size={13} />} {meta.label}
                    </span>
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2 flex-wrap">
                      {c.status === "BLOCKED" ? (
                        <button onClick={() => activate(c.id)} className="text-xs text-emerald-600 font-medium flex items-center gap-1">
                          <Check size={13} /> Kích hoạt lại
                        </button>
                      ) : (
                        <button onClick={() => revoke(c.id)} className="text-xs text-rose-500 font-medium flex items-center gap-1">
                          <Ban size={13} /> Thu hồi
                        </button>
                      )}
                      <button onClick={() => setEditingCard(c)} className="text-xs text-slate-500 font-medium flex items-center gap-1">
                        <Pencil size={12} /> Sửa
                      </button>
                      <button onClick={() => remove(c.id)} className="text-xs text-slate-400 hover:text-rose-500 font-medium flex items-center gap-1">
                        <Trash2 size={13} /> Xoá
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
            {cards.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-16 text-center text-slate-400">Chưa có thẻ nào</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {editingCard && (
        <EditCardModal card={editingCard} onClose={() => setEditingCard(null)} onSave={saveEdit} />
      )}
    </div>
  );
}

function EditCardModal({ card, onClose, onSave }) {
  const [ownerId, setOwnerId] = useState("");
  const [expiresAt, setExpiresAt] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    await onSave({ ownerId, expiresAt });
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold">Sửa thẻ {card.uid}</h3>
          <button type="button" onClick={onClose} className="text-slate-400"><X size={18} /></button>
        </div>
        <div>
          <label className="text-xs text-slate-400 mb-1 block">Chuyển cho user ID khác (để trống nếu giữ nguyên chủ thẻ)</label>
          <input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} placeholder={`Hiện tại: ${card.ownerName}`}
            className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
        </div>
        <div>
          <label className="text-xs text-slate-400 mb-1 block">Hạn dùng (để trống = vô thời hạn)</label>
          <input type="date" value={expiresAt} onChange={(e) => setExpiresAt(e.target.value)}
            className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
        </div>
        <button type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium">Lưu</button>
      </form>
    </div>
  );
}
