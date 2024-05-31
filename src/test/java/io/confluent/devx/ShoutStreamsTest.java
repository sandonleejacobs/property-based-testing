package io.confluent.devx;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;

import java.util.Properties;
import java.util.UUID;

import static io.confluent.devx.ShoutStreams.INPUT_TOPIC;
import static io.confluent.devx.ShoutStreams.OUTPUT_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShoutStreamsTest {

    private Topology topology;
    private TopologyTestDriver testDriver;

    Properties props = new Properties() {{
        put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    }};

    @Property
    void testShout(@ForAll("onlyAlphaStrings") String input) {
        topology = new ShoutStreams().buildTopology(props);
        testDriver = new TopologyTestDriver(topology, props);

        TestInputTopic<String, String> testInputTopic = testDriver.createInputTopic(INPUT_TOPIC,
                Serdes.String().serializer(), Serdes.String().serializer());

        TestOutputTopic<String, String> testOutputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC,
                Serdes.String().deserializer(), Serdes.String().deserializer());

        testInputTopic.pipeInput(UUID.randomUUID().toString(), input.toLowerCase());
        final String actualOutput = testOutputTopic.readValue();

        assertEquals(input.toUpperCase(), actualOutput);
    }

    @Provide
    public Arbitrary<String> onlyAlphaStrings() {
        return Arbitraries.strings().alpha()
                .ofMaxLength(1)
                .ofMaxLength(100);
    }

    @AfterProperty
    void teardown() {
        if (testDriver != null)
            testDriver.close();
    }
}
