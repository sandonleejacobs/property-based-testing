package io.confluent.devx;

import com.google.protobuf.Timestamp;
import io.confluent.devx.model.CampaignOuterClass;
import io.confluent.devx.model.ClickOuterClass;
import io.confluent.devx.model.Matched;
import io.confluent.devx.serilaization.SerdeUtil;
import io.confluent.devx.serilaization.SerializationConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class FilterClickPayEventsTest {

    private Serde<ClickOuterClass.Click> clickSerde;
    private Serde<CampaignOuterClass.Campaign> campaignSerde;
    private Serde<Matched.MatchedClick> matchedClickSerde;

    private Topology topology;
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, ClickOuterClass.Click> clickTestInputTopic;
    private TestInputTopic<String, CampaignOuterClass.Campaign> campaignTestInputTopic;
    private TestOutputTopic<String, Matched.MatchedClick> matchedClickTestOutputTopic;

    @BeforeEach
    void setup() {
        Map<String, String> serdeConfig = new HashMap<>() {{
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://my-registry");
        }};
        clickSerde = SerdeUtil.protobufSerde(ClickOuterClass.Click.class);
        clickSerde.configure(Map.of(SerializationConfig.VALUE_CLASS_NAME, ClickOuterClass.Click.class), false);
        campaignSerde = SerdeUtil.protobufSerde(CampaignOuterClass.Campaign.class);
        campaignSerde.configure(Map.of(SerializationConfig.VALUE_CLASS_NAME, CampaignOuterClass.Campaign.class), false);
        matchedClickSerde = SerdeUtil.protobufSerde(Matched.MatchedClick.class);
        matchedClickSerde.configure(Map.of(SerializationConfig.VALUE_CLASS_NAME, Matched.MatchedClick.class), false);

        Properties props = new Properties() {{
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        }};

        topology = new FilterClickPayEvents(clickSerde, campaignSerde, matchedClickSerde).buildTopology(props);
        testDriver = new TopologyTestDriver(topology, props);

        clickTestInputTopic = testDriver.createInputTopic(FilterClickPayEvents.CLICKS_INPUT_TOPIC,
                Serdes.String().serializer(), clickSerde.serializer());
        campaignTestInputTopic = testDriver.createInputTopic(FilterClickPayEvents.CAMPAIGN_INPUT_TOPIC,
                Serdes.String().serializer(), campaignSerde.serializer());
        matchedClickTestOutputTopic = testDriver.createOutputTopic(FilterClickPayEvents.OUTPUT_TOPIC,
                Serdes.String().deserializer(), matchedClickSerde.deserializer());
    }

    @AfterEach
    void teardown() {
        if (testDriver != null)
            testDriver.close();
    }

    @Test
    void filterEvents() {
        final UUID campaignId = UUID.randomUUID();
        CampaignOuterClass.Campaign campaign = campaignArbitrary(campaignId);
        ClickOuterClass.Click click = clickArbitrary(campaignId);

        clickTestInputTopic.pipeInput(click.getId(), click);
        campaignTestInputTopic.pipeInput(campaign.getId(), campaign);

        List<Matched.MatchedClick> output = matchedClickTestOutputTopic.readValuesToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertEquals(1, output.size());
    }


    public ClickOuterClass.Click clickArbitrary(final UUID campaignId) {

        return ClickOuterClass.Click.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setDeviceId(UUID.randomUUID().toString())
                .setCampaignId(campaignId.toString())
                .setImpressionId(UUID.randomUUID().toString())
                .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build())
                .build();
    }


    public CampaignOuterClass.Campaign campaignArbitrary(final UUID campaignId) {
        return CampaignOuterClass.Campaign.newBuilder()
                .setId(campaignId.toString())
                .setAdvertiserId(UUID.randomUUID().toString())
                .setProductId(UUID.randomUUID().toString())
                .setCost(0.25)
                .setCostType("CPC")
                .build();
    }

}
