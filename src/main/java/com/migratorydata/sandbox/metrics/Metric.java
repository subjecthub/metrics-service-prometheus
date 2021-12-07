package com.migratorydata.sandbox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Metric {

    private Map<String, Integer> serverNameToConnections = new HashMap<>();

    private AtomicLong numberOfClients = new AtomicLong(0);
    private final Counter messages;

    public final String application;

    public Metric(String application, PrometheusMeterRegistry prometheusRegistry) {
        this.application = application;

        Gauge.builder("active_connections", numberOfClients, AtomicLong::get)
                .description("Active connections at scrape.")
                .tag("app", application)
                .register(prometheusRegistry);

        messages = io.micrometer.core.instrument.Counter.builder("messages_total")
                .description("Total in/out messages.")
                .tag("app", application)
                .register(prometheusRegistry);
    }

    public void update(String op, int value, String serverName) {
        if ("connections".equals(op)) {
            Integer connections = serverNameToConnections.get(serverName);
            if (connections != null) {
                numberOfClients.addAndGet(-connections);
            }
            numberOfClients.addAndGet(value);
            serverNameToConnections.put(serverName, value);
        } else if ("messages".equals(op)) {
            messages.increment(value);
        }
    }

    @Override
    public String toString() {
        return "Metric{" +
                "serverNameToConnections=" + serverNameToConnections +
                ", numberOfClients=" + numberOfClients +
                ", messages=" + messages +
                ", application='" + application + '\'' +
                '}';
    }
}
