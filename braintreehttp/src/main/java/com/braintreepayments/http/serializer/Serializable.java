package com.braintreepayments.http.serializer;

import java.util.Map;

public interface Serializable {
    void serialize(Map<String, Object> serialized);
}
