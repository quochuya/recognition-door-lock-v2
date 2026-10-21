# Smart Lock Management — Frontend

React (Vite) + Tailwind, gọi thẳng API thật của `lock-backend`.

## Chạy thử

1. **Đảm bảo backend đang chạy** ở `http://localhost:8080` (`mvn spring-boot:run`
   trong project `lock-backend`) — trước đó phải có PostgreSQL và MQTT broker sẵn sàng.

2. Cài dependency:
   ```bash
   npm install
   ```

3. Chạy dev server:
   ```bash
   npm run dev
   ```
   Mở `http://localhost:5173`

4. Đăng nhập bằng tài khoản admin mặc định (backend tự tạo khi khởi động lần đầu):
   ```
   Email:    admin@smartlock.local
   Password: Admin@123
   ```
   Đổi mật khẩu ngay sau khi login (chưa có nút đổi mật khẩu trên UI — gọi tạm
   `POST /api/auth/change-password` bằng Postman, hoặc thêm màn hình sau).

## Cấu trúc

```
src/
├── api/
│   ├── client.js      # axios instance, tự gắn JWT vào mọi request
│   └── socket.js       # kết nối WebSocket (STOMP/SockJS) nhận real-time
├── context/
│   └── AuthContext.jsx # quản lý đăng nhập, lưu token vào localStorage
├── components/
│   └── Layout.jsx       # sidebar + header dùng chung mọi trang
└── pages/
    ├── LoginPage.jsx
    ├── OverviewPage.jsx  # chọn tòa/tầng, xem & điều khiển khóa
    ├── HistoryPage.jsx   # lịch sử ra vào, tự cập nhật real-time
    ├── CardsPage.jsx     # quản lý thẻ RFID + enrollment một chạm
    ├── DevicesPage.jsx   # quản lý thiết bị
    ├── ReportsPage.jsx   # báo cáo & thống kê
    └── UsersPage.jsx     # quản lý người dùng (ADMIN only)
```

## Đổi địa chỉ backend

Nếu backend không chạy ở `localhost:8080`, sửa `API_BASE` trong `src/api/client.js`.

## Còn thiếu / có thể làm thêm

- Trang chi tiết thiết bị/thẻ (hiện chỉ có danh sách + hành động nhanh)
- Toast thông báo đẹp hơn thay vì `alert()`/inline error text
- Trang cài đặt tài khoản cá nhân + đổi mật khẩu qua UI
- Build production (`npm run build`) rồi deploy static file thay vì chạy dev server
- Thêm phân trang/filter nâng cao ở trang Thiết bị, Thẻ RFID (giống Lịch sử ra vào)
