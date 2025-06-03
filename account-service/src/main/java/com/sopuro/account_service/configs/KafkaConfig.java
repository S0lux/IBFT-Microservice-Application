package com.sopuro.account_service.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Configuration
@EnableKafka
@EnableKafkaRetryTopic
public class KafkaConfig {
}
