package io.confluent.devx;

import io.confluent.devx.kstreams.ShoutStreams;
import net.jqwik.api.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;

import java.util.Properties;
import java.util.UUID;

import static io.confluent.devx.kstreams.ShoutStreams.INPUT_TOPIC;
import static io.confluent.devx.kstreams.ShoutStreams.OUTPUT_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShoutStreamsTest {

    Properties props = new Properties() {{
        put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    }};

    @Property
    void testShout(@ForAll("alphaNumericStrings") String input) {
        // for later comparison, force the input to lower case
        final String lowerInput = input.toLowerCase();

        Topology topology = new ShoutStreams().buildTopology(props);
        try(TopologyTestDriver testDriver = new TopologyTestDriver(topology, props)) {
            TestInputTopic<String, String> testInputTopic = testDriver.createInputTopic(INPUT_TOPIC,
                    Serdes.String().serializer(), Serdes.String().serializer());

            TestOutputTopic<String, String> testOutputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC,
                    Serdes.String().deserializer(), Serdes.String().deserializer());

            // with a randomized key, send the lowerInput to the input topic
            testInputTopic.pipeInput(UUID.randomUUID().toString(), lowerInput);
            final String actualOutput = testOutputTopic.readValue();

            // the topology should output a string identical to the UPPER CASE of our input value.
            assertEquals(lowerInput.toUpperCase(), actualOutput);
        }
    }

    @Provide
    public Arbitrary<String> alphaNumericStrings() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMaxLength(1)
                .ofMaxLength(100);
    }

}
