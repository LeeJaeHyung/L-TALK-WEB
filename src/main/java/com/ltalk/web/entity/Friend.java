package com.ltalk.web.entity;

import com.ltalk.web.enums.FriendStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
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

    public Friend(Member fromMember, Member toMember, FriendStatus status) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = status;
    }
}
