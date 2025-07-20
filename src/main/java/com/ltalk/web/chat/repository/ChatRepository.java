package com.ltalk.web.chat.repository;

import com.ltalk.web.chat.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllBySender_IdIn(List<Long> senderIds);
}
