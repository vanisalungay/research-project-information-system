package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public java.util.Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus("PENDING");
    }

    public User createUser(User user) {
        user.setStatus("PENDING");
        return userRepository.save(user);
    }

    public User approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("APPROVED");
        User saved = userRepository.save(user);
        emailService.sendApprovalEmail(saved.getEmail(), saved.getName());
        return saved;
    }

    public User rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("REJECTED");
        User saved = userRepository.save(user);
        emailService.sendRejectionEmail(saved.getEmail(), saved.getName());
        return saved;
    }

    public User login(String email, String password, String role) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password) && 
                             u.getRole().equalsIgnoreCase(role) && 
                             u.getStatus() != null && 
                             u.getStatus().equalsIgnoreCase("APPROVED"))
                .orElse(null);
    }

    public void seedUsers() {
        createIfNotExist("Proponent User", "proponent@gmail.com", "proponentpassword1234", "PROPONENT");
        createIfNotExist("RII Staff", "rii_staff@gmail.com", "riistaffpassword1234", "RII_STAFF");
        createIfNotExist("OVCAF User", "ovcaf@gmail.com", "ovcafpassword1234", "OVCAF");
        createIfNotExist("OVCRIGE User", "ovcrige@gmail.com", "ovcrigepassword1234", "OVCRIGE");
        createIfNotExist("REC User", "rec@gmail.com", "recpassword1234", "REC");
        createIfNotExist("RII Admin User", "rii_admin@gmail.com", "riiadminpassword1234", "RII_ADMIN");
        createIfNotExist("OC User", "oc@gmail.com", "ocpassword1234", "OC");

        // Migrate any existing users with null status to APPROVED
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (u.getStatus() == null) {
                u.setStatus("APPROVED");
                userRepository.save(u);
            }
        }
    }

    private void createIfNotExist(String name, String email, String password, String role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            user.setStatus("APPROVED");
            userRepository.save(user);
        }
    }
}
