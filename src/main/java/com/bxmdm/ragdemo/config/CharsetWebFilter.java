package com.bxmdm.ragdemo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
//
//@Component
//public class CharsetWebFilter implements WebFilter {
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        exchange.getResponse().getHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
//        return chain.filter(exchange);
//    }
//}