package io.confluent.devx;

import io.confluent.devx.domain.Device;
import io.confluent.devx.domain.User;
import io.confluent.devx.domain.UserDeviceDetails;
import io.confluent.devx.serilaization.JsonSerdes;
import net.jqwik.api.*;
import net.jqwik.web.api.EmailArbitrary;
import net.jqwik.web.api.Web;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;

import java.util.*;
import java.util.stream.Collectors;

import static io.confluent.devx.DeviceUserEnricher.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeviceUserEnricherTest {

    @Property(tries = 50, edgeCases = EdgeCasesMode.FIRST)
    void testMatch(@ForAll("userArbitrary") User user, @ForAll("deviceArbitrary") Device device) {

        final String matchingUserId = UUID.randomUUID().toString();

        User inputUser = user.toBuilder()
                .id(matchingUserId)
                .build();
        Device inputDevice = device.toBuilder()
                .userId(matchingUserId)
                .build();

        Properties props = new Properties() {{
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        }};

        JsonSerdes<User> userJsonSerdes = new JsonSerdes<>(User.class);
        JsonSerdes<Device> deviceJsonSerdes = new JsonSerdes<>(Device.class);
        JsonSerdes<UserDeviceDetails> userDeviceDetailsJsonSerdes = new JsonSerdes<>(UserDeviceDetails.class);


        Topology topology = new DeviceUserEnricher(userJsonSerdes, deviceJsonSerdes, userDeviceDetailsJsonSerdes)
                .buildTopology(props);

        try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, props)) {
            TestInputTopic<String, User> userTestInputTopic = testDriver.createInputTopic(USERS_TOPIC,
                    Serdes.String().serializer(), userJsonSerdes.serializer());
            TestInputTopic<String, Device> deviceTestInputTopic = testDriver.createInputTopic(DEVICES_TOPIC,
                    Serdes.String().serializer(), deviceJsonSerdes.serializer());

            TestOutputTopic<String, UserDeviceDetails> outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC,
                    Serdes.String().deserializer(), userDeviceDetailsJsonSerdes.deserializer());

            userTestInputTopic.pipeInput(inputUser.getId(), inputUser);
            deviceTestInputTopic.pipeInput(inputDevice.getId(), inputDevice);

            List<UserDeviceDetails> output = outputTopic.readValuesToList()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(ud -> ud.userId().equals(matchingUserId))
                    .collect(Collectors.toUnmodifiableList());

            assertFalse(output.isEmpty());
        }
    }


    @Provide
    public Arbitrary<User> userArbitrary() {
        Arbitrary<String> idArb = Arbitraries.strings().alpha().ofLength(20);
        Arbitrary<String> nameArb = Arbitraries.strings().alpha().ofLength(10);
        EmailArbitrary emailArb = Web.emails();

        return Combinators.combine(idArb, nameArb, emailArb).as((id, name, email) ->
                User.builder()
                        .id(id)
                        .name(name)
                        .email(email)
                        .build());
    }

    private static final List<String> MOBILE_DEVICES = Arrays.asList(
            "iPhone",
            "Galaxy",
            "Pixel",
            "OnePlus",
            "Xperia",
            "Nokia",
            "Huawei",
            "Motorola"
    );

    @Provide
    public Arbitrary<Device> deviceArbitrary() {

        Arbitrary<String> idArb = Arbitraries.strings().alpha().ofLength(20);
        Arbitrary<String> typeArb = Arbitraries.of(MOBILE_DEVICES);
        Arbitrary<String> uidArb = Arbitraries.strings().alpha().ofLength(20);

        return Combinators.combine(idArb, typeArb, uidArb).as((id, type, uid) ->
                Device.builder()
                        .id(id)
                        .type(type)
                        .userId(uid)
                        .build()
        );
    }
}