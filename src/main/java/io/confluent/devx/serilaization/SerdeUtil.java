package io.confluent.devx.serilaization;

import com.google.protobuf.Message;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SerdeUtil {

    public static final String VALUE_CLASS_NAME = "value.class";
    public static final String KEY_CLASS_NAME = "key.class";

    public static <T extends Message> Serde<T> protobufSerde(final Class<T> theClass, final boolean isKey) {
        Serializer<T> serializer = new ProtoSerializer<>();
        Deserializer<T> deserializer = new ProtoDeserializer<>();
        Map<String, Class<T>> configs = new HashMap<>();
        configs.put(getClassConfig(isKey), theClass);
        deserializer.configure(configs,isKey);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public static <T extends Message> Serde<T> protobufSerde(final Class<T> theClass) {
        return protobufSerde(theClass, false);
    }

    private static Map<String, String> propertiesToMap(final Properties properties) {
        Map<String, String> map = new HashMap<>();
        properties.forEach((key, value) -> map.put((String)key, (String)value));
        return map;
    }

    public static String getClassConfig(boolean isKey) {
        return isKey ? KEY_CLASS_NAME : VALUE_CLASS_NAME;
    }


}