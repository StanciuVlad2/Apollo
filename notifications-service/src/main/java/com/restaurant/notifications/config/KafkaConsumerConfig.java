package com.restaurant.notifications.config;

import com.restaurant.notifications.kafka.OrderReadyEvent;
import com.restaurant.notifications.kafka.ReservationEvent;
import com.restaurant.notifications.kafka.VoucherIssuedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notifications-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    @Bean
    public ConsumerFactory<String, ReservationEvent> reservationConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        JsonDeserializer<ReservationEvent> deserializer = new JsonDeserializer<>(ReservationEvent.class, false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> reservationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reservationConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrderReadyEvent> orderReadyConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        JsonDeserializer<OrderReadyEvent> deserializer = new JsonDeserializer<>(OrderReadyEvent.class, false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderReadyEvent> orderReadyKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderReadyEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderReadyConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, VoucherIssuedEvent> voucherIssuedConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        JsonDeserializer<VoucherIssuedEvent> deserializer = new JsonDeserializer<>(VoucherIssuedEvent.class, false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VoucherIssuedEvent> voucherIssuedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, VoucherIssuedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(voucherIssuedConsumerFactory());
        return factory;
    }
}
