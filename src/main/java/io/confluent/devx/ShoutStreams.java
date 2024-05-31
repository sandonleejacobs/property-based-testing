package io.confluent.devx;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ShoutStreams {

    private static final Logger LOG = LoggerFactory.getLogger(ShoutStreams.class);

    public static final String INPUT_TOPIC = "shout-input";
    public static final String OUTPUT_TOPIC = "shout-output";

    public Topology buildTopology(final Properties props) {
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> inputStream = builder.stream(INPUT_TOPIC);

        inputStream.peek((k, v) -> LOG.debug("input event -> key: {}, value {}", k, v))
                .mapValues(v -> v.toUpperCase())
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        return builder.build(props);
    }
}
