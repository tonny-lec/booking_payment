package com.booking.iam.adapter.persistence;

import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.HashedPassword;
import com.booking.iam.domain.model.User;
import com.booking.iam.domain.model.UserId;
import com.booking.iam.domain.model.UserStatus;
import org.hibernate.exception.ConstraintViolationException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaUserRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("JpaUserRepository integration")
class JpaUserRepositoryIntegrationTest {

    private static final String VALID_BCRYPT_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYlQBc/G2HHe";

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("save/find")
    class SaveFind {

        @Test
        @DisplayName("should persist user and find by id and email")
        void shouldPersistUserAndFindByIdAndEmail() {
            User user = User.create(Email.of("user@example.com"), HashedPassword.of(VALID_BCRYPT_HASH));

            User saved = userRepository.save(user);

            assertThat(saved.id()).isNotNull();
            assertThat(userRepository.findById(saved.id())).contains(saved);
            assertThat(userRepository.findByEmail(saved.email())).contains(saved);
        }

        @Test
        @DisplayName("should return empty when email does not exist")
        void shouldReturnEmptyWhenEmailDoesNotExist() {
            assertThat(userRepository.findByEmail(Email.of("missing@example.com"))).isEmpty();
        }

        @Test
        @DisplayName("should update existing user")
        void shouldUpdateExistingUser() {
            User created = userRepository.save(User.create(
                    Email.of("update@example.com"),
                    HashedPassword.of(VALID_BCRYPT_HASH)
            ));

            created.suspend();
            userRepository.save(created);

            User reloaded = userRepository.findById(created.id()).orElseThrow();
            assertThat(reloaded.status()).isEqualTo(UserStatus.SUSPENDED);
        }
    }

    @Test
    @DisplayName("should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        userRepository.save(User.create(Email.of("dup@example.com"), HashedPassword.of(VALID_BCRYPT_HASH)));

        assertThatThrownBy(() -> {
            userRepository.save(User.create(Email.of("dup@example.com"), HashedPassword.of(VALID_BCRYPT_HASH)));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    @DisplayName("should return empty when id does not exist")
    void shouldReturnEmptyWhenIdDoesNotExist() {
        assertThat(userRepository.findById(UserId.generate())).isEmpty();
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.booking.iam.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.booking.iam.adapter.persistence.repository")
    static class TestApplication {
    }
}
