import React, { useEffect, useState } from "react";
import { Lock, Unlock, WifiOff, X, Battery } from "lucide-react";
import api from "../api/client";

const STATUS_STYLE = {
  LOCKED: { card: "bg-emerald-50 border-emerald-100", label: "Đã khóa", labelColor: "text-slate-700", iconBg: "bg-emerald-500" },
  UNLOCKED: { card: "bg-rose-50 border-rose-200 ring-1 ring-rose-200", label: "Đang mở", labelColor: "text-rose-600", iconBg: "bg-rose-500" },
};

export default function OverviewPage() {
  const [buildings, setBuildings] = useState([]);
  const [buildingId, setBuildingId] = useState(null);
  const [floors, setFloors] = useState([]);
  const [floorId, setFloorId] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/api/buildings").then(({ data }) => {
      setBuildings(data);
      if (data.length > 0) setBuildingId(data[0].id);
    });
  }, []);

  useEffect(() => {
    if (!buildingId) return;
    api.get(`/api/buildings/${buildingId}/floors`).then(({ data }) => {
      setFloors(data);
      if (data.length > 0) setFloorId(data[0].id);
    });
  }, [buildingId]);

  const loadRooms = () => {
    if (!floorId) return;
    setLoading(true);
    api.get(`/api/floors/${floorId}/rooms`)
      .then(({ data }) => setRooms(data))
      .catch(() => setError("Không tải được danh sách phòng"))
      .finally(() => setLoading(false));
  };

  useEffect(loadRooms, [floorId]);

  const toggleLock = async (room) => {
    if (!room.lockDeviceId) return;
    setActionLoading(true);
    setError("");
    try {
      const action = room.lockState === "LOCKED" ? "unlock" : "lock";
      const { data } = await api.post(`/api/locks/${room.lockDeviceId}/${action}`);
      if (!data.success) {
        setError(data.message);
      } else {
        loadRooms();
        setSelected((s) => (s ? { ...s, lockState: action === "unlock" ? "UNLOCKED" : "LOCKED" } : s));
      }
    } catch (e) {
      setError(e.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={buildingId || ""}
          onChange={(e) => setBuildingId(Number(e.target.value))}
          className="text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white outline-none"
        >
          {buildings.map((b) => (
            <option key={b.id} value={b.id}>{b.name}</option>
          ))}
        </select>
        <div className="flex bg-white border border-slate-200 rounded-lg p-1">
          {floors.map((f) => (
            <button
              key={f.id}
              onClick={() => setFloorId(f.id)}
              className={`px-4 py-1.5 text-sm rounded-md font-medium ${floorId === f.id ? "bg-blue-600 text-white" : "text-slate-500"}`}
            >
              Tầng {f.level}
            </button>
          ))}
        </div>
      </div>

      {error && <div className="text-sm text-rose-600 bg-rose-50 rounded-lg px-4 py-2">{error}</div>}

      {loading ? (
        <div className="text-center text-slate-400 py-16">Đang tải...</div>
      ) : rooms.length === 0 ? (
        <div className="text-center text-slate-400 py-16 bg-white rounded-xl border border-slate-200">
          Không có phòng nào ở tầng này
        </div>
      ) : (
        <div className="grid grid-cols-4 gap-4">
          {rooms.map((room) => {
            const s = STATUS_STYLE[room.lockState] || STATUS_STYLE.LOCKED;
            const offline = room.connectionStatus === "OFFLINE";
            return (
              <button
                key={room.roomId}
                onClick={() => setSelected(room)}
                className={`text-left rounded-xl border p-4 transition-all ${offline ? "bg-slate-100 border-slate-200" : s.card}`}
              >
                <div className="text-xl font-semibold text-slate-800">{room.roomNumber}</div>
                <div className="flex items-center gap-2 mt-2">
                  {offline ? (
                    <>
                      <span className="w-7 h-7 rounded-full flex items-center justify-center bg-slate-300">
                        <WifiOff size={14} className="text-white" />
                      </span>
                      <span className="text-sm font-medium text-slate-500">Offline</span>
                    </>
                  ) : (
                    <>
                      <span className={`w-7 h-7 rounded-full flex items-center justify-center ${s.iconBg}`}>
                        {room.lockState === "LOCKED" ? <Lock size={14} className="text-white" /> : <Unlock size={14} className="text-white" />}
                      </span>
                      <span className={`text-sm font-medium ${s.labelColor}`}>{s.label}</span>
                    </>
                  )}
                </div>
                {room.batteryPercent != null && (
                  <div className="flex items-center gap-1 text-xs text-slate-400 mt-3">
                    <Battery size={12} /> {room.batteryPercent}%
                  </div>
                )}
              </button>
            );
          })}
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={() => setSelected(null)}>
          <div onClick={(e) => e.stopPropagation()} className="w-full max-w-sm bg-white rounded-2xl shadow-2xl p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Phòng {selected.roomNumber}</h3>
              <button onClick={() => setSelected(null)} className="text-slate-400 hover:text-slate-600">
                <X size={18} />
              </button>
            </div>
            <div className="text-sm text-slate-500 mb-1">Thiết bị: {selected.deviceCode || "—"}</div>
            <div className="text-sm text-slate-500 mb-4">
              Trạng thái: {selected.connectionStatus === "OFFLINE" ? "Offline" : selected.lockState === "LOCKED" ? "Đã khóa" : "Đang mở"}
            </div>
            <button
              onClick={() => toggleLock(selected)}
              disabled={actionLoading || selected.connectionStatus === "OFFLINE"}
              className="w-full py-3 rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {selected.lockState === "LOCKED" ? "Mở khóa" : "Khóa cửa"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
