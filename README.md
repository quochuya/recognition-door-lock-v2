# Smart Lock Management System

Hệ thống quản lý khóa cửa thông minh sử dụng RFID, ESP32, MQTT và dashboard web. Dự án được tổ chức thành ba phần chính: firmware cho thiết bị cửa, backend xử lý nghiệp vụ và frontend quản trị.

## Mục lục

- [Tổng quan](#tổng-quan)
- [Chức năng](#chức-năng)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [Luồng hoạt động chính](#luồng-hoạt-động-chính)
- [Giao tiếp MQTT](#giao-tiếp-mqtt)
- [API và cập nhật thời gian thực](#api-và-cập-nhật-thời-gian-thực)
- [Cấu hình thiết bị ESP32](#cấu-hình-thiết-bị-esp32)
- [Bảo mật và triển khai thực tế](#bảo-mật-và-triển-khai-thực-tế)
- [Hạn chế cần lưu ý](#hạn-chế-cần-lưu-ý)

## Tổng quan

Smart Lock Management System là dự án khóa cửa thông minh hướng tới quản lý nhiều cửa/phòng trên dashboard tập trung. Thiết bị ESP32 đọc thẻ RFID bằng module RC522 và giao tiếp với backend thông qua MQTT. Backend đánh giá yêu cầu truy cập bằng chuỗi quy tắc (rule engine), lưu lịch sử, phát lệnh điều khiển và cung cấp REST API cho giao diện web.

Dashboard React hỗ trợ đăng nhập, theo dõi trạng thái, quản lý thẻ và thiết bị, xem lịch sử truy cập, quản lý người dùng và xem báo cáo. Frontend cũng có trang QR; quy trình QR thực tế phụ thuộc vào cấu hình và phần triển khai tương ứng trong backend/firmware.

## Chức năng

### Thiết bị cửa và firmware ESP32

- Đọc UID thẻ RFID thông qua RC522.
- Gửi sự kiện quẹt thẻ lên MQTT.
- Nhận lệnh điều khiển khóa từ backend và điều khiển relay.
- Tự khóa lại sau khoảng thời gian mở khóa được cấu hình trong firmware.
- Gửi heartbeat/trạng thái thiết bị.
- Tự kết nối lại Wi-Fi và MQTT khi mất kết nối.
- Lưu danh sách UID được phép vào bộ nhớ flash để hỗ trợ chế độ offline.
- Xếp hàng sự kiện khi mất kết nối và gửi lại khi kết nối phục hồi, có cơ chế chờ ACK.
- Hiển thị phản hồi bằng LED cho các trạng thái được firmware xử lý.

### Backend

- Xác thực đăng nhập bằng JWT.
- Quản lý người dùng, thẻ RFID, thiết bị, tòa nhà/phòng và quyền truy cập.
- Áp dụng rule engine để quyết định cho phép hoặc từ chối truy cập.
- Ghi nhật ký truy cập và lý do từ chối.
- Điều khiển khóa từ xa thông qua MQTT.
- Đăng ký thẻ theo UID hoặc quy trình enrollment một chạm.
- Thu hồi và kích hoạt lại thẻ.
- Quản lý token QR và quyền phòng theo các module tương ứng.
- Cung cấp dữ liệu báo cáo và thống kê.
- Phát sự kiện cập nhật lên WebSocket/STOMP để dashboard cập nhật gần thời gian thực.

### Dashboard web

- Đăng nhập và lưu trạng thái xác thực ở phía frontend.
- Tổng quan hệ thống, trạng thái khóa và điều khiển cửa.
- Quản lý tòa nhà/phòng.
- Quản lý thẻ RFID, đăng ký thẻ mới, thu hồi/kích hoạt thẻ.
- Theo dõi thiết bị.
- Tra cứu lịch sử ra vào.
- Xem báo cáo và thống kê.
- Quản lý người dùng.
- Trang giao diện QR.

## Kiến trúc hệ thống

```text
[RFID Card]
     |
     v
[RC522 + ESP32 + Relay/Lock]
     |
     | MQTT
     v
[MQTT Broker: EMQX hoặc broker tương thích]
     |
     v
[Spring Boot Backend]
     |                 \
     |                  \ WebSocket/STOMP
     v                   v
[PostgreSQL]        [React Dashboard]
```

Các thành phần:

- **Firmware ESP32:** giao tiếp với đầu đọc RFID, relay và broker MQTT; lưu cache whitelist và hàng đợi sự kiện offline.
- **MQTT broker:** trung chuyển thông điệp giữa thiết bị và backend.
- **Backend Spring Boot:** xác thực, kiểm tra quy tắc, lưu dữ liệu, phát lệnh điều khiển và cung cấp API.
- **PostgreSQL:** lưu người dùng, thiết bị, thẻ, quyền và nhật ký.
- **Frontend React:** giao diện quản trị, gọi REST API và nhận sự kiện thời gian thực.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Firmware | ESP32, Arduino framework, C++ |
| Đầu đọc thẻ | MFRC522 / RC522 |
| Giao tiếp thiết bị | MQTT, PubSubClient |
| Định dạng thông điệp | JSON, ArduinoJson |
| Backend | Java 17, Spring Boot 3.3.4 |
| API | Spring Web, REST |
| Cơ sở dữ liệu | PostgreSQL, Spring Data JPA / Hibernate |
| Bảo mật API | Spring Security, JWT (JJWT) |
| Cập nhật thời gian thực | Spring WebSocket, STOMP/SockJS |
| Frontend | React 18, Vite, React Router |
| Giao diện và biểu đồ | Tailwind CSS, Recharts, Lucide |
| Công cụ build | Maven, npm |

## Cấu trúc thư mục

```text
Project/
├── Lequochuy/
│   └── Lequochuy.ino          # Firmware ESP32
├── lock-backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/lock/
│       │   ├── access/        # Rule engine và nghiệp vụ truy cập
│       │   ├── card/          # Quản lý thẻ RFID
│       │   ├── config/        # Cấu hình ứng dụng
│       │   ├── device/        # Quản lý thiết bị
│       │   ├── security/      # Xác thực và JWT
│       │   └── ...            # API, báo cáo, người dùng, MQTT
│       └── resources/
│           └── application.properties
└── lock-frontend/
    ├── package.json
    └── src/
        ├── api/               # REST client và WebSocket
        ├── components/        # Thành phần giao diện dùng chung
        ├── context/           # Trạng thái xác thực
        └── pages/             # Các trang dashboard
```

## Yêu cầu môi trường

Cài đặt các thành phần sau trước khi chạy:

- JDK 17.
- Apache Maven tương thích với dự án.
- PostgreSQL.
- MQTT broker, ví dụ EMQX.
- Node.js và npm.
- Arduino IDE hoặc PlatformIO để nạp firmware ESP32.
- ESP32, RC522, relay và khóa điện phù hợp để thử nghiệm phần cứng.

## Cài đặt và chạy

### 1. Cấu hình PostgreSQL

Tạo cơ sở dữ liệu:

```sql
CREATE DATABASE smart_lock;
```

Mở `lock-backend/src/main/resources/application.properties`, cập nhật thông tin kết nối PostgreSQL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/smart_lock
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Các biến `${DB_USERNAME}` và `${DB_PASSWORD}` cần được cung cấp trong môi trường chạy. Nếu muốn cấu hình trực tiếp khi phát triển cục bộ, hãy dùng thông tin của máy bạn và không commit mật khẩu thật lên Git.

### 2. Cấu hình MQTT và JWT

Trong `application.properties`, thiết lập broker MQTT và secret JWT phù hợp với môi trường:

```properties
mqtt.broker-url=tcp://localhost:1883
mqtt.client-id=smart-lock-backend
mqtt.username=${MQTT_USERNAME:}
mqtt.password=${MQTT_PASSWORD:}
jwt.secret=${JWT_SECRET}
jwt.expiration-minutes=480
```

`JWT_SECRET` phải là chuỗi ngẫu nhiên đủ mạnh, tối thiểu 32 ký tự cho HS256. Broker trong ví dụ cần tồn tại và có thể truy cập từ backend lẫn ESP32.

### 3. Chạy backend

Mở terminal tại thư mục `lock-backend`:

```bash
mvn spring-boot:run
```

Backend mặc định phục vụ tại:

```text
http://localhost:8080
```

Cấu hình hiện tại sử dụng `spring.jpa.hibernate.ddl-auto=update` để cập nhật schema khi chạy. Với môi trường triển khai thật, nên chuyển sang công cụ quản lý migration như Flyway hoặc Liquibase và kiểm thử quá trình nâng cấp dữ liệu.

Tài khoản quản trị khởi tạo được mô tả trong README của backend. Nếu sử dụng tài khoản mặc định khi chạy lần đầu, hãy đổi mật khẩu ngay và không dùng thông tin mặc định trong môi trường công khai.

### 4. Chạy frontend

Mở terminal khác tại `lock-frontend`:

```bash
npm install
npm run dev
```

Mở địa chỉ Vite hiển thị trong terminal, thường là:

```text
http://localhost:5173
```

Nếu backend chạy ở địa chỉ khác, cập nhật `API_BASE` trong `src/api/client.js` và kiểm tra cấu hình CORS ở backend. Không nên dùng máy chủ phát triển Vite làm máy chủ production.

### 5. Nạp firmware ESP32

1. Mở `Lequochuy/Lequochuy.ino` bằng Arduino IDE.
2. Cài các thư viện cần thiết: `PubSubClient`, `MFRC522`, `ArduinoJson` và các thư viện đi kèm ESP32 Arduino Core.
3. Cập nhật Wi-Fi, địa chỉ MQTT broker và mã thiết bị trong firmware.
4. Kiểm tra sơ đồ chân và mức kích hoạt relay theo module thực tế.
5. Chọn đúng board ESP32 và cổng serial, sau đó nạp chương trình.
6. Mở Serial Monitor để kiểm tra kết nối Wi-Fi, MQTT và sự kiện quẹt thẻ.

Không nạp firmware trước khi thay thông tin Wi-Fi/broker mẫu và kiểm tra kỹ phần điều khiển relay trên phần cứng thực tế.

## Luồng hoạt động chính

### Quẹt thẻ RFID

1. Người dùng đưa thẻ đến gần RC522.
2. ESP32 đọc UID và gửi thông điệp truy cập qua MQTT.
3. Backend tiếp nhận thông điệp và tạo ngữ cảnh kiểm tra.
4. Rule engine đánh giá các quy tắc đang áp dụng.
5. Backend ghi kết quả vào nhật ký.
6. Nếu được cho phép, backend gửi lệnh mở khóa đến thiết bị; nếu bị từ chối, kết quả và lý do được trả về theo luồng MQTT.
7. Dashboard nhận các sự kiện cập nhật qua WebSocket/STOMP.

### Rule engine

Các rule được tổ chức thành các thành phần riêng, có thứ tự ưu tiên. Những rule hiện có trong mã nguồn bao gồm:

- `RateLimitRule`: giới hạn tần suất thử truy cập.
- `DeviceOnlineRule`: kiểm tra trạng thái thiết bị cho thao tác điều khiển từ xa.
- `CardStatusRule`: kiểm tra trạng thái thẻ.
- `RoomPermissionRule`: kiểm tra quyền vào phòng.
- `TimeWindowRule`: kiểm tra khung thời gian truy cập.
- `QrTokenStatusRule`: kiểm tra trạng thái token QR trong luồng tương ứng.

Quyết định cuối cùng phụ thuộc vào cấu hình, ngữ cảnh và thứ tự rule trong mã nguồn. Khi bổ sung rule mới, cần viết kiểm thử cho trường hợp cho phép, từ chối và lỗi dữ liệu.

### Chế độ offline

Firmware lưu whitelist và hàng đợi sự kiện trong flash. Khi mất mạng, thiết bị có thể sử dụng whitelist đã cache theo logic firmware; các sự kiện được xếp hàng để gửi lại khi MQTT hoạt động trở lại và được xóa sau khi nhận ACK.

Chế độ offline có giới hạn: dữ liệu quyền được cache có thể cũ, dung lượng hàng đợi hữu hạn và thời gian sự kiện phụ thuộc vào việc đồng bộ đồng hồ. Cần kiểm thử tình huống mất nguồn, đầy hàng đợi, mất ACK và thay đổi quyền trong thời gian thiết bị offline.

## Giao tiếp MQTT

Firmware hiện xây dựng topic theo mã thiết bị và sử dụng các nhóm topic cho yêu cầu truy cập, trạng thái, lệnh, kết quả, whitelist và đồng bộ sự kiện offline. Backend cũng đăng ký các topic tương ứng. Cấu trúc topic cụ thể phải được giữ đồng nhất giữa `Lequochuy.ino` và cấu hình/listener MQTT của backend.

Một số topic theo thiết kế backend được mô tả trong mã nguồn:

```text
lock/{deviceCode}/access-attempt
lock/{deviceCode}/status
lock/{deviceCode}/command
lock/{deviceCode}/result
```

Payload truy cập dùng JSON và chứa UID thẻ; payload lệnh có trường `command`, ví dụ `UNLOCK`. Khi thay đổi topic hoặc cấu trúc JSON, cần cập nhật đồng thời firmware, backend và kiểm thử tích hợp.

## API và cập nhật thời gian thực

Backend cung cấp REST API dưới tiền tố `/api`. Các nhóm endpoint chính gồm:

| Nhóm | Mục đích |
|---|---|
| `/api/auth` | Đăng nhập và thông tin người dùng hiện tại |
| `/api/cards` | Danh sách, đăng ký, enrollment và trạng thái thẻ |
| `/api/locks` | Điều khiển khóa |
| `/api/devices` | Theo dõi và quản lý thiết bị |
| `/api/access-logs` | Tra cứu lịch sử truy cập |
| `/api/reports` | Báo cáo và thống kê |
| Các endpoint người dùng, tòa nhà/phòng | Quản lý cấu trúc và tài khoản hệ thống |

Phần lớn API nghiệp vụ yêu cầu JWT. Sau khi đăng nhập, client cần gửi token theo header:

```http
Authorization: Bearer <token>
```

Dashboard sử dụng WebSocket/STOMP để nhận cập nhật thời gian thực. Endpoint và topic cụ thể cần được đối chiếu với cấu hình WebSocket và các controller trong backend khi tích hợp client khác.

## Cấu hình thiết bị ESP32

Trong `Lequochuy.ino`, các thông số phần cứng và kết nối được khai báo ở đầu file, gồm:

- `WIFI_SSID`, `WIFI_PASSWORD`: thông tin Wi-Fi.
- `MQTT_BROKER`, `MQTT_PORT`: địa chỉ và cổng broker.
- `DEVICE_CODE`: mã định danh thiết bị.
- `SS_PIN`, `RST_PIN`: chân kết nối RC522.
- `RELAY_PIN`: chân điều khiển relay.
- `RELAY_ACTIVE_LOW`: xác định relay kích mức thấp hay mức cao.
- `UNLOCK_DURATION_MS`: thời gian mở khóa.
- `HEARTBEAT_INTERVAL_MS`: chu kỳ heartbeat.

Các chân trong firmware chỉ là cấu hình hiện tại, không phải sơ đồ đấu nối phổ quát. Hãy kiểm tra pinout của board, điện áp relay và dòng điện của khóa trước khi cấp nguồn. Không cấp nguồn khóa điện công suất lớn trực tiếp từ chân GPIO của ESP32.

## Bảo mật và triển khai thực tế

Trước khi triển khai ngoài môi trường thử nghiệm:

- Xóa thông tin Wi-Fi và thông tin xác thực đã ghi trực tiếp trong firmware; đổi mật khẩu Wi-Fi/broker nếu từng chia sẻ mã nguồn.
- Không dùng broker MQTT công khai cho hệ thống khóa thật. Dùng broker riêng, xác thực mạnh, ACL theo topic và TLS khi có thể.
- Đổi tài khoản/mật khẩu quản trị mặc định và đặt `JWT_SECRET` ngẫu nhiên, riêng biệt cho môi trường triển khai.
- Không commit mật khẩu database, token, secret hoặc file cấu hình chứa thông tin nhạy cảm.
- Giới hạn quyền theo vai trò ở backend; xác thực đăng nhập không đồng nghĩa với phân quyền quản trị đầy đủ.
- Bật HTTPS cho dashboard/API khi triển khai; cấu hình CORS theo domain được phép thay vì mở rộng tùy tiện.
- Bổ sung kiểm thử tự động cho rule engine, API, luồng MQTT và xử lý lỗi.
- Xem xét chống phát lại thông điệp, xác thực thiết bị, quản lý khóa bí mật, nhật ký kiểm toán và cơ chế mở cửa an toàn khi mất điện.
- Kiểm thử relay/khóa ở môi trường an toàn trước khi gắn lên cửa sử dụng thật.

## Hạn chế cần lưu ý

- Firmware có cấu hình Wi-Fi được khai báo trực tiếp trong mã nguồn; cần chuyển sang cấu hình an toàn trước khi chia sẻ hoặc triển khai.
- Firmware dùng MQTT không mã hóa trên cổng `1883` theo cấu hình hiện tại; không phù hợp để sử dụng trên mạng không tin cậy.
- Whitelist cache và hàng đợi offline có giới hạn dung lượng; cần đánh giá độ nhất quán khi quyền truy cập thay đổi.
- Cần xác nhận mọi chức năng QR từ đầu đến cuối giữa giao diện, backend và firmware; sự hiện diện của trang QR không tự chứng minh thiết bị đã quét và xác thực QR thành công.
- README này mô tả cấu trúc và chức năng dựa trên mã nguồn hiện có; các tính năng cần được xác minh bằng kiểm thử tích hợp trước khi xem là sẵn sàng cho production.

## Đóng góp và phát triển

Khi phát triển thêm chức năng:

1. Tách nghiệp vụ thành service/rule riêng thay vì dồn logic vào controller.
2. Giữ hợp đồng API và MQTT nhất quán giữa frontend, backend và firmware.
3. Thêm kiểm thử cho cả trường hợp thành công, bị từ chối, mất kết nối và dữ liệu không hợp lệ.
4. Không đưa thông tin xác thực hoặc dữ liệu cá nhân thật vào repository.
5. Cập nhật tài liệu khi thay đổi cách cài đặt, cấu hình hoặc luồng nghiệp vụ.
