package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<Notification> getNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Notification createNotification(Long userId, String message) {
        return createNotification(userId, message, null, null, null);
    }

    public Notification createNotification(Long userId, String message, String title, String type, Long proposalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setTitle(title != null ? title : "Notification");
        notification.setType(type);
        notification.setProposalId(proposalId);
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }
}
