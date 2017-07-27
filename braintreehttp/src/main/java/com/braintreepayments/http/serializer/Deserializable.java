package com.braintreepayments.http.serializer;

import java.util.Map;

public interface Deserializable {
    void deserialize(Map<String, Object> fields);
}
