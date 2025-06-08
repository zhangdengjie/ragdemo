package com.bxmdm.ragdemo;

import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class QdrantTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    public void testQdrant() throws ExecutionException, InterruptedException {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                "192.168.0.123",
                6334,
                false
        );
        // 不设置任何认证信息
        QdrantClient client = new QdrantClient(builder.build());
        List<String> list = client.listCollectionsAsync().get();
        System.out.println(list);
//        Collections.CollectionOperationResponse test = client.createCollectionAsync("test",
//                        Collections.VectorParams.newBuilder()
//                                .setDistance(Collections.Distance.Cosine)
//                                .setSize(4)
//                                .build())
//                .get();
//        System.out.println(test.getResult());
//        ListenableFuture<Collections.CollectionOperationResponse> test = client.deleteCollectionAsync("test");
//        System.out.println(test.get().getResult());
        QdrantVectorStore vectorStore = QdrantVectorStore.builder(client, embeddingModel).initializeSchema(true).build();
        List<Document> hello = vectorStore.similaritySearch("hello");
        System.out.println(hello);
    }
}
