package com.familytree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Spring Boot application class for Family Tree Manager.
 *
 * This application provides a comprehensive family tree management system
 * with features including genealogy tracking, GEDCOM import/export,
 * media management, and collaborative editing.
 *
 * @author Family Tree Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
// @EnableCaching  // Will enable in later phases when Redis is needed
@EnableJpaAuditing
public class FamilyTreeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyTreeApplication.class, args);
    }
}
