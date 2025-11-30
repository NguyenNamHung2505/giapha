package com.familytree.service;

import com.familytree.dto.admin.CreateUserRequest;
import com.familytree.dto.admin.UpdateUserRequest;
import com.familytree.dto.admin.UserWithProfileResponse;
import com.familytree.dto.response.UserResponse;
import com.familytree.dto.user.UserTreeProfileResponse;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import com.familytree.model.User;
import com.familytree.model.UserTreeProfile;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.UserRepository;
import com.familytree.repository.UserTreeProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for admin operations - user management and profile mapping
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final UserTreeProfileRepository userTreeProfileRepository;
    private final FamilyTreeRepository treeRepository;
    private final IndividualRepository individualRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Verify that the requesting user is an admin
     */
    private void verifyAdmin(String usernameOrEmail) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        if (!user.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    /**
     * Find user by username or email
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String adminEmail, Pageable pageable) {
        verifyAdmin(adminEmail);
        return userRepository.findAll(pageable).map(UserResponse::fromUser);
    }

    /**
     * Get all users for a specific tree with their profile mappings
     */
    @Transactional(readOnly = true)
    public List<UserWithProfileResponse> getUsersWithProfiles(UUID treeId, String adminEmail) {
        verifyAdmin(adminEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found"));

        List<User> allUsers = userRepository.findAll();

        return allUsers.stream().map(user -> {
            UserWithProfileResponse response = UserWithProfileResponse.fromUser(user);

            // Check if user has a profile mapping for this tree
            Optional<UserTreeProfile> profile = userTreeProfileRepository
                    .findByUserIdAndTreeId(user.getId(), treeId);

            if (profile.isPresent()) {
                Individual ind = profile.get().getIndividual();
                response.setLinkedIndividual(UserWithProfileResponse.IndividualInfo.builder()
                        .id(ind.getId())
                        .fullName(buildFullName(ind))
                        .gender(ind.getGender() != null ? ind.getGender().name() : null)
                        .birthDate(ind.getBirthDate())
                        .profilePictureUrl(ind.getProfilePictureUrl())
                        .build());
            }

            return response;
        }).collect(Collectors.toList());
    }

    /**
     * Create a new user (admin only)
     */
    public UserResponse createUser(CreateUserRequest request, String adminUsernameOrEmail) {
        verifyAdmin(adminUsernameOrEmail);
        log.info("Admin '{}' creating new user with username '{}'", adminUsernameOrEmail, request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already registered");
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .admin(request.isAdmin())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        return UserResponse.fromUser(savedUser);
    }

    /**
     * Update a user (admin only)
     */
    public UserResponse updateUser(UUID userId, UpdateUserRequest request, String adminUsernameOrEmail) {
        verifyAdmin(adminUsernameOrEmail);
        log.info("Admin '{}' updating user '{}'", adminUsernameOrEmail, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Username already in use");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getAdmin() != null) {
            user.setAdmin(request.getAdmin());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);
        log.info("User '{}' updated", userId);

        return UserResponse.fromUser(savedUser);
    }

    /**
     * Delete a user (admin only)
     */
    public void deleteUser(UUID userId, String adminEmail) {
        verifyAdmin(adminEmail);
        log.info("Admin '{}' deleting user '{}'", adminEmail, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Prevent self-deletion
        User admin = findUserByUsernameOrEmail(adminEmail);
        if (admin.getId().equals(userId)) {
            throw new BadRequestException("Cannot delete your own account");
        }

        // Delete user's tree profiles first
        userTreeProfileRepository.deleteByUserId(userId);
        log.info("Deleted tree profiles for user '{}'", userId);

        userRepository.delete(user);
        log.info("User '{}' deleted", userId);
    }

    /**
     * Link a user to an individual in a tree (admin only)
     */
    public UserTreeProfileResponse linkUserToIndividual(UUID treeId, UUID userId, UUID individualId, String adminEmail) {
        verifyAdmin(adminEmail);
        log.info("Admin '{}' linking user '{}' to individual '{}' in tree '{}'",
                adminEmail, userId, individualId, treeId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        if (!individual.getTree().getId().equals(treeId)) {
            throw new BadRequestException("Individual does not belong to this tree");
        }

        // Check if mapping already exists
        Optional<UserTreeProfile> existingProfile = userTreeProfileRepository
                .findByUserIdAndTreeId(userId, treeId);

        UserTreeProfile profile;
        if (existingProfile.isPresent()) {
            profile = existingProfile.get();
            profile.setIndividual(individual);
        } else {
            profile = UserTreeProfile.builder()
                    .user(user)
                    .tree(tree)
                    .individual(individual)
                    .build();
        }

        UserTreeProfile savedProfile = userTreeProfileRepository.save(profile);
        log.info("User '{}' linked to individual '{}' in tree '{}'", userId, individualId, treeId);

        return convertToResponse(savedProfile);
    }

    /**
     * Unlink a user from an individual in a tree (admin only)
     */
    public void unlinkUserFromIndividual(UUID treeId, UUID userId, String adminEmail) {
        verifyAdmin(adminEmail);
        log.info("Admin '{}' unlinking user '{}' from tree '{}'", adminEmail, userId, treeId);

        userTreeProfileRepository.deleteByUserIdAndTreeId(userId, treeId);
        log.info("User '{}' unlinked from tree '{}'", userId, treeId);
    }

    private UserTreeProfileResponse convertToResponse(UserTreeProfile profile) {
        Individual individual = profile.getIndividual();

        return UserTreeProfileResponse.builder()
                .id(profile.getId())
                .treeId(profile.getTree().getId())
                .treeName(profile.getTree().getName())
                .individual(UserTreeProfileResponse.IndividualInfo.builder()
                        .id(individual.getId())
                        .givenName(individual.getGivenName())
                        .surname(individual.getSurname())
                        .fullName(buildFullName(individual))
                        .gender(individual.getGender() != null ? individual.getGender().name() : null)
                        .birthDate(individual.getBirthDate())
                        .profilePictureUrl(individual.getProfilePictureUrl())
                        .build())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    /**
     * Build full name - Vietnamese name order: Surname + Suffix + Given Name
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();
        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            fullName.append(individual.getSurname());
        }
        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getSuffix());
        }
        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getGivenName());
        }
        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }

    /**
     * Create user from individual in a tree
     * - Generates username from individual's full name (remove accents, lowercase, no spaces)
     * - If username exists, appends number (1, 2, 3...)
     * - Creates user and links to individual
     * - Skips if individual already has a linked user
     */
    public UserResponse createUserFromIndividual(UUID treeId, UUID individualId, String adminEmail) {
        verifyAdmin(adminEmail);
        log.info("Admin '{}' creating user from individual '{}' in tree '{}'", adminEmail, individualId, treeId);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        if (!individual.getTree().getId().equals(treeId)) {
            throw new BadRequestException("Individual does not belong to this tree");
        }

        // Check if individual already has a linked user
        Optional<UserTreeProfile> existingProfile = userTreeProfileRepository.findByIndividualId(individualId);
        if (existingProfile.isPresent()) {
            throw new BadRequestException("This individual already has a linked user: " + existingProfile.get().getUser().getUsername());
        }

        // Generate username from full name
        String fullName = buildFullName(individual);
        String baseUsername = generateUsername(fullName);
        String username = findAvailableUsername(baseUsername);

        // Default password for all users created from genealogy
        String defaultPassword = "123456";

        // Create user with generated email
        String email = username + "@familytree.local";
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(defaultPassword))
                .name(fullName)
                .admin(false)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {} and username: {}", savedUser.getId(), username);

        // Link user to individual
        UserTreeProfile profile = UserTreeProfile.builder()
                .user(savedUser)
                .tree(tree)
                .individual(individual)
                .build();
        userTreeProfileRepository.save(profile);
        log.info("User '{}' automatically linked to individual '{}' in tree '{}'", savedUser.getId(), individualId, treeId);

        return UserResponse.fromUser(savedUser);
    }

    /**
     * Bulk create users from all individuals in a tree that don't have linked users
     */
    public List<UserResponse> createUsersFromTree(UUID treeId, String adminEmail) {
        verifyAdmin(adminEmail);
        log.info("Admin '{}' bulk creating users from tree '{}'", adminEmail, treeId);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Get all individuals in the tree
        List<Individual> individuals = individualRepository.findByTreeId(treeId);

        // Get all individuals that already have user profiles
        List<UUID> linkedIndividualIds = userTreeProfileRepository.findByTreeId(treeId)
                .stream()
                .map(profile -> profile.getIndividual().getId())
                .collect(Collectors.toList());

        // Filter individuals without linked users
        List<Individual> unlinkedIndividuals = individuals.stream()
                .filter(ind -> !linkedIndividualIds.contains(ind.getId()))
                .collect(Collectors.toList());

        log.info("Found {} unlinked individuals out of {} total", unlinkedIndividuals.size(), individuals.size());

        // Create users for each unlinked individual
        return unlinkedIndividuals.stream()
                .map(individual -> {
                    try {
                        String fullName = buildFullName(individual);
                        String baseUsername = generateUsername(fullName);
                        String username = findAvailableUsername(baseUsername);
                        String defaultPassword = "123456";
                        String email = username + "@familytree.local";

                        User user = User.builder()
                                .username(username)
                                .email(email)
                                .passwordHash(passwordEncoder.encode(defaultPassword))
                                .name(fullName)
                                .admin(false)
                                .enabled(true)
                                .build();

                        User savedUser = userRepository.save(user);

                        UserTreeProfile profile = UserTreeProfile.builder()
                                .user(savedUser)
                                .tree(tree)
                                .individual(individual)
                                .build();
                        userTreeProfileRepository.save(profile);

                        log.info("Created user '{}' for individual '{}'", username, fullName);
                        return UserResponse.fromUser(savedUser);
                    } catch (Exception e) {
                        log.error("Failed to create user for individual '{}': {}", individual.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Generate username from full name
     * - Remove Vietnamese accents
     * - Remove spaces
     * - Convert to lowercase
     * Example: "Nguyễn Nam Hưng" -> "nguyennamhung"
     */
    private String generateUsername(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "user";
        }

        // Normalize and remove accents
        String normalized = Normalizer.normalize(fullName, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        // Replace đ/Đ with d
        withoutAccents = withoutAccents.replace("đ", "d").replace("Đ", "D");

        // Remove spaces and convert to lowercase
        String username = withoutAccents.replaceAll("\\s+", "").toLowerCase();

        // Remove any non-alphanumeric characters
        username = username.replaceAll("[^a-z0-9]", "");

        // Ensure minimum length
        if (username.length() < 3) {
            username = "user" + username;
        }

        return username;
    }

    /**
     * Find an available username by appending numbers if needed
     * Example: nguyennamhung -> nguyennamhung1 -> nguyennamhung2
     */
    private String findAvailableUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 0;

        while (userRepository.existsByUsername(username)) {
            counter++;
            username = baseUsername + counter;
        }

        return username;
    }
}
