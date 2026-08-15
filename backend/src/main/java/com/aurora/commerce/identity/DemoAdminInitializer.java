package com.aurora.commerce.identity;

import com.aurora.commerce.shared.config.CommerceBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class DemoAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoAdminInitializer.class);

    private final CommerceBootstrapProperties properties;
    private final IdentityService identityService;

    DemoAdminInitializer(CommerceBootstrapProperties properties, IdentityService identityService) {
        this.properties = properties;
        this.identityService = identityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.enabled()) {
            identityService.ensureAdmin(properties.adminEmail(), properties.adminPassword());
            LOGGER.info("Demo administrator is available as {}", properties.adminEmail());
        }
    }
}
