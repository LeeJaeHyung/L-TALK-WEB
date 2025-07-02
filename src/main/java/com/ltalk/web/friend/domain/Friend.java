package com.ltalk.web.friend.domain;


import com.ltalk.web.member.domain.Member;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne (fetch = FetchType.EAGER)
    private Member fromMember;
    @ManyToOne (fetch = FetchType.EAGER)
    private Member toMember;
    @Enumerated(EnumType.STRING)
    private FriendStatus status;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Friend(Member fromMember, Member toMember, FriendStatus status) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = status;
    }
}
