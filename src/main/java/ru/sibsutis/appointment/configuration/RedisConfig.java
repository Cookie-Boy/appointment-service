package ru.sibsutis.appointment.configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        log.info("Trying to create bean. Redis host: {}, port: {}", redisHost, redisPort);
        config.setHostName(redisHost);
        config.setPort(redisPort);
        return new LettuceConnectionFactory(config);
    }

    @PostConstruct
    public void checkRedisConnection() {
        log.info("Attempting to connect to Redis at {}:{}", redisHost, redisPort);
        try {
            RedisURI redisUri = RedisURI.Builder.redis(redisHost, redisPort).build();
            RedisClient client = RedisClient.create(redisUri);
            StatefulRedisConnection<String, String> connection = client.connect();
            String ping = connection.sync().ping();
            log.info("Redis connected successfully, ping response: {}", ping);
            connection.close();
            client.shutdown();
        } catch (Exception e) {
            log.error("Failed to connect to Redis at {}:{}", redisHost, redisPort, e);
        }
    }
}
