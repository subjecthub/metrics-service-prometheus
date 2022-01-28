package com.migratorydata.sandbox.metrics;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Consumer implements MigratoryDataListener {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final Properties consumerProperties;
    private final List<String> topicList;
    private final Metrics metrics;

    private final MigratoryDataClient client;

    public Consumer(Properties props, String topicStats, Metrics metrics) {
        this.consumerProperties = new Properties();
        for (String pp : props.stringPropertyNames()) {
            this.consumerProperties.put(pp, props.get(pp));
        }

        this.metrics = metrics;

        client = new MigratoryDataClient();
        client.setServers(props.getProperty("push.servers").split(","));
        client.setEntitlementToken(props.getProperty("token"));
        client.setListener(this);

        this.topicList = Arrays.asList(topicStats);

        client.subscribe(topicList);
    }

    public void begin() {
        client.connect();
    }

    public void end() {
        client.disconnect();
    }

    @Override
    public void onMessage(MigratoryDataMessage m) {
        if (logger.isDebugEnabled()) {
            logger.debug("App-Cloud-Metrics-{}", m);
        }

        try {
            metrics.update(new JSONObject(new String(m.getContent())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatus(String s, String s1) {
        logger.debug("App-Cloud-Metrics-{}-{}", s, s1);
    }
}
