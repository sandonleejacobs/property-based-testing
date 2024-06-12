package io.confluent.devx.joiners;

import io.confluent.devx.domain.Device;
import io.confluent.devx.domain.User;
import io.confluent.devx.domain.UserDeviceDetails;
import net.jqwik.api.*;
import net.jqwik.web.api.EmailArbitrary;
import net.jqwik.web.api.Web;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceUserValueJoinerTest {

    @Property
    void testJoiner(@ForAll("userArbitrary") User user, @ForAll("deviceArbitrary") Device device) {
        UserDeviceDetails results = new DeviceUserValueJoiner().apply(device, user);

        assertEquals(user.getId(), results.userId());
        assertEquals(device.getId(), results.deviceId());
        assertEquals(user.getName(), results.userName());
        assertEquals(device.getType(), results.deviceType());
        assertEquals(user.getEmail(), results.userEmail());
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