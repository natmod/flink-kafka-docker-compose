package com.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.Properties;
import java.util.Random;

public class RandomWordKafkaReverser {

    private static final String[] WORDS = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"};
    private static final String INPUT_TOPIC = "input-topic";
    private static final String OUTPUT_TOPIC = "output-topic";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Create Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setTopics(INPUT_TOPIC)
                .setGroupId("flink-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Create Kafka sink
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(OUTPUT_TOPIC)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        // Generate random words and write to Kafka
        DataStream<String> inputStream = env.addSource(new RandomWordSource());
        inputStream.sinkTo(KafkaSink.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(INPUT_TOPIC)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build());

        // Read from Kafka, reverse the words, and write back to Kafka
        DataStream<String> outputStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .map(word -> new StringBuilder(word).reverse().toString());

        outputStream.sinkTo(sink);

        // Execute the Flink job
        env.execute("Random Word Kafka Reverser");
    }

    // Custom source to generate random words
    public static class RandomWordSource implements SourceFunction<String> {
        private volatile boolean isRunning = true;
        private final Random random = new Random();

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (isRunning) {
                ctx.collect(WORDS[random.nextInt(WORDS.length)]);
                Thread.sleep(1000); // Generate a word every second
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
    }
}