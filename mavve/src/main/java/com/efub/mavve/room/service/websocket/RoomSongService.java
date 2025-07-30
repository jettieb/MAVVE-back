package com.efub.mavve.room.service.websocket;

import com.efub.mavve.auth.domain.User;
import com.efub.mavve.global.exception.ExceptionCode;
import com.efub.mavve.global.exception.MavveException;
import com.efub.mavve.room.payload.request.AddSongRequestPayload;
import com.efub.mavve.room.payload.request.DeleteSongRequestPayload;
import com.efub.mavve.room.payload.response.AddSongResponsePayload;
import com.efub.mavve.room.payload.response.DeleteSongResponsePayload;
import com.efub.mavve.room.payload.response.NextSongResponsePayload;
import com.efub.mavve.room.payload.summary.SongRedis;
import com.efub.mavve.room.service.redis.RoomSongRedisService;
import com.efub.mavve.room.service.redis.RoomUserRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSongService {
    private final RoomSongRedisService songRedisService;
    private final RoomUserRedisService userRedisService;
    private final PrincipalUtil principalUtil;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ThreadPoolTaskScheduler taskScheduler;

    private static final int BROADCAST_DELAY_SECONDS = 1;

    public AddSongResponsePayload addSong(Long roomCode, AddSongRequestPayload request) {
        SongRedis songAdd = songRedisService.addSong(roomCode, request.getSong());
        return AddSongResponsePayload.from(songAdd);
    }

    public DeleteSongResponsePayload deleteSongs(Long roomCode, DeleteSongRequestPayload request) {
        songRedisService.deleteSongs(roomCode, request.getSongIds());
        return DeleteSongResponsePayload.from(request.getSongIds());
    }

    // 다음 노래 예약 스케쥴링
    public void scheduleNextSong(Long roomCode, SongRedis currentSong){
        LocalDateTime nextSongStartTime = LocalDateTime.now()
                .plus(Duration.ofMillis(currentSong.getDuration()))
                .plusSeconds(BROADCAST_DELAY_SECONDS); // 전송 시간 고려해 1초 더 지연
        Instant scheduleTime = nextSongStartTime
                .atZone(ZoneId.systemDefault())
                .toInstant();

        taskScheduler.schedule(() -> {
            // 방에 사용자 있는지 확인하고 스케쥴링
            if(!userRedisService.hasUsers(roomCode)){
                songRedisService.deleteCurrentSong(roomCode);
                return;
            }
            SongRedis nextSong = songRedisService.getNextSong(roomCode, currentSong);
            broadcastNextSong(roomCode, nextSong, nextSongStartTime);
        }, scheduleTime);
        log.info("next song schedule!");
    }

    // 다음 노래 전환 브로드캐스트
    private void broadcastNextSong(Long roomCode, SongRedis nextSong, LocalDateTime startTime){
        songRedisService.addCurrentSong(roomCode, nextSong, startTime);
        log.info("next song!");
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/songs", NextSongResponsePayload.from(nextSong, startTime));
        scheduleNextSong(roomCode, nextSong);   // 다음 노래 스케쥴링
    }

    // 노래 정보 받을 주소 구독 -> 사용자 정보 저장
    public User subscribe(Long roomCode, Principal principal, Message<?> message) {
        User user = principalUtil.principalToUser(principal);

        // sessionId 가져오기
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(accessor == null || accessor.getSessionId() == null){
            throw new MavveException(ExceptionCode.NO_SESSION_ID);
        }
        String sessionId = accessor.getSessionId();

        userRedisService.addUser(roomCode, user, sessionId);
        return user;
    }
}
