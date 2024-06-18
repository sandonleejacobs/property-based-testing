package io.confluent.devx.kstreams;

import io.confluent.devx.domain.Device;
import io.confluent.devx.domain.User;
import io.confluent.devx.domain.UserDeviceDetails;
import io.confluent.devx.kstreams.serialization.JsonSerdes;
import net.jqwik.api.*;
import net.jqwik.web.api.EmailArbitrary;
import net.jqwik.web.api.Web;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.confluent.devx.kstreams.DeviceUserEnricher.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based testing Kafka Streams Topology, which joins streams of data.
 * <p>
 * Because of the complexity of the topology, generating 1000 or more tests consumes a large amount of time.
 * Note that in the Property annotations on the test methods, we limit the number of tries, and run edge cases first.
 */
class DeviceUserEnricherTest {

    @Property(tries = 50, edgeCases = EdgeCasesMode.FIRST, shrinking = ShrinkingMode.BOUNDED)
    void testMatch(@ForAll("userArbitrary") User user, @ForAll("deviceArbitrary") Device device) {

        // generate a user id to use in both objects
        final String matchingUserId = UUID.randomUUID().toString();

        User inputUser = user.toBuilder()
                .id(matchingUserId)
                .build();
        Device inputDevice = device.toBuilder()
                .userId(matchingUserId)
                .build();

        final Function<TestOutputTopic<String, UserDeviceDetails>, List<UserDeviceDetails>> outputFunction = topic ->
                topic.readValuesToList()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(ud -> ud.userId().equals(matchingUserId))
                        .collect(Collectors.toUnmodifiableList());

        List<UserDeviceDetails> output = executeTopology(inputUser, inputDevice, outputFunction);
        // there should ALWAYS be a matching UserDeviceDetails record from the topology because we matched the user id values.
        assertFalse(output.isEmpty());
        assertEquals(1, output.size());
    }

    @Property(tries = 50, edgeCases = EdgeCasesMode.FIRST, shrinking = ShrinkingMode.BOUNDED)
    void testMiss(@ForAll("userArbitrary") User user, @ForAll("deviceArbitrary") Device device) {

        // generate a user ID
        final String userId = UUID.randomUUID().toString();

        // set that user ID here
        User inputUser = user.toBuilder()
                .id(userId)
                .build();
        // force a different user ID onto the device
        Device inputDevice = device.toBuilder()
                .userId(new StringBuilder(userId).reverse().toString())
                .build();

        final Function<TestOutputTopic<String, UserDeviceDetails>, List<UserDeviceDetails>> outputFunction = topic ->
                topic.readValuesToList()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(ud -> ud.userId().equals(userId))
                        .collect(Collectors.toUnmodifiableList());

        List<UserDeviceDetails> output = executeTopology(inputUser, inputDevice, outputFunction);
        assertTrue(output.isEmpty());
    }

    private List<UserDeviceDetails> executeTopology(final User inputUser, final Device inputDevice,
                                                    final Function<TestOutputTopic<String, UserDeviceDetails>, List<UserDeviceDetails>> outputFunction) {

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
            return outputFunction.apply(outputTopic);
        }
    }

    /**
     * Arbitrary generator for User objects.
     *
     * @return
     */
    @Provide
    public Arbitrary<User> userArbitrary() {
        Arbitrary<String> idArb = Arbitraries.strings().alpha().numeric().ofLength(20);
        Arbitrary<String> nameArb = Arbitraries.strings().alpha().ofLength(10);
        EmailArbitrary emailArb = Web.emails();

        return Combinators.combine(idArb, nameArb, emailArb).as((id, name, email) ->
                User.builder()
                        .id(id)
                        .name(name)
                        .email(email)
                        .build());
    }

    /**
     * list of available device types
     */
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

    /**
     * Arbitrary generator for Device objects.
     *
     * @return
     */
    @Provide
    public Arbitrary<Device> deviceArbitrary() {

        Arbitrary<String> idArb = Arbitraries.strings().alpha().numeric().ofLength(20);
        Arbitrary<String> typeArb = Arbitraries.of(MOBILE_DEVICES);
        Arbitrary<String> uidArb = Arbitraries.strings().alpha().numeric().ofLength(20);

        return Combinators.combine(idArb, typeArb, uidArb).as((id, type, uid) ->
                Device.builder()
                        .id(id)
                        .type(type)
                        .userId(uid)
                        .build()
        );
    }
}