package com.hls.streaming.infrastructure.config.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

@Mapper(config = CentralConfig.class)
public interface CommonMapper {

    @Named("bigDecimalToString")
    default String bigDecimalToString(BigDecimal decimal) {
        return decimal != null ? decimal.toPlainString() : null;
    }

    @Named("stringToBigDecimal")
    default BigDecimal stringToBigDecimal(String value) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Named("instantToTimestamp")
    default Timestamp mapToTimestamp(Instant source) {
        return source != null ? Timestamp.from(source) : null;
    }
}
