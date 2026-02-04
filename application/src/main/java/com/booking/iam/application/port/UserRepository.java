package com.booking.iam.application.port;

import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.User;
import com.booking.iam.domain.model.UserId;

import java.util.Optional;

/**
 * Port interface for user persistence operations.
 *
 * <p>This repository abstracts user storage for the IAM bounded context.
 * Implementations are provided by the adapter layer (e.g., JPA, JDBC).
 *
 * <p>Usage example:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class LoginUseCase {
 *     private final UserRepository userRepository;
 *
 *     public AuthenticationResult login(Email email, String password) {
 *         User user = userRepository.findByEmail(email)
 *             .orElseThrow(() -> new UserNotFoundException(email));
 *         AuthenticationResult result = user.authenticate(password, passwordMatcher);
 *         userRepository.save(user);
 *         return result;
 *     }
 * }
 * }</pre>
 */
public interface UserRepository {

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId the user ID
     * @return the user if found
     */
    Optional<User> findById(UserId userId);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address
     * @return the user if found
     */
    Optional<User> findByEmail(Email email);

    /**
     * Persists a user aggregate.
     *
     * @param user the user to persist
     * @return the persisted user (if the implementation enriches fields)
     */
    User save(User user);
}
