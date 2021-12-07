package com.migratorydata.sandbox.metrics;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Consumer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final KafkaConsumer<String, byte[]> consumer;
    private final Thread thread;

    private final Properties consumerProperties;
    private final List<String> topicList;
    private final Metrics metrics;

    public Consumer(Properties props, String topicStats, Metrics metrics) {
        this.consumerProperties = new Properties();
        for (String pp : props.stringPropertyNames()) {
            this.consumerProperties.put(pp, props.get(pp));
        }

        this.metrics = metrics;

        String groupId = "AuthorizationKafkaConsumer-" + UUID.randomUUID().toString().substring(0, 5);
        this.consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        this.consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        this.topicList = Arrays.asList(topicStats);

        consumer = new KafkaConsumer<>(this.consumerProperties);

        this.thread = new Thread(this);
        this.thread.setName("KafkaAgentConsumer-" + thread.getId());
        this.thread.setDaemon(true);
    }

    public void begin() {
        thread.start();
    }

    public void end() {
        closed.set(true);
        consumer.wakeup();
    }

    @Override
    public void run() {

//        ConsumerRebalanceListener rebalanceListener = new ConsumerRebalanceListener() {
//            @Override
//            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
//            }
//
//            @Override
//            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
//                for (TopicPartition p : partitions) {
//                    consumer.seekToBeginning(Collections.singleton(p));
//                }
//            }
//        };

//        consumer.subscribe(topicList, rebalanceListener);

        consumer.subscribe(topicList);

        try {
            while (!closed.get()) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, byte[]> record : records) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}-{}-{} key={}, value={}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
                    }
                    System.out.printf("%s-%d-%d, key = %s, value = %s --- %d %n", record.topic(), record.partition(), record.offset(), record.key(), new String(record.value()), record.timestamp());

                    try {
                        metrics.update(new JSONObject(new String(record.value())));
                    } catch (Exception e) {
                        e.printStackTrace();

                        if (e instanceof WakeupException) {
                            throw e;
                        }
                    }
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get()) throw e;
        } finally {
            consumer.close();
        }

    }
}
