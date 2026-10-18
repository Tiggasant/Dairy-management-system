package com.ghunsai.dairy.security;

import com.ghunsai.dairy.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    void locksAfterTooManyFailuresAndUnlocksOnReset() {
        LoginAttemptService service = new LoginAttemptService();
        String key = "1.2.3.4|worker1";

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure(key);
            assertDoesNotThrow(() -> service.assertNotLocked(key));
        }
        service.recordFailure(key);

        assertThrows(ApiException.TooManyRequests.class, () -> service.assertNotLocked(key));

        service.reset(key);
        assertDoesNotThrow(() -> service.assertNotLocked(key));
    }

    @Test
    void differentClientsAreTrackedSeparately() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("attacker|worker1");
        }
        assertThrows(ApiException.TooManyRequests.class, () -> service.assertNotLocked("attacker|worker1"));
        assertDoesNotThrow(() -> service.assertNotLocked("shop-wifi|worker1"));
    }
}
