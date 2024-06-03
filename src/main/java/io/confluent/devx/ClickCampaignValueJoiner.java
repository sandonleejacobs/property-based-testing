package io.confluent.devx;

import com.google.protobuf.Timestamp;
import io.confluent.devx.model.CampaignOuterClass;
import io.confluent.devx.model.ClickOuterClass;
import io.confluent.devx.model.Matched;
import org.apache.kafka.streams.kstream.ValueJoiner;

public class ClickCampaignValueJoiner implements ValueJoiner<ClickOuterClass.Click, CampaignOuterClass.Campaign, Matched.MatchedClick> {

    @Override
    public Matched.MatchedClick apply(ClickOuterClass.Click click, CampaignOuterClass.Campaign campaign) {
        return Matched.MatchedClick.newBuilder()
                .setMatchTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build())
                .setClick(click)
                .setCampaign(campaign)
                .build();
    }
}
