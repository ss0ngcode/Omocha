package org.omocha.domain.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;

@Component
public class DatabaseCleaner implements InitializingBean {

	@PersistenceContext
	private EntityManager entityManager;

	private List<String> tableNames;

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
