package com.ghunsai.dairy.config;

import com.ghunsai.dairy.entity.Role;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates the very first OWNER account from BOOTSTRAP_OWNER_USERNAME / BOOTSTRAP_OWNER_PASSWORD
 * when the database has no owner yet. There is no built-in default password. After the first start
 * remove the password variable and change it from the app.
 */
@Component
public class BootstrapOwnerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapOwnerRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public BootstrapOwnerRunner(UserRepository userRepository, PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.OWNER)) {
            return;
        }
        AppProperties.Owner owner = props.bootstrap() == null ? null : props.bootstrap().owner();
        if (owner == null || isBlank(owner.username()) || isBlank(owner.password())) {
            log.warn("No OWNER account exists and BOOTSTRAP_OWNER_USERNAME / BOOTSTRAP_OWNER_PASSWORD "
                    + "are not set. Nobody will be able to log in.");
            return;
        }
        if (owner.password().length() < 8 || owner.password().length() > 72) {
            throw new IllegalStateException("BOOTSTRAP_OWNER_PASSWORD must be 8 to 72 characters");
        }
        String username = owner.username().trim().toLowerCase(Locale.ROOT);
        String fullName = isBlank(owner.fullName()) ? "Owner" : owner.fullName().trim();
        userRepository.save(new User(username, fullName, passwordEncoder.encode(owner.password()), Role.OWNER));
        log.info("Created initial OWNER account '{}'", username);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
