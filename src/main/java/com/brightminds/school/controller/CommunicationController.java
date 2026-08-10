package com.brightminds.school.controller;

import com.brightminds.school.entity.Message;
import com.brightminds.school.repository.MessageRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/communication") @RequiredArgsConstructor @Tag(name = "Communication")
public class CommunicationController {
    private final MessageRepository repo;
    private final SchoolClassRepository classRepo;

    @GetMapping public List<Message> list() { return repo.findAllByOrderBySentAtDesc(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Message send(@RequestBody MessageRequest req) {
        var msg = Message.builder().body(req.getBody()).subject(req.getSubject())
                .channel(req.getChannel() != null ? req.getChannel() : "sms")
                .audience(req.getAudience()).sentAt(Instant.now()).build();
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(msg::setSchoolClass);
        return repo.save(msg);
    }

    @Data public static class MessageRequest {
        private String body; private String subject; private String channel;
        private String audience; private UUID classId;
    }
}
