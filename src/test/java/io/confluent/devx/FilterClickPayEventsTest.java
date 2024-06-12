package io.confluent.devx;

import com.google.protobuf.Timestamp;
import io.confluent.devx.kafka.MyProtobufSerdes;
import io.confluent.devx.model.CampaignOuterClass;
import io.confluent.devx.model.ClickOuterClass;
import io.confluent.devx.model.Matched;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


public class FilterClickPayEventsTest {

    private Serde<ClickOuterClass.Click> clickSerde;
    private Serde<CampaignOuterClass.Campaign> campaignSerde;
    private Serde<Matched.MatchedClick> matchedClickSerde;

    private Topology topology;
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, ClickOuterClass.Click> clickTestInputTopic;
    private TestInputTopic<String, CampaignOuterClass.Campaign> campaignTestInputTopic;
    private TestOutputTopic<String, Matched.MatchedClick> matchedClickTestOutputTopic;

    @BeforeProperty
    void setup() {

        clickSerde = MyProtobufSerdes.serde(ClickOuterClass.Click.newBuilder());
        campaignSerde = MyProtobufSerdes.serde(CampaignOuterClass.Campaign.newBuilder());
        matchedClickSerde = MyProtobufSerdes.serde(Matched.MatchedClick.newBuilder());

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

    @AfterProperty
    void teardown() {
        if (testDriver != null)
            testDriver.close();
    }

    @Property
    void filterEvents(@ForAll("clickArbitrary") ClickOuterClass.Click click,
                      @ForAll("campaignArbitrary") CampaignOuterClass.Campaign campaign) {
        final UUID campaignId = UUID.randomUUID();
        CampaignOuterClass.Campaign campaignInput = CampaignOuterClass.Campaign.newBuilder(campaign)
                .setId(campaignId.toString()).build();
        ClickOuterClass.Click clickInput = ClickOuterClass.Click.newBuilder(click)
                .setCampaignId(campaignId.toString()).build();

        campaignTestInputTopic.pipeInput(campaignInput.getId(), campaignInput);
        clickTestInputTopic.pipeInput(clickInput.getId(), clickInput);

        List<Matched.MatchedClick> output = matchedClickTestOutputTopic.readValuesToList()
                .stream()
                .filter(Objects::nonNull)
                .filter(m -> campaignId.toString().equals(m.getCampaign().getId()))
                .collect(Collectors.toList());

        assertFalse(output.isEmpty());
    }

    @Provide
    public Arbitrary<ClickOuterClass.Click> clickArbitrary() {

        Arbitrary<UUID> id = Arbitraries.forType(UUID.class);
        Arbitrary<UUID> deviceId = Arbitraries.forType(UUID.class);
        Arbitrary<UUID> campaignId = Arbitraries.forType(UUID.class);
        Arbitrary<UUID> impressionId = Arbitraries.forType(UUID.class);

        return id.flatMap(i ->
                deviceId.flatMap(d ->
                        campaignId.flatMap(c ->
                                impressionId.map(imp ->
                                        ClickOuterClass.Click.newBuilder()
                                                .setId(id.toString())
                                                .setDeviceId(d.toString())
                                                .setCampaignId(c.toString())
                                                .setImpressionId(imp.toString())
                                                .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build())
                                                .build()

                                )))
        );
    }

    @Provide
    public Arbitrary<CampaignOuterClass.Campaign> campaignArbitrary() {

        Arbitrary<UUID> id = Arbitraries.forType(UUID.class);
        Arbitrary<UUID> adId = Arbitraries.forType(UUID.class);
        Arbitrary<UUID> productId = Arbitraries.forType(UUID.class);
        Arbitrary<Double> cost = Arbitraries.doubles().between(0.0, 1.0).ofScale(2);

        return id.flatMap(i ->
                adId.flatMap(aid ->
                        productId.flatMap(pid ->
                                cost.map(c ->
                                        CampaignOuterClass.Campaign.newBuilder()
                                                .setId(i.toString())
                                                .setAdvertiserId(aid.toString())
                                                .setProductId(pid.toString())
                                                .setCost(c)
                                                .setCostType("CPC")
                                                .build()
                                ))));
    }
}