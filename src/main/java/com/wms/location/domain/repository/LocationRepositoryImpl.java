package com.wms.location.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.QLocation;
import com.wms.location.domain.model.QLocationConnection;
import com.wms.location.dto.LocationConnectionDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocationRepositoryImpl implements LocationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<LocationWithConnectionsDTO> findLocationWithConnections(Long id) {
		QLocation location = QLocation.location;
		QLocationConnection connection = QLocationConnection.locationConnection;

		Location loc = queryFactory.selectFrom(location)
				.where(location.id.eq(id))
				.fetchOne();

		if (loc == null) {
			return Optional.empty();
		}

		List<LocationConnection> connections = queryFactory
				.selectFrom(connection)
				.where(connection.locationAId.eq(id).or(connection.locationBId.eq(id)))
				.fetch();

		List<LocationConnectionDTO.Res> connectionDTOs = connections.stream()
				.map(conn -> new LocationConnectionDTO.Res(
						conn.getId(),
						conn.getLocationAId(),
						conn.getLocationBId(),
						conn.getTrt()
				))
				.collect(Collectors.toList());

		return Optional.of(
				new LocationWithConnectionsDTO(
						loc.getId(),
						loc.getName(),
						loc.getType(),
						loc.getCapacity(),
						loc.getCreatedAt(),
						loc.getUpdatedAt(),
						connectionDTOs
				)
		);
	}
}
