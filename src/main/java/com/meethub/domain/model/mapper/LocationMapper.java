package com.meethub.domain.model.mapper;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "meetings", ignore = true)
    Location toEntity(CreateLocationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Location location, UpdateLocationRequest request);

    LocationResponse toResponse(Location location);

}