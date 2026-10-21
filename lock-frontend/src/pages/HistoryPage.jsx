import React, { useEffect, useState, useCallback } from "react";
import { CheckCircle2, XCircle, ChevronLeft, ChevronRight } from "lucide-react";
import api from "../api/client";
import { connectSocket, disconnectSocket } from "../api/socket";

export default function HistoryPage() {
  const [logs, setLogs] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("");
  const [search, setSearch] = useState("");

  const load = useCallback(() => {
    api.get("/api/access-logs", {
      params: {
        page,
        size: 8,
        success: statusFilter === "" ? undefined : statusFilter === "success",
        search: search || undefined,
      },
    }).then(({ data }) => {
      setLogs(data.content);
      setTotalPages(data.totalPages || 1);
    });
  }, [page, statusFilter, search]);

  useEffect(load, [load]);

  useEffect(() => {
    connectSocket({
      onAccessLog: () => load(), // có sự kiện mới -> tải lại trang hiện tại
    });
    return () => disconnectSocket();
  }, [load]);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-4 flex flex-wrap items-center gap-3">
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white outline-none"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="success">Thành công</option>
          <option value="denied">Bị từ chối</option>
        </select>
        <input
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="Tìm theo tên, số phòng..."
          className="flex-1 min-w-[200px] text-sm border border-slate-200 rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
              <th className="text-left font-semibold px-5 py-3">Người dùng</th>
              <th className="text-left font-semibold px-5 py-3">Vị trí</th>
              <th className="text-left font-semibold px-5 py-3">Phương thức</th>
              <th className="text-left font-semibold px-5 py-3">Thời gian</th>
              <th className="text-left font-semibold px-5 py-3">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-5 py-3 font-medium text-slate-700">{log.userName}</td>
                <td className="px-5 py-3 text-slate-600">{log.buildingName} · Phòng {log.roomNumber}</td>
                <td className="px-5 py-3 text-slate-600">{log.method}</td>
                <td className="px-5 py-3 text-slate-500">{new Date(log.occurredAt).toLocaleString("vi-VN")}</td>
                <td className="px-5 py-3">
                  {log.success ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium">
                      <CheckCircle2 size={13} /> Thành công
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-50 text-rose-600 text-xs font-medium" title={log.denyReason}>
                      <XCircle size={13} /> Bị từ chối
                    </span>
                  )}
                </td>
              </tr>
            ))}
            {logs.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-16 text-center text-slate-400">Không có dữ liệu</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-center gap-2">
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="w-8 h-8 rounded-lg border border-slate-200 bg-white text-slate-400 flex items-center justify-center disabled:opacity-40">
          <ChevronLeft size={16} />
        </button>
        <span className="text-sm text-slate-500">Trang {page + 1} / {totalPages}</span>
        <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="w-8 h-8 rounded-lg border border-slate-200 bg-white text-slate-400 flex items-center justify-center disabled:opacity-40">
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}
