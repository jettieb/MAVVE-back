package com.efub.mavve.room.controller;

import com.efub.mavve.auth.domain.User;
import com.efub.mavve.playlist.dto.summary.PlaylistSummary;
import com.efub.mavve.room.dto.request.RoomCreateRequest;
import com.efub.mavve.room.dto.request.RoomUpdateRequest;
import com.efub.mavve.room.dto.response.*;
import com.efub.mavve.room.service.websocket.RoomChatService;
import com.efub.mavve.room.service.RoomLikeService;
import com.efub.mavve.room.service.RoomPlaylistService;
import com.efub.mavve.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class RoomController {
    private final RoomService roomService;
    private final RoomLikeService roomLikeService;
    private final RoomChatService roomChatService;
    private final RoomPlaylistService roomPlaylistService;

    // 방 생성
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody RoomCreateRequest request){
        RoomResponse response = roomService.createRoom(request, user);
        Long roomId = response.getRoomId();
        return ResponseEntity.created(URI.create("/rooms/"+roomId)).body(response);
    }

    // 방 수정
    @PatchMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable("roomId") Long roomId,
                                                   @AuthenticationPrincipal User user,
                                                   @Valid @RequestBody RoomUpdateRequest request){
        RoomResponse response = roomService.updateRoom(roomId, request, user);
        return ResponseEntity.ok(response);
    }

    // 방 삭제
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@AuthenticationPrincipal User user,
                                           @PathVariable("roomId") Long roomId){
        roomService.deleteRoom(roomId, user);
        return ResponseEntity.noContent().build();
    }

    // 공개된 방 리스트 전체 조회
    @GetMapping
    public ResponseEntity<RoomListResponse> getListRoom(@AuthenticationPrincipal User user){
        RoomListResponse response = roomService.getListRoom(user);
        return ResponseEntity.ok(response);
    }

    // 내가 만든 방 리스트 조회
    @GetMapping("/me")
    public ResponseEntity<RoomListResponse> getUserListRoom(@AuthenticationPrincipal User user){
        RoomListResponse response = roomService.getUserListRoom(user);
        return ResponseEntity.ok(response);
    }

    // 내가 좋아요 누른 방 리스트 조회
    @GetMapping("/like/me")
    public ResponseEntity<RoomListResponse> getUserLikeListRoom(@AuthenticationPrincipal User user){
        RoomListResponse response = roomService.getUserLikeListRoom(user);
        return ResponseEntity.ok(response);
    }

    // 조회순으로 공개된 방 리스트 Top5 조회
    @GetMapping("/hot")
    public ResponseEntity<RoomListResponse> getHotListRoom(@AuthenticationPrincipal User user){
        RoomListResponse response = roomService.getHotListRoom(user);
        return ResponseEntity.ok(response);
    }

    // 좋아요 순으로 TOP5 공개된 방 조회
    @GetMapping("/like")
    public ResponseEntity<RoomListResponse> getLikeListRoom(@AuthenticationPrincipal User user){
        RoomListResponse response = roomService.getLikeListRoom(user);
        return ResponseEntity.ok(response);
    }

    // 방 검색
    @GetMapping("/search")
    public ResponseEntity<RoomListResponse> searchRoom(@RequestParam("keyword") String keyword,
                                                       @AuthenticationPrincipal User user){
        RoomListResponse response = roomService.searchRoom(keyword, user);
        return ResponseEntity.ok(response);
    }

    // 방 좋아요
    @PostMapping("/{roomId}/like")
    public ResponseEntity<RoomLikeResponse> roomLike(@PathVariable("roomId") Long roomId,
                                                     @AuthenticationPrincipal User user) {
        RoomLikeResponse response = roomLikeService.roomLike(roomId, user);
        return ResponseEntity.ok(response);
    }

    // 방 입장
    @GetMapping("/{roomId}/enter")
    public ResponseEntity<RoomEnterResponse> getRoomEnterInfo(@PathVariable("roomId") Long roomId){
        RoomEnterResponse response = roomService.getRoomEnterInfo(roomId);
        return ResponseEntity.ok(response);
    }

    // 방에 플레이리스트 추가
    @PostMapping("/{roomId}/playlists")
    public void addPlaylistInRoom(@PathVariable("roomId") Long roomId,
                                  @RequestParam("playlistId") Long playlistId){
        roomPlaylistService.addPlaylistInRoom(roomId, playlistId);
    }

    // 방의 플레이리스트 조회
    @GetMapping("/{roomId}/playlists")
    public ResponseEntity<List<PlaylistSummary>> getPlaylistInRoom(@PathVariable("roomId") Long roomId){
        List<PlaylistSummary> response = roomPlaylistService.getPlaylistInRoom(roomId);
        return ResponseEntity.ok(response);
    }

}
