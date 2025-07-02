package org.omocha.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;

public class DatabaseCleaner implements InitializingBean {

	private final EntityManager entityManager;

	private List<String> tableNames;

	public DatabaseCleaner(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public void afterPropertiesSet() {
		tableNames = entityManager.getMetamodel().getEntities().stream()
			.filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
			.map(e -> {
				Table tableAnnotation = e.getJavaType().getAnnotation(Table.class);
				return tableAnnotation != null ? tableAnnotation.name() : convertToSnakeCase(e.getName());
			})
			.collect(Collectors.toList());
	}

	@Transactional
	public void execute() {
		entityManager.flush();

		String tableNamesStr = String.join(", ", tableNames);
		entityManager.createNativeQuery("TRUNCATE TABLE " + tableNamesStr + " RESTART IDENTITY CASCADE")
			.executeUpdate();
	}

	private String convertToSnakeCase(String name) {
		return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
	}
}
