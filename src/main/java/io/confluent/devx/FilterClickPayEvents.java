package io.confluent.devx;

import io.confluent.devx.model.CampaignOuterClass;
import io.confluent.devx.model.ClickOuterClass;
import io.confluent.devx.model.Matched;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class FilterClickPayEvents {

    private static final Logger LOG = LoggerFactory.getLogger(FilterClickPayEvents.class);

    public static final String CLICKS_INPUT_TOPIC = "clicks-input";
    public static final String CAMPAIGN_INPUT_TOPIC = "campaigns-input";
    public static final String OUTPUT_TOPIC = "clicks-output";

    private Serde<ClickOuterClass.Click> clickSerde;
    private Serde<CampaignOuterClass.Campaign> campaignSerde;
    private Serde<Matched.MatchedClick> matchedClickSerde;

    public FilterClickPayEvents(Serde<ClickOuterClass.Click> clickSerde,
                                Serde<CampaignOuterClass.Campaign> campaignSerde,
                                Serde<Matched.MatchedClick> matchedClickSerde) {
        this.clickSerde = clickSerde;
        this.campaignSerde = campaignSerde;
        this.matchedClickSerde = matchedClickSerde;
    }

    public Topology buildTopology(Properties props) {

        final StreamsBuilder builder = new StreamsBuilder();

        KTable<String, CampaignOuterClass.Campaign> cpcCampaigns = builder.table(CAMPAIGN_INPUT_TOPIC,
                        Consumed.with(Serdes.String(), campaignSerde))
                .filter((k, v) -> "CPC".equals(v.getCostType()));

        KStream<String, ClickOuterClass.Click> rekeyedClicks = builder.stream(CLICKS_INPUT_TOPIC,
                Consumed.with(Serdes.String(), clickSerde))
                .peek((k,v) -> LOG.warn("key {}, value {}", k, v))
                .map((k, v) -> new KeyValue<>(v.getCampaignId(), v));
        rekeyedClicks.to("rekeyedClicks", Produced.with(Serdes.String(), clickSerde));

        rekeyedClicks.join(cpcCampaigns, (click, campaign) -> Matched.MatchedClick.newBuilder()
                        .setCampaign(campaign)
                        .setClick(click).build())
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), matchedClickSerde));

        return builder.build(props);
    }
}
