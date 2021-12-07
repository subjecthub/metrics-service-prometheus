package com.migratorydata.sandbox.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {

    private static Properties configProperties = null;
    private static String topicStats = "s";

    static {
        boolean loadConfig = false;

        if (loadConfig == false) {
            try (InputStream input = new FileInputStream("./config/config.properties")) {
                System.out.println("load from ./config/config.properties");
                configProperties = new Properties();
                configProperties.load(input);
                loadConfig = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (loadConfig == false) {
            try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
                System.out.println("load from resources = config.properties");
                configProperties = new Properties();
                configProperties.load(input);
                loadConfig = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (loadConfig) {
            topicStats = configProperties.getProperty("topic.stats");
        }
    }


    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        Metrics metrics = new Metrics(prometheusRegistry);

        Consumer consumer = new Consumer(configProperties, topicStats, metrics);
        consumer.begin();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
