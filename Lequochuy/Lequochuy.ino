
#include <WiFi.h>
#include <PubSubClient.h>
#include <SPI.h>
#include <MFRC522.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>


const char* WIFI_SSID     = "Galaxy A12 F315";
const char* WIFI_PASSWORD = "Lequochuy";

const char* MQTT_BROKER = "broker.emqx.io"; // đổi sang broker riêng khi lắp thật
const int   MQTT_PORT   = 1883;
const char* MQTT_USER   = "";
const char* MQTT_PASS   = "";

const char* DEVICE_CODE = "ESP32-101";

#define RELAY_ACTIVE_LOW true
const unsigned long UNLOCK_DURATION_MS   = 4000;
const unsigned long HEARTBEAT_INTERVAL_MS = 30000;

#define MAX_QUEUED_EVENTS 30
#define MAX_WHITELIST_SIZE 50

const unsigned long ACK_TIMEOUT_MS = 10000;


#define SS_PIN           5
#define RST_PIN          22
#define RELAY_PIN        4
#define FEEDBACK_LED_PIN 2


WiFiClient espClient;
PubSubClient mqtt(espClient);
MFRC522 rfid(SS_PIN, RST_PIN);
Preferences prefs;

String topicAccessAttempt, topicStatus, topicCommand, topicResult;
String topicWhitelist, topicWhitelistRequest, topicOfflineEvents, topicOfflineAck;

bool unlocked = false;
unsigned long unlockedAt = 0;
unsigned long lastHeartbeat = 0;
bool wasConnected = false;

String whitelist[MAX_WHITELIST_SIZE];
int whitelistCount = 0;


bool waitingForAck = false;
unsigned long ackWaitStart = 0;

unsigned long lastWifiAttempt = 0;
unsigned long lastMqttAttempt = 0;
const unsigned long WIFI_RETRY_INTERVAL_MS = 10000;
const unsigned long MQTT_RETRY_INTERVAL_MS = 5000;


struct BlinkState {
  unsigned long transitions[8];
  int count = 0;
  int index = 0;
  bool active = false;
} blink;

void startBlink(const int* segmentsMs, int n) {
  unsigned long t = millis();
  blink.count = n;
  blink.index = 0;
  for (int i = 0; i < n && i < 8; i++) {
    t += segmentsMs[i];
    blink.transitions[i] = t;
  }
  blink.active = true;
  digitalWrite(FEEDBACK_LED_PIN, HIGH);
}

void updateBlink() {
  if (!blink.active) return;
  if (millis() >= blink.transitions[blink.index]) {
    blink.index++;
    if (blink.index >= blink.count) {
      digitalWrite(FEEDBACK_LED_PIN, LOW);
      blink.active = false;
      return;
    }
    digitalWrite(FEEDBACK_LED_PIN, (blink.index % 2 == 0) ? HIGH : LOW);
  }
}

void signalGranted() {
  int pattern[] = {800};
  startBlink(pattern, 1);
}

void signalDenied() {
  int pattern[] = {150, 150, 150};
  startBlink(pattern, 3);
}

// ---------- ĐIỀU KHIỂN RELAY ----------

void setRelay(bool energize) {
  bool level = RELAY_ACTIVE_LOW ? !energize : energize;
  digitalWrite(RELAY_PIN, level ? HIGH : LOW);
}

void doUnlock() { 
  setRelay(true);
  unlocked = true;
  unlockedAt = millis();
  Serial.println("[LOCK] Mo khoa");
}

void doLock() {
  setRelay(false);
  unlocked = false;
  Serial.println("[LOCK] Da khoa");
}

// ---------- THỜI GIAN THẬT  ----------

void setupTime() {
  configTime(7 * 3600, 0, "pool.ntp.org", "time.google.com");
}

String nowIso() {
  time_t now;
  time(&now);
  struct tm timeinfo;
  localtime_r(&now, &timeinfo);
  char buf[25];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%S", &timeinfo);
  return String(buf);
}

bool timeIsSynced() {
  time_t now;
  time(&now);
  return now > 1700000000;
}

// ---------- LƯU TRỮ WHITELIST  ----------

void saveWhitelistToFlash() {
  String joined = "";
  for (int i = 0; i < whitelistCount; i++) {
    joined += whitelist[i];
    if (i < whitelistCount - 1) joined += "|";
  }
  prefs.putString("whitelist", joined);
  Serial.printf("[CACHE] Da luu %d UID vao bo nho trong\n", whitelistCount);
}

void loadWhitelistFromFlash() {
  String joined = prefs.getString("whitelist", "");
  whitelistCount = 0;
  if (joined.length() == 0) return;

  int start = 0;
  while (start < (int)joined.length() && whitelistCount < MAX_WHITELIST_SIZE) {
    int sep = joined.indexOf('|', start);
    if (sep == -1) sep = joined.length();
    whitelist[whitelistCount++] = joined.substring(start, sep);
    start = sep + 1;
  }
  Serial.printf("[CACHE] Da nap %d UID tu bo nho trong\n", whitelistCount);
}

bool isInWhitelist(const String& uid) {
  for (int i = 0; i < whitelistCount; i++) {
    if (whitelist[i] == uid) return true;
  }
  return false;
}

void printWhitelistCache() {
  if (whitelistCount == 0) {
    Serial.println("[CACHE] RONG — chua co UID nao (chua nhan tu server, chua ADD tay)");
    return;
  }
  Serial.printf("[CACHE] Dang co %d UID:\n", whitelistCount);
  for (int i = 0; i < whitelistCount; i++) {
    Serial.printf("  %d. %s\n", i + 1, whitelist[i].c_str());
  }
}

// ---------- HÀNG ĐỢI SỰ KIỆN OFFLINE  ----------

String loadOfflineQueueRaw() { return prefs.getString("offlineQ", ""); }
void saveOfflineQueueRaw(const String& raw) { prefs.putString("offlineQ", raw); }

int countQueuedEvents(const String& raw) {
  if (raw.length() == 0) return 0;
  int count = 1;
  for (unsigned int i = 0; i < raw.length(); i++) if (raw[i] == ';') count++;
  return count;
}

void enqueueOfflineEvent(const String& uid, const String& isoTime) {
  String raw = loadOfflineQueueRaw();
  String entry = uid + "@" + isoTime;

  if (countQueuedEvents(raw) >= MAX_QUEUED_EVENTS) {
    int firstSep = raw.indexOf(';');
    if (firstSep != -1) raw = raw.substring(firstSep + 1);
    Serial.println("[QUEUE] Hang doi day, bo su kien cu nhat");
  }

  raw = (raw.length() == 0) ? entry : (raw + ";" + entry);
  saveOfflineQueueRaw(raw);
  Serial.printf("[QUEUE] Da xep hang: %s\n", entry.c_str());
}

void flushOfflineQueue() {
  if (waitingForAck) return; 
  String raw = loadOfflineQueueRaw();
  if (raw.length() == 0) return;
  if (!mqtt.connected()) return;

  JsonDocument doc;
  JsonArray events = doc["events"].to<JsonArray>();

  int start = 0;
  while (start < (int)raw.length()) {
    int sep = raw.indexOf(';', start);
    if (sep == -1) sep = raw.length();
    String entry = raw.substring(start, sep);

    int at = entry.indexOf('@');
    if (at != -1) {
      JsonObject ev = events.add<JsonObject>();
      ev["cardUid"] = entry.substring(0, at);
      ev["occurredAt"] = entry.substring(at + 1);
    }
    start = sep + 1;
  }

  String payload;
  serializeJson(doc, payload);
  bool ok = mqtt.publish(topicOfflineEvents.c_str(), payload.c_str());

  if (ok) {
    waitingForAck = true;
    ackWaitStart = millis();
    Serial.printf("[QUEUE] Da gui %d su kien, cho ACK tu backend...\n", events.size());
  } else {
    Serial.println("[QUEUE] Gui that bai (publish loi), se thu lai");
  }
}


void checkAckTimeout() {
  if (!waitingForAck) return;
  if (millis() - ackWaitStart > ACK_TIMEOUT_MS) {
    Serial.println("[QUEUE] Khong nhan duoc ACK dung han, se thu gui lai");
    waitingForAck = false; 
  }
}

void connectWiFi() {
  Serial.printf("[WIFI] Dang ket noi %s", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(400);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\n[WIFI] Da ket noi, IP: %s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("\n[WIFI] Chua ket noi duoc, se dung CHE DO OFFLINE (whitelist cache)");
  }
}

void maintainWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  if (millis() - lastWifiAttempt < WIFI_RETRY_INTERVAL_MS) return;
  lastWifiAttempt = millis();
  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

// ---------- MQTT ----------

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String message;
  for (unsigned int i = 0; i < length; i++) message += (char)payload[i];
  String topicStr = String(topic);

  Serial.printf("[MQTT] Nhan tu %s: %s\n", topic, message.c_str());

  if (topicStr == topicOfflineAck) {
    saveOfflineQueueRaw("");
    waitingForAck = false;
    Serial.println("[QUEUE] Backend da xac nhan (ACK) — xoa hang doi");
    return;
  }

  JsonDocument doc;
  if (deserializeJson(doc, message)) {
    Serial.println("[MQTT] Loi parse JSON, bo qua");
    return;
  }

  if (topicStr == topicWhitelist) {
    JsonArray uids = doc["uids"];
    whitelistCount = 0;
    for (JsonVariant v : uids) {
      if (whitelistCount >= MAX_WHITELIST_SIZE) break;
      whitelist[whitelistCount++] = v.as<String>();
    }
    saveWhitelistToFlash();
    Serial.printf("[MQTT] Da cap nhat whitelist: %d UID\n", whitelistCount);
  } else if (topicStr == topicCommand) {
    String command = doc["command"] | "";
    if (command == "UNLOCK") {
      doUnlock();
      signalGranted();
    } else if (command == "LOCK") {
      doLock();
    } else if (command == "RESTART") {
      Serial.println("[MQTT] RESTART sau 1 giay...");
      delay(1000);
      ESP.restart();
    }
  } else if (topicStr == topicResult) {
    bool allow = doc["allow"] | true;
    if (!allow) signalDenied();
  }
}

void connectMqtt() {
  Serial.print("[MQTT] Dang ket noi broker...");
  String clientId = "esp32-" + String(DEVICE_CODE);

  bool ok = (strlen(MQTT_USER) > 0)
                ? mqtt.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)
                : mqtt.connect(clientId.c_str());

  if (ok) {
    Serial.println(" OK");
    mqtt.subscribe(topicCommand.c_str());
    mqtt.subscribe(topicResult.c_str());
    mqtt.subscribe(topicWhitelist.c_str());
    mqtt.subscribe(topicOfflineAck.c_str());
  } else {
    Serial.printf(" that bai, rc=%d\n", mqtt.state());
  }
}

void maintainMqtt() {
  if (mqtt.connected()) return;
  if (WiFi.status() != WL_CONNECTED) return;
  if (millis() - lastMqttAttempt < MQTT_RETRY_INTERVAL_MS) return;
  lastMqttAttempt = millis();
  connectMqtt();
}

// ---------- GỬI DỮ LIỆU ----------

void publishJson(const String& topic, JsonDocument& doc) {
  String payload;
  serializeJson(doc, payload);
  mqtt.publish(topic.c_str(), payload.c_str());
}

void publishHeartbeat() {
  JsonDocument doc;
  doc["battery"] = 100; // TODO: thay bằng giá trị đo ADC thật nếu có mạch đo pin
  doc["signal"] = map(constrain(WiFi.RSSI(), -90, -30), -90, -30, 0, 100);
  doc["firmware"] = "v1.2.0-offline";
  publishJson(topicStatus, doc);
}

void publishAccessAttempt(const String& uid) {
  JsonDocument doc;
  doc["cardUid"] = uid;
  publishJson(topicAccessAttempt, doc);
}

void requestWhitelist() {
  bool ok = mqtt.publish(topicWhitelistRequest.c_str(), "");
  Serial.println(ok ? "[MQTT] Da gui yeu cau whitelist" : "[MQTT] Gui yeu cau whitelist THAT BAI");
}

// ---------- ĐỌC THẺ RFID ----------

String readCardUid() {
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
    if (i < rfid.uid.size - 1) uid += " ";
  }
  uid.toUpperCase();
  return uid;
}


void handleCardScanned(const String& uid) {
  bool online = mqtt.connected();

  if (online) {
    Serial.println("[SCAN] Dang online -> hoi server (luong day du: RoomPermission, TimeWindow...)");
    publishAccessAttempt(uid);
    return;
  }

  Serial.println("[SCAN] Dang OFFLINE -> tra whitelist da duoc server dong bo truoc do");
  printWhitelistCache();
  if (isInWhitelist(uid)) {
    Serial.println("[SCAN] UID co trong whitelist -> mo khoa ngay");
    doUnlock();
    signalGranted();
    String ts = timeIsSynced() ? nowIso() : ("unsynced-" + String(millis()));
    enqueueOfflineEvent(uid, ts);
  } else {
    Serial.println("[SCAN] UID khong co trong whitelist -> tu choi");
    signalDenied();
  }
}


void addUidToWhitelist(const String& uid) {
  if (isInWhitelist(uid)) {
    Serial.println("[SERIAL] UID nay da co san trong danh sach roi");
    return;
  }
  if (whitelistCount >= MAX_WHITELIST_SIZE) {
    Serial.println("[SERIAL] Danh sach da day, khong them duoc nua");
    return;
  }
  whitelist[whitelistCount++] = uid;
  saveWhitelistToFlash();
  Serial.printf("[SERIAL] Da them UID: %s (tong cong %d UID)\n", uid.c_str(), whitelistCount);
}

void removeUidFromWhitelist(const String& uid) {
  for (int i = 0; i < whitelistCount; i++) {
    if (whitelist[i] == uid) {
      for (int j = i; j < whitelistCount - 1; j++) whitelist[j] = whitelist[j + 1];
      whitelistCount--;
      saveWhitelistToFlash();
      Serial.printf("[SERIAL] Da xoa UID: %s (con lai %d UID)\n", uid.c_str(), whitelistCount);
      return;
    }
  }
  Serial.println("[SERIAL] Khong tim thay UID nay trong danh sach");
}

void clearWhitelist() {
  whitelistCount = 0;
  saveWhitelistToFlash();
  Serial.println("[SERIAL] Da xoa sach danh sach local");
}

void handleSerialCommand() {
  if (!Serial.available()) return;
  String line = Serial.readStringUntil('\n');
  line.trim();
  if (line.length() == 0) return;

  if (line.equalsIgnoreCase("LIST")) {
    printWhitelistCache();
  } else if (line.equalsIgnoreCase("CLEAR")) {
    clearWhitelist();
  } else if (line.startsWith("ADD ") || line.startsWith("add ")) {
    String uid = line.substring(4);
    uid.trim();
    uid.toUpperCase();
    addUidToWhitelist(uid);
  } else if (line.startsWith("REMOVE ") || line.startsWith("remove ")) {
    String uid = line.substring(7);
    uid.trim();
    uid.toUpperCase();
    removeUidFromWhitelist(uid);
  } else {
    Serial.println("[SERIAL] Lenh khong hop le. Dung: ADD <uid> | REMOVE <uid> | LIST | CLEAR");
  }
}

// ---------- SETUP ----------

void setup() {
  Serial.begin(115200);
  delay(300);

  pinMode(RELAY_PIN, OUTPUT);
  pinMode(FEEDBACK_LED_PIN, OUTPUT);
  digitalWrite(FEEDBACK_LED_PIN, LOW);
  doLock();

  prefs.begin("smartlock", false);
  loadWhitelistFromFlash();
  printWhitelistCache();

  topicAccessAttempt  = "lock/" + String(DEVICE_CODE) + "/access-attempt";
  topicStatus          = "lock/" + String(DEVICE_CODE) + "/status";
  topicCommand          = "lock/" + String(DEVICE_CODE) + "/command";
  topicResult           = "lock/" + String(DEVICE_CODE) + "/result";
  topicWhitelist         = "lock/" + String(DEVICE_CODE) + "/whitelist";
  topicWhitelistRequest  = "lock/" + String(DEVICE_CODE) + "/whitelist-request";
  topicOfflineEvents     = "lock/" + String(DEVICE_CODE) + "/offline-events";
  topicOfflineAck        = "lock/" + String(DEVICE_CODE) + "/offline-events-ack";

  connectWiFi();
  if (WiFi.status() == WL_CONNECTED) setupTime();

  SPI.begin();
  rfid.PCD_Init();
  Serial.println("[RFID] RC522 da san sang");

  mqtt.setServer(MQTT_BROKER, MQTT_PORT);
  mqtt.setBufferSize(1024);
  mqtt.setCallback(onMqttMessage);

  if (WiFi.status() == WL_CONNECTED) {
    connectMqtt();
    if (mqtt.connected()) {
      requestWhitelist();
      flushOfflineQueue();
      publishHeartbeat();
    }
  }
}

// ---------- LOOP ----------

void loop() {
  handleSerialCommand();
  maintainWifi();
  maintainMqtt();

  bool nowConnected = mqtt.connected();
  if (nowConnected) {
    mqtt.loop();

    if (!wasConnected) {
      Serial.println("[MQTT] Vua ket noi lai -> dong bo whitelist + gui bu su kien");
      if (!timeIsSynced()) setupTime();
      requestWhitelist();
    }
    flushOfflineQueue();
  }
  wasConnected = nowConnected;
  checkAckTimeout();
  updateBlink();

  if (unlocked && millis() - unlockedAt > UNLOCK_DURATION_MS) {
    doLock();
  }

  if (nowConnected && millis() - lastHeartbeat > HEARTBEAT_INTERVAL_MS) {
    publishHeartbeat();
    lastHeartbeat = millis();
  }

  if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
    String uid = readCardUid();
    Serial.printf("[RFID] Doc duoc the: %s\n", uid.c_str());
    handleCardScanned(uid);
    rfid.PICC_HaltA();
    rfid.PCD_StopCrypto1();
    delay(500);
  }
}
