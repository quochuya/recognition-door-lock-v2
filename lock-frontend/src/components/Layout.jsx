import React, { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  Home, History, Users, CreditCard, Cpu, BarChart3, ChevronLeft,
  Menu, Bell, ChevronDown, Shield, LogOut, Building2, QrCode,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

const NAV_GROUPS = [
  {
    label: "Giám sát",
    items: [
      { to: "/", label: "Tổng quan", icon: Home, end: true },
      { to: "/history", label: "Lịch sử ra vào", icon: History },
    ],
  },
  {
    label: "Quản lý truy cập",
    items: [
      { to: "/users", label: "Người dùng", icon: Users, roles: ["ADMIN"] },
      { to: "/cards", label: "Thẻ RFID", icon: CreditCard, roles: ["ADMIN", "STAFF"] },
      { to: "/qr", label: "Vé QR", icon: QrCode, roles: ["ADMIN"] },
      { to: "/devices", label: "Thiết bị", icon: Cpu, roles: ["ADMIN", "STAFF"] },
    ],
  },
  {
    label: "Hệ thống",
    items: [
      { to: "/buildings", label: "Tòa nhà / Tầng / Phòng", icon: Building2, roles: ["ADMIN"] },
      { to: "/reports", label: "Báo cáo", icon: BarChart3, roles: ["ADMIN", "STAFF"] },
    ],
  },
];

export default function Layout({ children }) {
  const [collapsed, setCollapsed] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const doLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="w-full min-h-screen bg-slate-100 flex text-slate-800">
      <aside className={`${collapsed ? "w-[76px]" : "w-64"} shrink-0 bg-[#0b1a3a] text-slate-200 flex flex-col transition-all duration-200`}>
        <div className={`flex items-center h-16 border-b border-white/10 ${collapsed ? "justify-center px-0" : "gap-3 px-5"}`}>
          <div className="w-9 h-9 rounded-lg bg-blue-500 flex items-center justify-center shrink-0">
            <Shield size={18} className="text-white" />
          </div>
          {!collapsed && (
            <div className="leading-tight">
              <div className="text-white font-semibold text-sm">Smart Lock</div>
              <div className="text-white font-semibold text-sm -mt-0.5">Management</div>
            </div>
          )}
        </div>

        <nav className="flex-1 py-3 px-3 space-y-5 overflow-y-auto">
          {NAV_GROUPS.map((group) => {
            const visibleItems = group.items.filter((item) => !item.roles || hasRole(...item.roles));
            if (visibleItems.length === 0) return null;
            return (
              <div key={group.label}>
                {!collapsed ? (
                  <div className="px-3 pb-1.5 text-[11px] tracking-wide text-slate-400 font-semibold">
                    {group.label.toUpperCase()}
                  </div>
                ) : (
                  <div className="mx-3 mb-2 border-t border-white/10" />
                )}
                <div className="space-y-1">
                  {visibleItems.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        end={item.end}
                        className={({ isActive }) =>
                          `group relative w-full flex items-center py-2.5 rounded-lg text-sm transition-colors ${
                            collapsed ? "justify-center px-0" : "gap-3 px-3"
                          } ${isActive ? "bg-blue-600 text-white" : "text-slate-300 hover:bg-white/5"}`
                        }
                      >
                        <Icon size={18} className="shrink-0" />
                        {!collapsed && <span>{item.label}</span>}
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </nav>

        <button
          onClick={() => setCollapsed((c) => !c)}
          className={`flex items-center py-4 text-sm text-slate-300 hover:text-white border-t border-white/10 ${
            collapsed ? "justify-center px-0" : "gap-2 px-5"
          }`}
        >
          <ChevronLeft size={16} className={`transition-transform ${collapsed ? "rotate-180" : ""}`} />
          {!collapsed && "Thu gọn"}
        </button>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 shrink-0">
          <button className="text-slate-500 hover:text-slate-700">
            <Menu size={20} />
          </button>
          <div className="flex items-center gap-4">
            <button className="relative text-slate-500 hover:text-slate-700">
              <Bell size={20} />
            </button>
            <div className="relative">
              <button onClick={() => setMenuOpen((o) => !o)} className="flex items-center gap-2 pl-3 border-l border-slate-200">
                <div className="w-9 h-9 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center font-semibold text-sm">
                  {user?.fullName?.charAt(0) || "?"}
                </div>
                <div className="leading-tight text-left">
                  <div className="text-sm font-medium text-slate-800">{user?.fullName}</div>
                  <div className="text-xs text-slate-400">{user?.role}</div>
                </div>
                <ChevronDown size={16} className="text-slate-400" />
              </button>
              {menuOpen && (
                <div className="absolute right-0 top-14 w-48 bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden z-30">
                  <button
                    onClick={doLogout}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-rose-500 hover:bg-slate-50"
                  >
                    <LogOut size={16} /> Đăng xuất
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
