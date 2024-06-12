package io.confluent.devx.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record UserDeviceDetails(String userId, String userEmail, String userName, String deviceId, String deviceType) {
}
