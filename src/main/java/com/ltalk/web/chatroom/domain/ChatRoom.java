package com.ltalk.web.chatroom.domain;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    public ChatRoom(ChatRoomCreateRequest chatRoomCreateRequest, Set<ChatRoomMember> chatRoomMembers) {
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
    private Set<ChatRoomMember> memberList = new HashSet<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Chat> chatList = new HashSet<>();


    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
