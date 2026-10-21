import React, { useEffect, useState } from "react";
import { QrCode, Plus, X, Clock } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import api from "../api/client";

export default function QrPage() {
  const [users, setUsers] = useState([]);
  const [buildings, setBuildings] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [generated, setGenerated] = useState(null); // QrTokenDto vừa sinh, để hiển thị mã QR

  useEffect(() => {
    api.get("/api/users").then(({ data }) => setUsers(data));
    api.get("/api/buildings").then(({ data }) => setBuildings(data));
  }, []);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <QrCode size={18} className="text-blue-600" />
          <div>
            <div className="text-sm font-medium text-slate-800">Vé QR tạm thời</div>
            <div className="text-xs text-slate-400">Dùng cho khách vãng lai, người giao hàng — 1 lần, có hạn dùng ngắn</div>
          </div>
        </div>
        <button
          onClick={() => { setGenerated(null); setShowForm(true); }}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium"
        >
          <Plus size={16} /> Cấp vé QR
        </button>
      </div>

      {!generated && !showForm && (
        <div className="bg-white rounded-xl border border-slate-200 p-16 text-center text-slate-400">
          Bấm "Cấp vé QR" để tạo mã cho khách quét mở cửa
        </div>
      )}

      {generated && (
        <div className="bg-white rounded-xl border border-slate-200 p-8 flex flex-col items-center gap-4">
          <QRCodeSVG value={generated.token} size={220} />
          <div className="text-center">
            <div className="font-medium text-slate-800">{generated.userName}</div>
            <div className="text-sm text-slate-500">Phòng {generated.roomNumber}</div>
            <div className="flex items-center justify-center gap-1.5 text-xs text-amber-600 mt-2">
              <Clock size={13} />
              Hết hạn lúc {new Date(generated.expiresAt).toLocaleString("vi-VN")}
            </div>
          </div>
          <div className="text-xs text-slate-400 max-w-xs text-center">
            Đưa mã này cho khách để họ quét trước camera ESP32-CAM tại cửa — chỉ dùng được 1 lần.
          </div>
          <button
            onClick={() => { setGenerated(null); setShowForm(true); }}
            className="text-sm text-blue-600 font-medium"
          >
            Cấp vé khác
          </button>
        </div>
      )}

      {showForm && (
        <GenerateQrModal
          users={users}
          buildings={buildings}
          onClose={() => setShowForm(false)}
          onGenerated={(dto) => { setGenerated(dto); setShowForm(false); }}
        />
      )}
    </div>
  );
}

function GenerateQrModal({ users, buildings, onClose, onGenerated }) {
  const [userId, setUserId] = useState("");
  const [buildingId, setBuildingId] = useState("");
  const [floors, setFloors] = useState([]);
  const [floorId, setFloorId] = useState("");
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState("");
  const [validityMinutes, setValidityMinutes] = useState(5);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (buildingId) api.get(`/api/buildings/${buildingId}/floors`).then(({ data }) => setFloors(data));
  }, [buildingId]);
  useEffect(() => {
    if (floorId) api.get(`/api/floors/${floorId}/rooms`).then(({ data }) => setRooms(data));
  }, [floorId]);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const { data } = await api.post("/api/qr-tokens", {
        userId: Number(userId),
        roomId: Number(roomId),
        validityMinutes: Number(validityMinutes),
      });
      onGenerated(data);
    } catch (err) {
      setError(err.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold">Cấp vé QR mới</h3>
          <button type="button" onClick={onClose} className="text-slate-400"><X size={18} /></button>
        </div>

        <div>
          <label className="text-xs text-slate-400 mb-1 block">Cấp cho người dùng</label>
          <select required value={userId} onChange={(e) => setUserId(e.target.value)} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none">
            <option value="">-- Chọn người dùng --</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>)}
          </select>
        </div>

        <div>
          <label className="text-xs text-slate-400 mb-1 block">Tòa nhà</label>
          <select required value={buildingId} onChange={(e) => setBuildingId(e.target.value)} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none">
            <option value="">-- Chọn tòa --</option>
            {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs text-slate-400 mb-1 block">Tầng</label>
          <select required value={floorId} onChange={(e) => setFloorId(e.target.value)} disabled={!buildingId} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none disabled:bg-slate-50">
            <option value="">-- Chọn tầng --</option>
            {floors.map((f) => <option key={f.id} value={f.id}>Tầng {f.level}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs text-slate-400 mb-1 block">Phòng</label>
          <select required value={roomId} onChange={(e) => setRoomId(e.target.value)} disabled={!floorId} className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none disabled:bg-slate-50">
            <option value="">-- Chọn phòng --</option>
            {rooms.map((r) => <option key={r.roomId} value={r.roomId}>Phòng {r.roomNumber}</option>)}
          </select>
        </div>

        <div>
          <label className="text-xs text-slate-400 mb-1 block">Thời hạn (phút, tối đa 60)</label>
          <input required type="number" min="1" max="60" value={validityMinutes}
            onChange={(e) => setValidityMinutes(e.target.value)}
            className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none" />
        </div>

        {error && <div className="text-sm text-rose-600 bg-rose-50 rounded-lg px-3 py-2">{error}</div>}

        <button disabled={saving} type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium disabled:opacity-60">
          Sinh mã QR
        </button>
      </form>
    </div>
  );
}
