package com.efub.mavve.room.controller.websocket;

import com.efub.mavve.auth.domain.User;
import com.efub.mavve.room.payload.request.AddSongRequestPayload;
import com.efub.mavve.room.payload.request.DeleteSongRequestPayload;
import com.efub.mavve.room.payload.response.AddSongResponsePayload;
import com.efub.mavve.room.payload.response.DeleteSongResponsePayload;
import com.efub.mavve.room.payload.response.MessageType;
import com.efub.mavve.room.payload.response.SubscribeResponsePayload;
import com.efub.mavve.room.service.websocket.RoomSongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
@MessageMapping("/rooms")
public class RoomSongController {
    private final SimpMessageSendingOperations messagingTemplate;
    private final RoomSongService roomSongService;

    @SubscribeMapping("/{roomCode}/songs")
    public SubscribeResponsePayload subscribe(@DestinationVariable Long roomCode,
                                              Principal principal,
                                              Message<?> message) {
        User user = roomSongService.subscribe(roomCode, principal, message);
        log.info("user{} subscribe {} room", user.getUserId(), roomCode);
        return new SubscribeResponsePayload(MessageType.SUBSCRIBE_COMPLETE);
    }

    @MessageMapping("/{roomCode}/add-song")
    public void addSong(@DestinationVariable Long roomCode, @Payload AddSongRequestPayload request) {
        AddSongResponsePayload response = roomSongService.addSong(roomCode, request);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/songs", response);
    }

    @MessageMapping("/{roomCode}/delete-song")
    public void deleteSongs(@DestinationVariable Long roomCode, @Payload DeleteSongRequestPayload request){
        DeleteSongResponsePayload response = roomSongService.deleteSongs(roomCode, request);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/songs", response);
    }
}
