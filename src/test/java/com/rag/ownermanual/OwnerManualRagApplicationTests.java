package com.rag.ownermanual;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration")
@Import(TestVectorStoreConfig.class)
class OwnerManualRagApplicationTests {

	@Test
	void contextLoads() {
	}

}
