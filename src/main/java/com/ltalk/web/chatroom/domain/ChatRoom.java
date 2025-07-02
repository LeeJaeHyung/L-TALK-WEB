package com.ltalk.web.chatroom.domain;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    public ChatRoom(ChatRoomCreateRequest chatRoomCreateRequest, List<ChatRoomMember> chatRoomMembers) {
        this.name = chatRoomCreateRequest.getName();
        this.type = chatRoomCreateRequest.getType();
        this.memberList = chatRoomMembers;
        this.participantCount = chatRoomMembers.size();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    @Column(nullable = false)
    private int participantCount;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomMember> memberList = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> chatList = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
