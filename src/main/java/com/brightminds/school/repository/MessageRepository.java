package com.brightminds.school.repository;

import com.brightminds.school.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByOrderBySentAtDesc();
}
