package theMenteeChat.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {

    // 방 ID를 키로, 해당 방의 WebSocketSession 리스트를 값으로 가지는 맵
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // WebSocket 연결이 성공적으로 수립된 후 호출됨
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션에서 roomId 가져와서 맵에 추가
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null) {
            sessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
            log.info("User connected to room {}", roomId);
            System.out.printf("User connected to room %s\n", roomId);
        }
    }

    // 클라이언트로부터 메시지가 수신되었을 때 호출됨
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        String payload = message.getPayload();
        Map<String, String> messageMap = objectMapper.readValue(payload, Map.class);

        String roomId = messageMap.get("roomId");
        String userId = messageMap.get("userId");
        String chatMessage = messageMap.get("message");

        // 대상 Room의 모든 세션으로 메시지 전송
        List<WebSocketSession> targetSessions = sessions.get(roomId);
        if (targetSessions != null) {
            for (WebSocketSession targetSession : targetSessions) {
                targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageMap)));
            }
        }
    }

    // WebSocket 연결이 종료된 후 호출됨
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 세션에서 roomId을 가져와서 해당 방의 세션 리스트에서 삭제
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null) {
            List<WebSocketSession> sessionsInRoom = sessions.get(roomId);
            if (sessionsInRoom != null) {
                sessionsInRoom.remove(session);
                // 방에 더 이상 세션이 없으면 방 삭제
                if (sessionsInRoom.isEmpty()) {
                    sessions.remove(roomId);
                }
            }
            log.info("User disconnected from room {}", roomId);
        }
    }
}
