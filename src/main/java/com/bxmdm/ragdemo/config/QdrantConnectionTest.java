//package com.bxmdm.ragdemo.config;
//
//import io.qdrant.client.QdrantClient;
//import io.qdrant.client.QdrantGrpcClient;
//import jakarta.annotation.PostConstruct;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class QdrantConnectionTest {
//
//    @PostConstruct
//    public void testConnection() {
//        try {
//            QdrantClient client = new QdrantClient(
//                QdrantGrpcClient.newBuilder("192.168.0.123", 6334, false)
//                    .build()
//            );
//
////            List<String> list = client.listCollectionsAsync().get();
//
//            // 尝试获取集合列表
////            Collections.ListCollectionsRequest request = Collections.ListCollectionsRequest.newBuilder().build();
//            var response = client.listCollectionsAsync().get();
//
//            System.out.println("Collections: " + response);
//
//        } catch (Exception e) {
//            System.err.println("Connection test failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}