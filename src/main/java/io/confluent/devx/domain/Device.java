package io.confluent.devx.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder(toBuilder = true)
@Value
@Jacksonized
public class Device {
    private String id;
    private String type;
    private String userId;
}
