package com.migratorydata.sandbox.metrics;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Metrics {

    private final PrometheusMeterRegistry prometheusRegistry;

    private Map<String, Metric> appToMetrics = new HashMap<>();

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public Metrics(PrometheusMeterRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }

    public void update(JSONObject result) {
        executor.execute(() -> {
            String op = result.getString("op");
            String serverName = result.getString("server");
            JSONArray metrics = result.getJSONArray("metrics");
            for (int i = 0; i < metrics.length(); i++) {
                String topicName = metrics.getJSONObject(i).getString("topic");
                String application = metrics.getJSONObject(i).getString("application");
                int value = metrics.getJSONObject(i).getInt("value");

                Metric metric = appToMetrics.get(application);
                if (metric == null) {
                    metric = new Metric(application, prometheusRegistry);
                    appToMetrics.put(application, metric);
                }
                metric.update(op, value, serverName);

                System.out.println(metric);
            }
        });
    }

}
