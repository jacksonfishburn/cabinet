package com.cabinet.cabinet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"cabinet.storage-dir=target/test-storage",
		"cabinet.max-size-mb=10",
		"cabinet.token=test-token",
		"spring.datasource.url=jdbc:h2:mem:cabinet-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class CabinetApplicationTests {

	@Test
	void contextLoads() {
	}

}
