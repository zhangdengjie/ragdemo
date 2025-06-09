package com.bxmdm.ragdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
//        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
//                .maxMessages(10)
//                .build();
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor(),MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你将作为一名Sense-U产品说明的智能助理，对于用户的使用问题作出解答")
                .build();
    }
}
