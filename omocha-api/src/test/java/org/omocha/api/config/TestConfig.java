package org.omocha.api.config;

import org.omocha.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

	@Autowired
	private jakarta.persistence.EntityManager entityManager;

	@Bean
	public DatabaseCleaner databaseCleaner() {
		return new DatabaseCleaner(entityManager);
	}
}
