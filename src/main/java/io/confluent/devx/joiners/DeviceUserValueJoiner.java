package io.confluent.devx.joiners;

import io.confluent.devx.domain.Device;
import io.confluent.devx.domain.User;
import io.confluent.devx.domain.UserDeviceDetails;
import org.apache.kafka.streams.kstream.ValueJoiner;

public class DeviceUserValueJoiner implements ValueJoiner<Device, User, UserDeviceDetails> {

    @Override
    public UserDeviceDetails apply(final Device device, final User user) {
        return new UserDeviceDetails(user.getId(), user.getEmail(), user.getName(),
                device.getId(), device.getType());
    }
}
