package com.hls.streaming.security.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class MapUtils {

    public static Map<String, Object> stripNullValue(Map<String, Object> convertValue) {
        convertValue.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet())
                .forEach(convertValue::remove);
        return convertValue;
    }
}
