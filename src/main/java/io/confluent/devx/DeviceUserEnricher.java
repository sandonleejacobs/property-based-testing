package io.confluent.devx;

import io.confluent.devx.domain.Device;
import io.confluent.devx.domain.User;
import io.confluent.devx.domain.UserDeviceDetails;
import io.confluent.devx.joiners.DeviceUserValueJoiner;
import io.confluent.devx.serilaization.JsonSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class DeviceUserEnricher {

    private final Logger LOG = LoggerFactory.getLogger(DeviceUserEnricher.class);

    public static final String USERS_TOPIC = "users";
    public static final String DEVICES_TOPIC = "devices";

    public static final String OUTPUT_TOPIC = "user-device-matched";

    private Serde<User> userSerde;
    private Serde<Device> deviceSerde;
    private Serde<UserDeviceDetails> userDeviceDetailsSerde;

    public DeviceUserEnricher(Serde<User> userSerde, Serde<Device> deviceSerde, Serde<UserDeviceDetails> userDeviceDetailsSerde) {
        this.userSerde = userSerde;
        this.deviceSerde = deviceSerde;
        this.userDeviceDetailsSerde = userDeviceDetailsSerde;
    }

    public Topology buildTopology(final Properties props) {
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Device> devicesByUser = builder.stream(DEVICES_TOPIC,
                Consumed.with(Serdes.String(), deviceSerde))
                .peek((k, v) -> LOG.warn("device: key = {}, value = {}", k, v))
                .map((k,v) -> new KeyValue<>(v.getUserId(), v));
        devicesByUser.to("rekeyed-devices", Produced.with(Serdes.String(), deviceSerde));

        KTable<String, User> userTable = builder.table(USERS_TOPIC,
                Consumed.with(Serdes.String(), new JsonSerdes<>(User.class)));

        devicesByUser.join(userTable, new DeviceUserValueJoiner(),
                Joined.with(Serdes.String(), new JsonSerdes<>(Device.class), userSerde))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), userDeviceDetailsSerde));

        return builder.build(props);
    }

}
