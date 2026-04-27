package com.hls.streaming.features.mapper;

import org.mapstruct.*;

@MapperConfig(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_ALL_FROM_CONFIG,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface CentralConfig {}
