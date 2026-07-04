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

        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            throw new IllegalArgumentException(
                    "Only MSU Naawan email addresses are allowed.");
        }

        user.setStatus("PENDING");
        user.setDateRegistered(java.time.LocalDate.now());
        user.setEmailVerified(false);

        return userRepository.save(user);
    }

    public User createStaffAccount(User user) {

        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            throw new IllegalArgumentException(
                    "Only MSU Naawan email addresses are allowed.");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }

        user.setStatus("APPROVED");
        user.setEmailVerified(true);
        user.setDateRegistered(java.time.LocalDate.now());

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

        User user = userRepository.findByEmail(email).orElse(null);

        System.out.println("========== LOGIN DEBUG ==========");
        System.out.println("INPUT EMAIL = " + email);
        System.out.println("INPUT PASSWORD = " + password);
        System.out.println("INPUT ROLE = " + role);

        if (user == null) {
            System.out.println("USER NOT FOUND");
            return null;
        }
        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            return null;
        }

        System.out.println("DB EMAIL = " + user.getEmail());
        System.out.println("DB PASSWORD = " + user.getPassword());
        System.out.println("DB ROLE = " + user.getRole());
        System.out.println("DB STATUS = " + user.getStatus());

        boolean passwordMatch = user.getPassword().equals(password);
        boolean roleMatch = user.getRole().equalsIgnoreCase(role);
        boolean statusMatch = user.getStatus() != null &&
                user.getStatus().equalsIgnoreCase("APPROVED");

        System.out.println("PASSWORD MATCH = " + passwordMatch);
        System.out.println("ROLE MATCH = " + roleMatch);
        System.out.println("STATUS MATCH = " + statusMatch);

        return passwordMatch && roleMatch && statusMatch
                ? user
                : null;
    }

    public void seedUsers() {

        createIfNotExist(
                "Proponent User",
                "proponent@msunaawan.edu.ph",
                "proponentpassword1234",
                "PROPONENT");

        createIfNotExist(
                "RPS Staff",
                "rpsstaff@msunaawan.edu.ph",
                "rpsstaffpassword1234",
                "RPS_STAFF");

        createIfNotExist(
                "RPS Admin",
                "rpsadmin@msunaawan.edu.ph",
                "rpsadminpassword1234",
                "RPS_ADMIN");

        createIfNotExist(
                "OVCRIGE User",
                "ovcrige@msunaawan.edu.ph",
                "ovcrigepassword1234",
                "OVCRIGE");

        createIfNotExist(
                "REC User",
                "rec@msunaawan.edu.ph",
                "recpassword1234",
                "REC");

        createIfNotExist(
                "OVCAF User",
                "ovcaf@msunaawan.edu.ph",
                "ovcafpassword1234",
                "OVCAF");

        createIfNotExist(
                "OC User",
                "oc@msunaawan.edu.ph",
                "ocpassword1234",
                "OC");

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {

            if (user.getStatus() == null) {
                user.setStatus("APPROVED");
            }

            if (user.getEmailVerified() == null) {
                user.setEmailVerified(true);
            }

            if (user.getDateRegistered() == null) {
                user.setDateRegistered(java.time.LocalDate.now());
            }

            userRepository.save(user);
        }
    }

    public java.util.Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().trim().isEmpty()) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            user.setPassword(updatedUser.getPassword());
        }
        if (updatedUser.getDepartmentOffice() != null) {
            user.setDepartmentOffice(updatedUser.getDepartmentOffice());
        }
        if (updatedUser.getContactNumber() != null) {
            user.setContactNumber(updatedUser.getContactNumber());
        }
        if (updatedUser.getSpecialization() != null) {
            user.setSpecialization(updatedUser.getSpecialization());
        }

        return userRepository.save(user);
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
