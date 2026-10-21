import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE } from "./client";

let client = null;

export function connectSocket({ onAccessLog, onCardEnrollment } = {}) {
  if (client) return client;

  client = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
    reconnectDelay: 4000,
    onConnect: () => {
      if (onAccessLog) {
        client.subscribe("/topic/access-logs", (msg) => onAccessLog(JSON.parse(msg.body)));
      }
      if (onCardEnrollment) {
        client.subscribe("/topic/card-enrollment", (msg) => onCardEnrollment(JSON.parse(msg.body)));
      }
    },
  });

  client.activate();
  return client;
}

export function disconnectSocket() {
  if (client) {
    client.deactivate();
    client = null;
  }
}
