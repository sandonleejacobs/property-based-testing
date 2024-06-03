package io.confluent.devx.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

public class MyProtobufSerdes {

    public static class ProtobufSerializer<T extends Message> implements Serializer<T> {
        @Override
        public byte[] serialize(String topic, T data) {
            return data == null ? null : data.toByteArray();
        }
    }

    public static class ProtobufDeserializer<T extends Message> implements Deserializer<T> {
        private final Message.Builder builder;

        public ProtobufDeserializer(Message.Builder builder) {
            this.builder = builder;
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                builder.clear();
                builder.mergeFrom(data);
                return (T) builder.build();
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T extends Message> Serde<T> serde(Message.Builder builder) {
        return Serdes.serdeFrom(new ProtobufSerializer<>(), new ProtobufDeserializer<>(builder));
    }
}
