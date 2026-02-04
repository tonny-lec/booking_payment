package com.booking.iam.adapter.persistence;

import com.booking.iam.adapter.persistence.entity.UserEntity;
import com.booking.iam.adapter.persistence.repository.UserJpaRepository;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.User;
import com.booking.iam.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA-based implementation of {@link UserRepository}.
 */
@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public JpaUserRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.value())
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value())
                .map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        return userJpaRepository.save(entity).toDomain();
    }
}
