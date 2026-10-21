# Module 1: Khóa & Thẻ (rule engine core)

Build từ đầu theo thống nhất: **Maven + Java 17 + Spring Boot 3.3 + PostgreSQL + MQTT**,
topic theo device (`lock/{deviceCode}/...`), tên class theo pattern `AccessEngine` / `AccessRule` / `LockController`.

## Chạy thử

1. Tạo DB: `CREATE DATABASE smart_lock;`
2. Sửa `application.properties` nếu user/pass Postgres hoặc broker MQTT khác localhost
3. `mvn spring-boot:run`
4. Bảng tự tạo nhờ `ddl-auto=update` (chuyển sang Flyway khi ổn định schema)

## Kiến trúc rule engine

```
AccessRule (interface)
   ├── RateLimitRule       order=1   chống dò UID
   ├── DeviceOnlineRule    order=5   chỉ áp dụng cho lệnh remote
   ├── CardStatusRule      order=10  thẻ còn hiệu lực?
   ├── RoomPermissionRule  order=20  có quyền vào phòng?
   └── TimeWindowRule      order=30  đúng khung giờ? (cấu hình qua DB)

AccessEngine -> tự nhặt mọi bean AccessRule, sắp theo priority, dừng ở DENY/ALLOW đầu tiên
AccessService -> build context, gọi engine, ghi AccessLog + bắn WebSocket, điều khiển khoá vật lý qua MQTT
```

## Luồng quẹt thẻ

```
ESP32 quẹt thẻ
  -> publish lock/{deviceCode}/access-attempt {"cardUid":"..."}
  -> DeviceMessageListener (inbound)
  -> AccessService.handleCardScan()
  -> AccessEngine.run() (chạy rule chain)
  -> ALLOW: cập nhật lockState=UNLOCKED, publish lock/{deviceCode}/command {"command":"UNLOCK"}
  -> DENY:  publish lock/{deviceCode}/result {"allow":false,"reason":"..."}
  -> cả 2 trường hợp: lưu AccessLog + bắn /topic/access-logs cho dashboard
```

## Luồng admin bấm "Mở cửa từ xa" / "Khoá cửa" trên dashboard

```
POST /api/locks/{id}/unlock  hoặc  /lock
  -> AccessService.handleRemoteControl() -- CÙNG engine, adminOverride=true
  -> DeviceOnlineRule vẫn chặn nếu thiết bị offline
  -> RoomPermissionRule/TimeWindowRule bỏ qua vì admin luôn có quyền
```

## Đăng ký thẻ thủ công (xem thêm mục "Module RFID đầy đủ" bên dưới cho enrollment)

```
POST /api/cards  { "uid": "A1B2C3D4", "userId": 1, "expiresAt": null }
```
Đã kiểm tra `userId` tồn tại trước khi insert để tránh lỗi Foreign Key Constraint.

## Thêm rule mới — chỉ 3 bước

```java
@Component
public class YourNewRule implements AccessRule {
    public String getKey() { return "YOUR_KEY"; }
    public int getDefaultOrder() { return 40; }
    public RuleDecision evaluate(AccessContext ctx) { ... }
}
```
Không đụng `AccessEngine` hay rule khác. Muốn admin bật/tắt từ dashboard —
thêm 1 dòng trong bảng `access_rule_configs` với `ruleKey` trùng `getKey()`.

## Module RFID đầy đủ (mới thêm)

**Danh sách / chi tiết thẻ**
```
GET /api/cards            -> danh sách CardDto (chủ thẻ, vai trò, phòng được cấp quyền, số lượt dùng...)
GET /api/cards/{id}       -> chi tiết 1 thẻ
```

**Đăng ký thủ công**
```
POST /api/cards   { "uid": "A1B2C3D4", "userId": 1, "expiresAt": null }
```

**One-touch enrollment** — không cần gõ tay UID:
```
POST /api/cards/enrollment/start          # admin bật chế độ trên dashboard
                                           # đưa thẻ mới lại gần đầu đọc bất kỳ
GET  /api/cards/enrollment/status
GET  /api/cards/pending                   # danh sách UID đang chờ gán
POST /api/cards/pending/{id}/assign  { "userId": 1, "expiresAt": null }
POST /api/cards/pending/{id}/discard      # bỏ qua lượt quẹt này
POST /api/cards/enrollment/stop
```
Dashboard nên subscribe `/topic/card-enrollment` để thấy UID mới xuất hiện
ngay khi có người quẹt thẻ lạ, không cần bấm refresh `/api/cards/pending`.

**Thu hồi thẻ**
```
POST /api/cards/{id}/revoke     # mất thẻ / nghỉ việc — chuyển status = BLOCKED
POST /api/cards/{id}/activate   # kích hoạt lại nếu thu hồi nhầm
```

**Cơ chế chặn ở tầng MQTT:** `DeviceMessageListener` kiểm tra UID trước khi
đưa vào `AccessEngine` — nếu UID lạ **và** đang bật enrollment, request được
"rẽ nhánh" sang `CardEnrollmentService` thay vì chạy qua rule chain, nên
không bị tính nhầm thành 1 lượt truy cập bị từ chối trong `AccessLog`.

## Module Báo cáo & Thống kê (mới thêm)

6 endpoint, đều nhận `from`, `to` (yyyy-MM-dd, mặc định 7 ngày gần nhất) và
`buildingId` (optional, mặc định tất cả tòa):

```
GET /api/reports/summary               -> 4 thẻ KPI: tổng mở cửa, tỷ lệ thành công, bị từ chối, thiết bị pin yếu
GET /api/reports/activity-trend        -> [{date, successCount, deniedCount}] theo ngày, cho biểu đồ vùng
GET /api/reports/top-rooms?limit=5     -> top phòng hoạt động nhiều nhất
GET /api/reports/building-comparison   -> so sánh lượt hoạt động + số thiết bị offline giữa các tòa
GET /api/reports/device-health         -> phân bố pin (tốt/trung bình/yếu) + số thiết bị online/offline
GET /api/reports/security-events       -> đếm số lượt bị DENY theo từng rule (RATE_LIMIT, ROOM_PERMISSION...)
```

`security-events` lấy trực tiếp từ `AccessLog.denyReason` (chính là `ruleKey`
của rule đã chặn) — nghĩa là **không cần bảng riêng**, mọi rule mới thêm vào
rule engine sau này tự động xuất hiện trong báo cáo này, chỉ cần thêm dòng map
label dễ đọc trong `ReportService.RULE_LABELS` (không bắt buộc, thiếu thì hiện
luôn `ruleKey`).

## Module Đăng nhập / Spring Security + JWT (mới thêm)

**Lần đầu chạy** — app tự tạo tài khoản admin mặc định (xem log khi khởi động):
```
Email:    admin@smartlock.local
Password: Admin@123
```
Đổi mật khẩu này ngay khi có endpoint đổi mật khẩu ở module Người dùng.

**Login:**
```
POST /api/auth/login   { "email": "admin@smartlock.local", "password": "Admin@123" }
-> { "token": "...", "userId": 1, "fullName": "Administrator", "email": "...", "role": "ADMIN" }
```

**Gọi API khác** — đính kèm header:
```
Authorization: Bearer <token>
```

`GET /api/auth/me` — lấy lại thông tin user hiện tại từ token (dùng để hiển thị
tên/avatar admin trên dashboard sau khi F5).

**Những gì đã đổi so với trước:**
- `LockController.currentAdmin()` (tạm bợ, lấy admin đầu tiên trong DB) đã bị
  xoá — giờ lấy đúng user đang đăng nhập từ `Authentication` do `JwtAuthFilter` set
- `CorsConfig` đổi từ `WebMvcConfigurer` sang bean `CorsConfigurationSource`
  (bắt buộc khi có Spring Security, nếu không preflight `OPTIONS` sẽ bị chặn)
- Mọi endpoint `/api/**` (trừ `/api/auth/**`) giờ **bắt buộc có JWT hợp lệ**,
  `/ws/**` vẫn công khai để dashboard connect WebSocket được

**Phân quyền theo role** (chưa làm chi tiết): hiện tại chỉ yêu cầu "đã đăng
nhập", chưa giới hạn ADMIN mới được cấp thẻ/điều khiển khoá. Muốn siết, thêm
`@PreAuthorize("hasRole('ADMIN')")` lên method tương ứng trong `CardController`/`LockController`.

## Module Lịch sử ra vào (mới thêm)

```
GET /api/access-logs
  ?from=2025-07-06&to=2025-07-12   # optional, mặc định 7 ngày gần nhất
  &buildingId=1                     # optional
  &method=RFID                      # optional: RFID | PIN | APP | FACE
  &success=false                    # optional: true = chỉ thành công, false = chỉ bị từ chối
  &search=an                        # optional: tìm theo tên người dùng hoặc số phòng
  &page=0&size=8                    # phân trang chuẩn Spring
```

Trả về `Page<AccessLogDto>` — đã gồm sẵn `content`, `totalElements`, `totalPages`
nên UI phân trang có thể lấy trực tiếp không cần tính tay. Mỗi dòng đã "làm
phẳng" (userName, buildingName, roomNumber...) để tránh vấn đề serialize
entity có quan hệ lazy — không cần gọi thêm API nào khác để hiển thị bảng lịch sử.

Kết hợp với WebSocket `/topic/access-logs` (đã có từ module core) là đủ cho
toàn bộ trang History: load lần đầu bằng REST có filter/phân trang, sau đó
chèn thêm dòng mới real-time khi có sự kiện.

## Module Thiết bị (mới thêm)

```
GET  /api/devices?buildingId=&search=&status=ONLINE|OFFLINE|LOW_BATTERY   -> danh sách
GET  /api/devices/{id}                                                     -> chi tiết + 5 log gần nhất
POST /api/devices        { "deviceCode": "ESP32-1102", "roomId": 5 }       -> gắn thiết bị mới vào 1 phòng
POST /api/devices/{id}/restart                                             -> gửi lệnh RESTART qua MQTT
DELETE /api/devices/{id}                                                   -> gỡ thiết bị
```

`status=LOW_BATTERY` lọc pin < 25%, xử lý ở tầng service (không phải trường
DB trực tiếp) — tránh phải thêm cột tính toán hoặc trigger.

**Heartbeat MQTT giờ nhận thêm `signal`:**
```json
{ "battery": 85, "signal": 72, "firmware": "v2.4.1" }
```
`signal` là optional — thiết bị nào chưa gửi vẫn hoạt động bình thường, chỉ
không hiển thị cường độ Wi-Fi trên dashboard.

**Lệnh RESTART** dùng chung topic `lock/{deviceCode}/command` như LOCK/UNLOCK,
chỉ khác payload `{"command":"RESTART"}` — phía ESP32 cần thêm 1 nhánh xử lý
lệnh này (gợi ý: gọi `ESP.restart()`).

## Module Người dùng (mới thêm — hoàn tất toàn bộ backend)

```
GET  /api/users?search=an              -> danh sách (tìm theo tên/email)
GET  /api/users/{id}                    -> chi tiết + danh sách phòng được cấp quyền
POST /api/users   { fullName, email, password, phone, role }
PUT  /api/users/{id}   { fullName, phone, role, active }
POST /api/users/{id}/change-password    { newPassword }
POST /api/users/{id}/deactivate         -> vô hiệu hoá (không xoá cứng)
POST /api/users/{id}/reactivate

POST   /api/users/{id}/permissions              { roomId, expiresAt }  -> cấp quyền vào 1 phòng
DELETE /api/users/{id}/permissions/{roomId}                             -> thu hồi quyền phòng đó
```

**Vì sao không xoá cứng user (`DELETE`)?** `User` đang được `RfidCard`,
`AccessLog`, `RoomPermission` tham chiếu bằng khoá ngoại — xoá cứng sẽ vỡ dữ
liệu lịch sử hoặc phải cascade xoá luôn cả log. `deactivate()` set `active=false`,
`CustomUserDetailsService` sẽ từ chối đăng nhập ngay, nhưng thẻ/lịch sử cũ vẫn
nguyên vẹn để tra cứu.

**Cấp quyền phòng** chính là cách nạp dữ liệu cho `RoomPermissionRule` đã có
từ module core — cấp quyền ở đây xong, `AccessEngine` áp dụng ngay lập tức
lần quẹt thẻ tiếp theo, không cần khởi động lại app.

## Còn thiếu — làm ở các module sau

## Phân quyền theo role (@PreAuthorize) — mới thêm

Bật bằng `@EnableMethodSecurity` trong `SecurityConfig`. Bảng phân quyền:

| Endpoint | Ai được gọi |
|---|---|
| `POST /api/auth/login` | Công khai |
| `POST /api/auth/change-password` (tự đổi mật khẩu) | Bất kỳ ai đã đăng nhập |
| `GET /api/floors/{id}/rooms` | Bất kỳ ai đã đăng nhập |
| `POST /api/locks/{id}/lock`\|`unlock` | ADMIN, STAFF, SECURITY |
| `GET /api/cards`, `/api/devices` (xem) | ADMIN, STAFF |
| `POST/DELETE /api/cards/**`, `/api/devices/**` (thay đổi) | ADMIN |
| `GET/POST/PUT /api/users/**` (toàn bộ) | ADMIN |
| `GET /api/reports/**`, `/api/access-logs` | ADMIN, STAFF |

Gọi endpoint không đủ quyền sẽ nhận JSON `403` rõ ràng (`GlobalExceptionHandler`),
không còn trang lỗi HTML mặc định của Spring Boot.

**Đổi mật khẩu:** dùng `POST /api/auth/change-password` (yêu cầu `oldPassword`)
để tự đổi — **nên làm ngay** sau khi login bằng tài khoản admin mặc định.
Endpoint `POST /api/users/{id}/change-password` là ADMIN reset mật khẩu người
khác (không cần biết mật khẩu cũ), khác mục đích với endpoint trên.

## Checklist: thêm phương thức xác thực mới (vd khoá vân tay) — chuẩn bị sẵn cho ngày mai

Đã thêm `FINGERPRINT` vào `AccessMethod` enum. Còn 3 bước để hoàn thiện, làm
theo đúng thứ tự này khi bạn sẵn sàng:

1. **Nhận sự kiện từ thiết bị** — thêm 1 method trong `AccessService`, tương tự
   `handleCardScan()`, ví dụ `handleFingerprintScan(String deviceCode, String fingerprintId)`.
   Build `AccessContext` với `method = AccessMethod.FINGERPRINT`.

2. **Route từ MQTT** — trong `DeviceMessageListener.handle()`, thêm 1 nhánh
   `case "fingerprint-attempt" -> handleFingerprintAttempt(...)` song song với
   `access-attempt` hiện tại (hoặc dùng chung topic `access-attempt` với field
   `method` trong payload nếu ESP32 gửi được, tùy bạn quyết định quy ước).

3. **(Nếu cần) rule riêng cho vân tay** — vd kiểm tra độ khớp vân tay tối
   thiểu, tạo class mới trong `access/rule/impl/`:
   ```java
   @Component
   public class FingerprintMatchRule implements AccessRule {
       public String getKey() { return "FINGERPRINT_MATCH"; }
       public RuleDecision evaluate(AccessContext ctx) {
           if (ctx.getMethod() != AccessMethod.FINGERPRINT) return RuleDecision.SKIP;
           // logic kiểm tra riêng
       }
   }
   ```
   Không cần sửa `AccessEngine`, `CardStatusRule`, hay bất kỳ rule nào khác —
   `CardStatusRule` đã tự `SKIP` khi `method != RFID` nên không xung đột.

Entity `RfidCard` hiện chỉ dùng cho RFID — vân tay sẽ cần entity riêng (vd
`FingerprintTemplate` với `userId`, `templateData`) nếu lưu mẫu vân tay trong DB,
hoặc nếu module vân tay xử lý local trên ESP32 (chỉ gửi `matched: true/false`
lên server) thì không cần entity mới, chỉ cần bước 1-2 ở trên là đủ.

## Còn thiếu — không còn module riêng nào, chỉ còn việc "hoàn thiện thêm"

- Flyway/Liquibase thay cho `ddl-auto=update` trước khi lên production
- ESP32 firmware: cập nhật topic MQTT theo quy ước mới + xử lý lệnh `RESTART`
- Dashboard frontend nối vào toàn bộ API đã có (UI mockup đã làm sẵn từ trước,
  giờ có đủ backend thật cho mọi trang: Tổng quan, Lịch sử, RFID, Thiết bị,
  Báo cáo, Người dùng, Đăng nhập)
- Dashboard frontend nối vào các endpoint này (đã có sẵn thiết kế UI từ trước, giờ chỉ cần gọi API thật thay vì mock data + thêm màn hình Login)
- Dashboard frontend nối vào các endpoint này (đã có sẵn thiết kế UI từ trước, giờ chỉ cần gọi API thật thay vì mock data)

Frontend `dashboard.html` của bạn có thể kết nối ngay:
- REST: `GET /api/floors/{id}/rooms`, `POST /api/locks/{id}/lock|unlock`, `POST /api/cards`
- WebSocket: `new SockJS('/ws')` + STOMP subscribe `/topic/access-logs`
