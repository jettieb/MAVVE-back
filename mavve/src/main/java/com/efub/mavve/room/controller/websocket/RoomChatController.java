package com.efub.mavve.room.controller.websocket;

import com.efub.mavve.room.dto.response.ChatListResponse;
import com.efub.mavve.room.payload.request.ChatRequestPayload;
import com.efub.mavve.room.payload.response.ChatResponsePayload;
import com.efub.mavve.room.service.websocket.RoomChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/rooms")
@MessageMapping("/rooms")
public class RoomChatController {
    private final SimpMessageSendingOperations messagingTemplate;
    private final RoomChatService roomChatService;

    @MessageMapping("/{roomCode}/chats")
    public void chat(@DestinationVariable Long roomCode,
                     @Payload ChatRequestPayload request,
                     Principal principal) {
        ChatResponsePayload response = roomChatService.createAndSendMessage(roomCode, request, principal);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/chats", response);
    }

    // 채팅 전체 조회(페이징)
    @GetMapping("/{roomId}/chats")
    public ResponseEntity<ChatListResponse> getAllChats(@PathVariable("roomId") Long roomId,
                                                        @RequestParam(value = "lastChatId", required = false) Long lastChatId){
        ChatListResponse response = roomChatService.getAllChats(roomId, lastChatId);
        return ResponseEntity.ok(response);
    }
}