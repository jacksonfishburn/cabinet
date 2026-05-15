package com.cabinet.cabinet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"cabinet.storage-dir=target/test-storage",
		"cabinet.max-size-mb=10",
		"cabinet.token=test-token"
})
class CabinetApplicationTests {

	@Test
	void contextLoads() {
	}

}
