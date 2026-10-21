import React, { useEffect, useState } from "react";
import { DoorOpen, TrendingUp, ShieldAlert, BatteryWarning } from "lucide-react";
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from "recharts";
import api from "../api/client";

export default function ReportsPage() {
  const [summary, setSummary] = useState(null);
  const [trend, setTrend] = useState([]);
  const [topRooms, setTopRooms] = useState([]);
  const [security, setSecurity] = useState([]);

  useEffect(() => {
    api.get("/api/reports/summary").then(({ data }) => setSummary(data));
    api.get("/api/reports/activity-trend").then(({ data }) => setTrend(data));
    api.get("/api/reports/top-rooms").then(({ data }) => setTopRooms(data));
    api.get("/api/reports/security-events").then(({ data }) => setSecurity(data));
  }, []);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-4 gap-4">
        <Kpi icon={<DoorOpen size={17} className="text-blue-600" />} bg="bg-blue-50" label="Tổng lượt mở cửa" value={summary?.totalUnlock ?? "—"} />
        <Kpi icon={<TrendingUp size={17} className="text-emerald-600" />} bg="bg-emerald-50" label="Tỷ lệ thành công" value={summary ? `${summary.successRate}%` : "—"} />
        <Kpi icon={<ShieldAlert size={17} className="text-rose-600" />} bg="bg-rose-50" label="Bị từ chối" value={summary?.deniedCount ?? "—"} />
        <Kpi icon={<BatteryWarning size={17} className="text-amber-600" />} bg="bg-amber-50" label="Thiết bị pin yếu" value={summary?.lowBatteryDevices ?? "—"} />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <div className="text-sm font-semibold text-slate-700 mb-4">Xu hướng hoạt động (7 ngày)</div>
        <ResponsiveContainer width="100%" height={260}>
          <AreaChart data={trend} margin={{ left: -20, right: 10 }}>
            <defs>
              <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#2563eb" stopOpacity={0.35} />
                <stop offset="100%" stopColor="#2563eb" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis dataKey="date" tick={{ fontSize: 12, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 12, fill: "#94a3b8" }} axisLine={false} tickLine={false} allowDecimals={false} />
            <Tooltip />
            <Area type="monotone" dataKey="successCount" name="Thành công" stroke="#2563eb" strokeWidth={2} fill="url(#g1)" />
            <Area type="monotone" dataKey="deniedCount" name="Bị từ chối" stroke="#fb7185" strokeWidth={2} fill="transparent" />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <div className="text-sm font-semibold text-slate-700 mb-4">Top phòng hoạt động</div>
          <div className="space-y-3">
            {topRooms.map((r) => (
              <div key={r.roomLabel} className="flex items-center justify-between text-sm">
                <span className="text-slate-600">{r.roomLabel}</span>
                <span className="font-medium text-slate-700">{r.count}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <div className="text-sm font-semibold text-slate-700 mb-4">Sự kiện an ninh</div>
          <div className="space-y-3">
            {security.map((e) => (
              <div key={e.ruleKey} className="flex items-center justify-between text-sm">
                <span className="text-slate-600">{e.label}</span>
                <span className="font-medium text-rose-600">{e.count}</span>
              </div>
            ))}
            {security.length === 0 && <div className="text-sm text-slate-400">Không có sự kiện nào</div>}
          </div>
        </div>
      </div>
    </div>
  );
}

function Kpi({ icon, bg, label, value }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4">
      <div className="flex items-center gap-2.5 mb-2">
        <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${bg}`}>{icon}</div>
        <div className="text-xs text-slate-400">{label}</div>
      </div>
      <div className="text-2xl font-semibold text-slate-800">{value}</div>
    </div>
  );
}
