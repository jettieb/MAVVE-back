package com.efub.mavve.room.service;

import com.efub.mavve.auth.domain.User;
import com.efub.mavve.global.exception.ExceptionCode;
import com.efub.mavve.global.exception.MavveException;
import com.efub.mavve.room.domain.Room;
import com.efub.mavve.room.domain.RoomLike;
import com.efub.mavve.room.dto.projection.RoomLikeCountProjection;
import com.efub.mavve.room.dto.request.RoomCreateRequest;
import com.efub.mavve.room.dto.request.RoomUpdateRequest;
import com.efub.mavve.room.dto.response.RoomEnterResponse;
import com.efub.mavve.room.dto.response.RoomListResponse;
import com.efub.mavve.room.dto.response.RoomResponse;
import com.efub.mavve.room.payload.summary.CurrentSongSummary;
import com.efub.mavve.room.payload.summary.SongRedis;
import com.efub.mavve.room.repository.RoomRepository;
import com.efub.mavve.room.service.redis.RoomSongRedisService;
import com.efub.mavve.room.service.redis.RoomUserRedisService;
import com.efub.mavve.room.service.websocket.RoomSongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.efub.mavve.room.repository.RoomLikeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomSongRedisService roomSongRedisService;
    private final RoomUserRedisService roomUserRedisService;
    private final RoomSongService roomSongService;
    private final RoomPlaylistService roomPlaylistService;
    private final RoomLikeRepository roomLikeRepository;

    // 방 생성
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, User user){
        Room room = roomRepository.save(request.toEntity(user));
        return RoomResponse.from(room, 0, false);
    }

    // 방 수정
    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpdateRequest request, User user){
        Room room = findByRoomId(roomId);
        authorizeUser(user, room);

        if(request.roomName()!=null){room.changeRoomName(request.roomName());}
        if(request.tag()!=null){room.changeTag(request.tag());}
        if(request.imageURL()!=null){room.changeImageURL(request.imageURL());}
        room.changeIsPublic(request.isPublic());
        int likeCount = roomLikeRepository.countByRoom(room);

        return RoomResponse.from(room, likeCount, getLiked(room, user));
    }

    // 방 삭제
    @Transactional
    public void deleteRoom(Long roomId, User user){
        Room room = findByRoomId(roomId);
        authorizeUser(user, room); // 방장인지 확인
        roomRepository.delete(room);
    }

    // 공개된 방 리스트 전체 조회
    @Transactional(readOnly = true)
    public RoomListResponse getListRoom(User user) {
        List<Room> roomList = roomRepository.findAll();

        List<RoomResponse> responseList = roomList.stream().filter(Room::isPublic)
                .map(room -> {
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());

        return RoomListResponse.from(responseList);
    }

    // 내가 만든 방 리스트 조회
    @Transactional(readOnly = true)
    public RoomListResponse getUserListRoom(User user){
        List<Room> roomList = roomRepository.findByUser(user);

        List<RoomResponse> responseList = roomList.stream()
                .map(room -> {
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());

        return RoomListResponse.from(responseList);
    }

    // 내가 좋아요 누른 방 조회
    @Transactional(readOnly = true)
    public RoomListResponse getUserLikeListRoom(User user){
        List<Room> roomList = roomLikeRepository.findRoomByUser(user);

        List<RoomResponse> responseList = roomList.stream()
                .map(room -> {
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());
        return RoomListResponse.from(responseList);
    }

    // 조회순으로 공개된 방 리스트 Top5 조회
    @Transactional(readOnly = true)
    public RoomListResponse getHotListRoom(User user){
        List<Room> roomList = roomRepository.findTop5ByIsPublicTrueOrderByViewCountDesc();

        List<RoomResponse> responseList = roomList.stream()
                .map(room -> {
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());
        return RoomListResponse.from(responseList);
    }

    // 좋아요 순으로 TOP5 공개된 방 조회
    @Transactional(readOnly = true)
    public RoomListResponse getLikeListRoom(User user){
        List<RoomLikeCountProjection> projection = roomRepository.findTop5ByIsPublicTrueOrderByLikeCountDesc();

        List<RoomResponse> responseList = projection.stream()
                .map(proj -> {
                    Room room = findByRoomId(proj.getRoomId());
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());
        return RoomListResponse.from(responseList);
    }

    // 방 검색
    @Transactional(readOnly = true)
    public RoomListResponse searchRoom(String keyword, User user){
        List<Room> roomList = roomRepository.findByRoomNameContainingIgnoreCaseAndIsPublicTrueOrderByCreatedAtDesc(keyword);

        List<RoomResponse> responseList = roomList.stream()
                .map(room -> {
                    int likeCount = roomLikeRepository.countByRoom(room);
                    return RoomResponse.from(room, likeCount, getLiked(room, user));})
                .collect(Collectors.toList());
        return RoomListResponse.from(responseList);
    }

    // roomId 조회
    @Transactional(readOnly = true)
    public Room findByRoomId(Long roomId){
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new MavveException(ExceptionCode.ROOM_NOT_FOUND));
    }

    // 수정, 삭제 권한 확인
    private void authorizeUser(User user, Room room){
        if(!user.getUserId().equals(room.getUser().getUserId())){
            throw new MavveException(ExceptionCode.ROOM_OWNER_MISMATCH);
        }
    }

    // 방 입장 시 방 정보 + 플레이리스트 + 현재 재생중인 노래 전달
    @Transactional(readOnly = true)
    public RoomEnterResponse getRoomEnterInfo(Long roomId) {
        Room room = findByRoomId(roomId);
        CurrentSongSummary currentSong = handleFirstEnter(roomId);

        List<SongRedis> songList = roomSongRedisService.getAllSongsInRoom(roomId);
        log.info("song count : {}", songList.size());
        String duration = getTotalDurationTime(songList);
        return RoomEnterResponse.from(room, songList, duration, currentSong);
    }

    // 좋아요 눌렀는지 확인
    @Transactional(readOnly = true)
    public boolean getLiked(Room room, User user){
        Optional<RoomLike> existingLike = roomLikeRepository.findByUserAndRoom(user, room);

        if (existingLike.isPresent()) {return true;}
        else {return false;}
    }

    // 첫 입장자인 경우 처리
    private CurrentSongSummary handleFirstEnter(Long roomId) {
        // 입장자 없음 + 현재 저장되어 있는 노래가 없는 경우 첫 노래부터 재생 시작
        if (!roomUserRedisService.hasUsers(roomId) && !roomSongRedisService.hasCurrentSong(roomId)) {
            // 방 생성된 뒤 플레이리스트의 노래들 redis에 저장
            if(!roomSongRedisService.hasSongs(roomId)){
                roomPlaylistService.addSongsByPlaylist(roomId);
            }

            // 첫 번째 노래 재생 시작
            SongRedis firstSong = roomSongRedisService.getFirstSongInRoom(roomId);
            if (firstSong != null) {
                log.info("first user!!");
                LocalDateTime startTime = LocalDateTime.now();

                // 현재 노래 저장 및 다음 노래 스케쥴링
                roomSongRedisService.addCurrentSong(roomId, firstSong, startTime);
                roomSongService.scheduleNextSong(roomId, firstSong);
                return CurrentSongSummary.from(firstSong, startTime);
            } else{
                return null;    // 저장된 노래 시스트가 없을 경우 null 반환
            }
        }
        return roomSongRedisService.getCurrentSong(roomId);
    }

    // 노래 총 재생시간 String으로 변환
    private String getTotalDurationTime(List<SongRedis> songs){
        int totalMillis = songs.stream()
                .mapToInt(SongRedis::getDuration)
                .sum();
        int totalSeconds = totalMillis / 1000;
        
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if(hours > 0) return String.format("%d시간 %d분 %d초", hours, minutes, seconds);
        else if (minutes > 0) return String.format("%d분 %d초", minutes, seconds);
        else return String.format("%d초", seconds);
    }
}
