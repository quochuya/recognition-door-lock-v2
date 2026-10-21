import React, { useEffect, useState } from "react";
import { Building2, Layers, DoorOpen, Plus, Pencil, Trash2, X } from "lucide-react";
import api from "../api/client";

export default function BuildingsPage() {
  const [buildings, setBuildings] = useState([]);
  const [selectedBuilding, setSelectedBuilding] = useState(null);
  const [floors, setFloors] = useState([]);
  const [selectedFloor, setSelectedFloor] = useState(null);
  const [rooms, setRooms] = useState([]);

  const [modal, setModal] = useState(null); // {type: 'building'|'floor'|'room', mode:'create'|'edit', data}
  const [error, setError] = useState("");

  const loadBuildings = () => api.get("/api/buildings").then(({ data }) => setBuildings(data));
  useEffect(() => { loadBuildings(); }, []);

  const loadFloors = (buildingId) => {
    api.get(`/api/buildings/${buildingId}/floors`).then(({ data }) => setFloors(data));
    setRooms([]);
    setSelectedFloor(null);
  };

  useEffect(() => {
    if (selectedBuilding) loadFloors(selectedBuilding.id);
  }, [selectedBuilding]);

  const loadRooms = (floorId) => {
    api.get(`/api/floors/${floorId}/rooms`).then(({ data }) => setRooms(data));
  };

  useEffect(() => {
    if (selectedFloor) loadRooms(selectedFloor.id);
  }, [selectedFloor]);

  // --- Building CRUD ---
  const saveBuilding = async (form) => {
    if (modal.mode === "create") await api.post("/api/buildings", form);
    else await api.put(`/api/buildings/${modal.data.id}`, form);
    setModal(null);
    loadBuildings();
  };
  const deleteBuilding = async (b) => {
    if (!confirm(`Xoá tòa "${b.name}"? Phải xoá hết tầng bên trong trước.`)) return;
    try {
      await api.delete(`/api/buildings/${b.id}`);
      if (selectedBuilding?.id === b.id) setSelectedBuilding(null);
      loadBuildings();
    } catch (e) {
      setError(e.response?.data?.message || "Không xoá được");
    }
  };

  // --- Floor CRUD ---
  const saveFloor = async (form) => {
    if (modal.mode === "create") await api.post(`/api/buildings/${selectedBuilding.id}/floors`, form);
    else await api.put(`/api/floors/${modal.data.id}`, form);
    setModal(null);
    loadFloors(selectedBuilding.id);
  };
  const deleteFloor = async (f) => {
    if (!confirm(`Xoá tầng ${f.level}? Phải xoá hết phòng bên trong trước.`)) return;
    try {
      await api.delete(`/api/floors/${f.id}`);
      if (selectedFloor?.id === f.id) setSelectedFloor(null);
      loadFloors(selectedBuilding.id);
    } catch (e) {
      setError(e.response?.data?.message || "Không xoá được");
    }
  };

  // --- Room CRUD ---
  const saveRoom = async (form) => {
    if (modal.mode === "create") await api.post(`/api/floors/${selectedFloor.id}/rooms`, form);
    else await api.put(`/api/rooms/${modal.data.roomId}`, form);
    setModal(null);
    loadRooms(selectedFloor.id);
  };
  const deleteRoom = async (r) => {
    if (!confirm(`Xoá phòng ${r.roomNumber}? Phải gỡ thiết bị gắn trong phòng trước (nếu có).`)) return;
    try {
      await api.delete(`/api/rooms/${r.roomId}`);
      loadRooms(selectedFloor.id);
    } catch (e) {
      setError(e.response?.data?.message || "Không xoá được");
    }
  };

  return (
    <div className="space-y-6">
      {error && (
        <div className="text-sm text-rose-600 bg-rose-50 rounded-lg px-4 py-2 flex items-center justify-between">
          {error}
          <button onClick={() => setError("")}><X size={14} /></button>
        </div>
      )}

      <div className="grid grid-cols-3 gap-4">
        {/* Buildings column */}
        <Column
          icon={<Building2 size={16} />}
          title="Tòa nhà"
          onAdd={() => setModal({ type: "building", mode: "create" })}
        >
          {buildings.map((b) => (
            <Row
              key={b.id}
              active={selectedBuilding?.id === b.id}
              label={b.name}
              sub={b.address}
              onClick={() => setSelectedBuilding(b)}
              onEdit={() => setModal({ type: "building", mode: "edit", data: b })}
              onDelete={() => deleteBuilding(b)}
            />
          ))}
          {buildings.length === 0 && <Empty text="Chưa có tòa nhà nào" />}
        </Column>

        {/* Floors column */}
        <Column
          icon={<Layers size={16} />}
          title="Tầng"
          onAdd={selectedBuilding ? () => setModal({ type: "floor", mode: "create" }) : null}
          hint={!selectedBuilding ? "Chọn 1 tòa nhà trước" : null}
        >
          {floors.map((f) => (
            <Row
              key={f.id}
              active={selectedFloor?.id === f.id}
              label={`Tầng ${f.level}`}
              onClick={() => setSelectedFloor(f)}
              onEdit={() => setModal({ type: "floor", mode: "edit", data: f })}
              onDelete={() => deleteFloor(f)}
            />
          ))}
          {selectedBuilding && floors.length === 0 && <Empty text="Chưa có tầng nào" />}
        </Column>

        {/* Rooms column */}
        <Column
          icon={<DoorOpen size={16} />}
          title="Phòng"
          onAdd={selectedFloor ? () => setModal({ type: "room", mode: "create" }) : null}
          hint={!selectedFloor ? "Chọn 1 tầng trước" : null}
        >
          {rooms.map((r) => (
            <Row
              key={r.roomId}
              label={`Phòng ${r.roomNumber}`}
              sub={r.deviceCode ? `Thiết bị: ${r.deviceCode}` : "Chưa gắn thiết bị"}
              onEdit={() => setModal({ type: "room", mode: "edit", data: r })}
              onDelete={() => deleteRoom(r)}
            />
          ))}
          {selectedFloor && rooms.length === 0 && <Empty text="Chưa có phòng nào" />}
        </Column>
      </div>

      {modal?.type === "building" && (
        <FormModal
          title={modal.mode === "create" ? "Thêm tòa nhà" : "Sửa tòa nhà"}
          fields={[
            { key: "name", label: "Tên tòa nhà", required: true, value: modal.data?.name },
            { key: "address", label: "Địa chỉ", value: modal.data?.address },
          ]}
          onSubmit={saveBuilding}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === "floor" && (
        <FormModal
          title={modal.mode === "create" ? "Thêm tầng" : "Sửa tầng"}
          fields={[{ key: "level", label: "Số tầng", type: "number", required: true, value: modal.data?.level }]}
          onSubmit={saveFloor}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === "room" && (
        <FormModal
          title={modal.mode === "create" ? "Thêm phòng" : "Sửa phòng"}
          fields={[{ key: "roomNumber", label: "Số phòng", type: "number", required: true, value: modal.data?.roomNumber }]}
          onSubmit={saveRoom}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}

function Column({ icon, title, onAdd, hint, children }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">{icon} {title}</div>
        {onAdd && (
          <button onClick={onAdd} className="text-blue-600 hover:bg-blue-50 rounded-lg p-1.5">
            <Plus size={16} />
          </button>
        )}
      </div>
      {hint && <div className="text-xs text-slate-400 text-center py-6">{hint}</div>}
      <div className="space-y-1.5">{children}</div>
    </div>
  );
}

function Row({ active, label, sub, onClick, onEdit, onDelete }) {
  return (
    <div
      onClick={onClick}
      className={`flex items-center justify-between px-3 py-2 rounded-lg text-sm ${
        active ? "bg-blue-50 text-blue-700" : "hover:bg-slate-50"
      } ${onClick ? "cursor-pointer" : ""}`}
    >
      <div>
        <div className="font-medium">{label}</div>
        {sub && <div className="text-xs text-slate-400">{sub}</div>}
      </div>
      <div className="flex items-center gap-1 shrink-0">
        <button onClick={(e) => { e.stopPropagation(); onEdit(); }} className="text-slate-400 hover:text-blue-600 p-1">
          <Pencil size={14} />
        </button>
        <button onClick={(e) => { e.stopPropagation(); onDelete(); }} className="text-slate-400 hover:text-rose-500 p-1">
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  );
}

function Empty({ text }) {
  return <div className="text-xs text-slate-400 text-center py-6">{text}</div>;
}

function FormModal({ title, fields, onSubmit, onClose }) {
  const [form, setForm] = useState(() =>
    Object.fromEntries(fields.map((f) => [f.key, f.value ?? ""]))
  );
  const [saving, setSaving] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = { ...form };
      fields.forEach((f) => {
        if (f.type === "number") payload[f.key] = Number(payload[f.key]);
      });
      await onSubmit(payload);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <form onClick={(e) => e.stopPropagation()} onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold">{title}</h3>
          <button type="button" onClick={onClose} className="text-slate-400"><X size={18} /></button>
        </div>
        {fields.map((f) => (
          <div key={f.key}>
            <label className="text-xs text-slate-400 mb-1 block">{f.label}</label>
            <input
              type={f.type || "text"}
              required={f.required}
              value={form[f.key]}
              onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
              className="w-full px-3 py-2.5 text-sm rounded-lg border border-slate-200 outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}
        <button disabled={saving} type="submit" className="w-full py-2.5 rounded-lg bg-blue-600 text-white font-medium disabled:opacity-60">
          Lưu
        </button>
      </form>
    </div>
  );
}
