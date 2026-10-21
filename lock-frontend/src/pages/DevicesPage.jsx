import React, { useEffect, useState } from "react";
import { Cpu, Wifi, WifiOff, Battery, RefreshCw, Trash2, Plus, Pencil, X } from "lucide-react";
import api from "../api/client";

export default function DevicesPage() {
  const [devices, setDevices] = useState([]);
  const [status, setStatus] = useState("");
  const [modal, setModal] = useState(null); // {mode:'create'|'edit', device}

  const load = () => api.get("/api/devices", { params: { status: status || undefined } }).then(({ data }) => setDevices(data));

  useEffect(() => { load(); }, [status]);

  const restart = async (id) => { await api.post(`/api/devices/${id}/restart`); };
  const remove = async (id) => {
    if (!confirm("Gỡ thiết bị này khỏi hệ thống?")) return;
    await api.delete(`/api/devices/${id}`);
    load();
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-4 flex items-center justify-between">
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white outline-none">
          <option value="">Tất cả trạng thái</option>
          <option value="ONLINE">Online</option>
          <option value="OFFLINE">Offline</option>
          <option value="LOW_BATTERY">Pin yếu</option>
        </select>
        <button onClick={() => setModal({ mode: "create" })} className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium">
          <Plus size={16} /> Thêm thiết bị
        </button>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
              <th className="text-left font-semibold px-5 py-3">Thiết bị</th>
              <th className="text-left font-semibold px-5 py-3">Vị trí</th>
              <th className="text-left font-semibold px-5 py-3">Pin</th>
              <th className="text-left font-semibold px-5 py-3">Firmware</th>
              <th className="text-left font-semibold px-5 py-3">Trạng thái</th>
              <th className="text-left font-semibold px-5 py-3">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {devices.map((d) => (
              <tr key={d.id} className="border-t border-slate-100">
                <td className="px-5 py-3">
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${d.connectionStatus === "ONLINE" ? "bg-blue-50" : "bg-slate-100"}`}>
                      <Cpu size={16} className={d.connectionStatus === "ONLINE" ? "text-blue-600" : "text-slate-400"} />
                    </div>
                    <div className="font-medium text-slate-700">{d.deviceCode}</div>
                  </div>
                </td>
                <td className="px-5 py-3 text-slate-600">{d.buildingName} · Tầng {d.floorLevel} · Phòng {d.roomNumber}</td>
                <td className="px-5 py-3 text-slate-600 flex items-center gap-1.5"><Battery size={14} /> {d.batteryPercent}%</td>
                <td className="px-5 py-3 text-slate-600 font-mono text-xs">{d.firmwareVersion || "—"}</td>
                <td className="px-5 py-3">
                  {d.connectionStatus === "ONLINE" ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium"><Wifi size={13} /> Online</span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-medium"><WifiOff size={13} /> Offline</span>
                  )}
                </td>
                <td className="px-5 py-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <button onClick={() => setModal({ mode: "edit", device: d })} className="text-xs text-slate-500 font-medium flex items-center gap-1"><Pencil size={12} /> Đổi phòng</button>
                    <button onClick={() => restart(d.id)} className="text-xs text-blue-600 font-medium flex items-center gap-1"><RefreshCw size={13} /> Khởi động lại</button>
                    <button onClick={() => remove(d.id)} className="text-xs text-rose-500 font-medium flex items-center gap-1"><Trash2 size={13} /> Gỡ</button>
                  </div>
                </td>
              </tr>
            ))}
            {devices.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-16 text-center text-slate-400">Chưa có thiết bị nào</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <DeviceFormModal
          modal={modal}
          onClose={() => setModal(null)}
          onSaved={() => { setModal(null); load(); }}
        />
      )}
    </div>
  );
}

function DeviceFormModal({ modal, onClose, onSaved }) {
  const [deviceCode, setDeviceCode] = useState(modal.device?.deviceCode || "");
  const [buildings, setBuildings] = useState([]);
  const [buildingId, setBuildingId] = useState("");
  const [floors, setFloors] = useState([]);
  const [floorId, setFloorId] = useState("");
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState("");
  const [error, setError] = useState("");

  useEffect(() => { api.get("/api/buildings").then(({ data }) => setBuildings(data)); }, []);
  useEffect(() => {
    if (buildingId) api.get(`/api/buildings/${buildingId}/floors`).then(({ data }) => setFloors(data));
  }, [buildingId]);
  useEffect(() => {
    if (floorId) api.get(`/api/floors/${floorId}/rooms`).then(({ data }) => setRooms(data));
  }, [floorId]);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      if (modal.mode === "create") {
        await api.post("/api/devices", { deviceCode, roomId: Number(roomId) });
      } else {
        await api.put(`/api/devices/${modal.device.id}`, { roomId: Number(roomId) });
      }
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || "Có lỗi xảy ra");
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold">{modal.mode === "create" ? "Thêm thiết bị" : "Đổi phòng gắn thiết bị"}</h3>
          <button type="button" onClick={onClose} className="text-slate-400"><X size={18} /></button>
        </div>

        {modal.mode === "create" && (
          <div>
            <label className="text-xs text-slate-400 mb-1 block">Mã thiết bị (deviceCode — phải khớp firmware ESP32)</label>
            <input required value={deviceCode} onChange={(e) => setDeviceCode(e.target.value)} placeholder="ESP32-101"
              className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none font-mono" />
          </div>
        )}

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
            {rooms.map((r) => <option key={r.roomId} value={r.roomId}>Phòng {r.roomNumber}{r.deviceCode ? ` (đã có ${r.deviceCode})` : ""}</option>)}
          </select>
        </div>

        {error && <div className="text-sm text-rose-600 bg-rose-50 rounded-lg px-3 py-2">{error}</div>}

        <button type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium">Lưu</button>
      </form>
    </div>
  );
}
