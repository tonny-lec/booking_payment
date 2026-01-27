package com.booking.domain.iam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UserStatus} enumeration.
 */
@DisplayName("UserStatus")
class UserStatusTest {

    @Nested
    @DisplayName("Status codes")
    class StatusCodes {

        @Test
        @DisplayName("PENDING_VERIFICATION should have correct code")
        void pendingVerificationShouldHaveCorrectCode() {
            assertThat(UserStatus.PENDING_VERIFICATION.code())
                    .isEqualTo("pending_verification");
        }

        @Test
        @DisplayName("ACTIVE should have correct code")
        void activeShouldHaveCorrectCode() {
            assertThat(UserStatus.ACTIVE.code()).isEqualTo("active");
        }

        @Test
        @DisplayName("LOCKED should have correct code")
        void lockedShouldHaveCorrectCode() {
            assertThat(UserStatus.LOCKED.code()).isEqualTo("locked");
        }

        @Test
        @DisplayName("SUSPENDED should have correct code")
        void suspendedShouldHaveCorrectCode() {
            assertThat(UserStatus.SUSPENDED.code()).isEqualTo("suspended");
        }

        @Test
        @DisplayName("DEACTIVATED should have correct code")
        void deactivatedShouldHaveCorrectCode() {
            assertThat(UserStatus.DEACTIVATED.code()).isEqualTo("deactivated");
        }
    }

    @Nested
    @DisplayName("Authentication capability")
    class AuthenticationCapability {

        @Test
        @DisplayName("only ACTIVE users can authenticate")
        void onlyActiveUsersCanAuthenticate() {
            assertThat(UserStatus.ACTIVE.canAuthenticate()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"PENDING_VERIFICATION", "LOCKED", "SUSPENDED", "DEACTIVATED"})
        @DisplayName("non-ACTIVE users cannot authenticate")
        void nonActiveUsersCannotAuthenticate(UserStatus status) {
            assertThat(status.canAuthenticate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Operational status")
    class OperationalStatus {

        @Test
        @DisplayName("only ACTIVE status is operational")
        void onlyActiveIsOperational() {
            assertThat(UserStatus.ACTIVE.isOperational()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"PENDING_VERIFICATION", "LOCKED", "SUSPENDED", "DEACTIVATED"})
        @DisplayName("non-ACTIVE statuses are not operational")
        void nonActiveStatusesAreNotOperational(UserStatus status) {
            assertThat(status.isOperational()).isFalse();
        }
    }

    @Nested
    @DisplayName("Restricted status")
    class RestrictedStatus {

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"LOCKED", "SUSPENDED", "DEACTIVATED"})
        @DisplayName("LOCKED, SUSPENDED, DEACTIVATED are restricted")
        void restrictedStatuses(UserStatus status) {
            assertThat(status.isRestricted()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"PENDING_VERIFICATION", "ACTIVE"})
        @DisplayName("PENDING_VERIFICATION and ACTIVE are not restricted")
        void nonRestrictedStatuses(UserStatus status) {
            assertThat(status.isRestricted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Transition to ACTIVE")
    class TransitionToActive {

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"PENDING_VERIFICATION", "LOCKED", "SUSPENDED"})
        @DisplayName("can transition to ACTIVE from PENDING_VERIFICATION, LOCKED, SUSPENDED")
        void canTransitionToActiveFromValidStates(UserStatus status) {
            assertThat(status.canTransitionToActive()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"ACTIVE", "DEACTIVATED"})
        @DisplayName("cannot transition to ACTIVE from ACTIVE or DEACTIVATED")
        void cannotTransitionToActiveFromInvalidStates(UserStatus status) {
            assertThat(status.canTransitionToActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Description")
    class Description {

        @ParameterizedTest
        @EnumSource(UserStatus.class)
        @DisplayName("all statuses should have non-empty description")
        void allStatusesShouldHaveDescription(UserStatus status) {
            assertThat(status.description())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("fromCode() factory method")
    class FromCodeFactoryMethod {

        @Test
        @DisplayName("should return PENDING_VERIFICATION for 'pending_verification'")
        void shouldReturnPendingVerificationForCode() {
            assertThat(UserStatus.fromCode("pending_verification"))
                    .isEqualTo(UserStatus.PENDING_VERIFICATION);
        }

        @Test
        @DisplayName("should return ACTIVE for 'active'")
        void shouldReturnActiveForCode() {
            assertThat(UserStatus.fromCode("active"))
                    .isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("should return LOCKED for 'locked'")
        void shouldReturnLockedForCode() {
            assertThat(UserStatus.fromCode("locked"))
                    .isEqualTo(UserStatus.LOCKED);
        }

        @Test
        @DisplayName("should return SUSPENDED for 'suspended'")
        void shouldReturnSuspendedForCode() {
            assertThat(UserStatus.fromCode("suspended"))
                    .isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should return DEACTIVATED for 'deactivated'")
        void shouldReturnDeactivatedForCode() {
            assertThat(UserStatus.fromCode("deactivated"))
                    .isEqualTo(UserStatus.DEACTIVATED);
        }

        @Test
        @DisplayName("should throw exception for null code")
        void shouldThrowExceptionForNullCode() {
            assertThatThrownBy(() -> UserStatus.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "unknown", "ACTIVE", "Active", "invalid_status"})
        @DisplayName("should throw exception for unknown code")
        void shouldThrowExceptionForUnknownCode(String unknownCode) {
            assertThatThrownBy(() -> UserStatus.fromCode(unknownCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown user status code");
        }
    }

    @Nested
    @DisplayName("Enum completeness")
    class EnumCompleteness {

        @Test
        @DisplayName("should have exactly 5 status values")
        void shouldHaveExactly5StatusValues() {
            assertThat(UserStatus.values()).hasSize(5);
        }

        @ParameterizedTest
        @EnumSource(UserStatus.class)
        @DisplayName("all statuses should have unique codes")
        void allStatusesShouldHaveUniqueCodes(UserStatus status) {
            long countWithSameCode = java.util.Arrays.stream(UserStatus.values())
                    .filter(s -> s.code().equals(status.code()))
                    .count();
            assertThat(countWithSameCode).isEqualTo(1);
        }

        @ParameterizedTest
        @EnumSource(UserStatus.class)
        @DisplayName("fromCode should round-trip for all statuses")
        void fromCodeShouldRoundTripForAllStatuses(UserStatus status) {
            assertThat(UserStatus.fromCode(status.code())).isEqualTo(status);
        }
    }
}
