package io.confluent.devx.domain;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder(toBuilder = true)
@Value
@Jacksonized
public class User {
    private String id;
    private String name;
    private String email;
}
