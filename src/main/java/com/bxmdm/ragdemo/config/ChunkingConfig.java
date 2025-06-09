package com.bxmdm.ragdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag.chunking")
public class ChunkingConfig {
    
    private int maxChunkSize = 1000;
    private int minChunkSize = 100;
    private int overlapSize = 50;
    private int maxTokens = 300;
    private String strategy = "semantic"; // semantic, sliding_window, hybrid
    
    // Getters and Setters
    public int getMaxChunkSize() { return maxChunkSize; }
    public void setMaxChunkSize(int maxChunkSize) { this.maxChunkSize = maxChunkSize; }
    
    public int getMinChunkSize() { return minChunkSize; }
    public void setMinChunkSize(int minChunkSize) { this.minChunkSize = minChunkSize; }
    
    public int getOverlapSize() { return overlapSize; }
    public void setOverlapSize(int overlapSize) { this.overlapSize = overlapSize; }
    
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
}