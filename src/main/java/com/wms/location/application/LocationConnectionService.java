package com.wms.location.application;

import java.util.List;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.exception.LocationConnectException;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationConnectionDTO;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationConnectionService {

	private final LocationConnectionRepository connectionRepository;
	private final LocationRepository locationRepository;

	// 연결 생성
	@Transactional
	public LocationConnection createConnection(LocationConnectionDTO.CreateReq request) {
		// Location 존재 여부 확인
		Long locationId1 = request.getLocationId1();
		Long locationId2 = request.getLocationId2();

		validateLocationExists(locationId1);
		validateLocationExists(locationId2);

		Long smallestId = Math.min(locationId1, locationId2);
		Long biggestId = Math.max(locationId1, locationId2);

		// 이미 연결이 있는지 확인
		if (connectionRepository.findByLocationIds(smallestId, biggestId).isPresent()) {
			throw LocationConnectException.duplicate(FieldEnum.LOCATION_CONNETCTION, smallestId + " - " + biggestId);
		}

		LocationConnection connection = request.toEntity();
		return connectionRepository.save(connection);
	}

	// 연결 상세 조회
	public LocationConnection getConnection(Long connectionId) {
		return getConnectionEntity(connectionId);
	}

	// 모든 연결 조회
	public List<LocationConnection> getAllConnections() {
		return connectionRepository.findAll();
	}

	// 특정 Location의 연결 정보 조회
	public List<LocationConnection> getConnectionsByLocationId(Long locationId) {
		validateLocationExists(locationId);
		return connectionRepository.findAllByLocationId(locationId);
	}

	// 연결 수정 (이동 시간 변경)
	@Transactional
	public LocationConnection updateConnection(Long connectionId, LocationConnectionDTO.UpdateReq request) {
		LocationConnection connection = getConnectionEntity(connectionId);
		connection.updateDuration(request.getTrt());
		return connection;
	}

	// 연결 삭제
	@Transactional
	public void deleteConnection(Long connectionId) {
		LocationConnection connection = getConnectionEntity(connectionId);
		connectionRepository.delete(connection);
	}

	// 두 Location 간 연결 조회
/*	public LocationConnection getConnectionByLocations(Long locationId1, Long locationId2) {
		validateLocationExists(locationId1);
		validateLocationExists(locationId2);

		return connectionRepository.findByLocationIds(locationId1, locationId2)
				.orElseThrow(() -> new IllegalArgumentException("두 장소 간 연결이 존재하지 않습니다."));
	}*/

	// 특정 Location의 연결 개수 조회
	public long getConnectionCount(Long locationId) {
		validateLocationExists(locationId);
		return connectionRepository.countByLocationId(locationId);
	}

	// 연결 존재 여부 확인
	public boolean isConnected(Long locationId1, Long locationId2) {
		return connectionRepository.findByLocationIds(locationId1, locationId2).isPresent();
	}

	// 특정 Location들과 관련된 모든 연결 조회
	public List<LocationConnection> getConnectionsByLocationIds(List<Long> locationIds) {
		return connectionRepository.findAllByLocationIds(locationIds);
	}

	// Helper Methods

	private LocationConnection getConnectionEntity(Long connectionId) {
		return connectionRepository.findById(connectionId)
				.orElseThrow(() -> LocationConnectException.notFound(connectionId));
	}

	private void validateLocationExists(Long locationId) {
		if (!locationRepository.existsById(locationId)) {
			throw LocationException.notFound(locationId);
		}
	}

	//	Location 삭제 시 LocationDeletedEvent 발행
	//	이벤트 리스너가 LocationConnection 삭제 처리
	// 같은 트랜잭션에서 실행됨 - 실패 시 전체 롤백!
	@EventListener
	public void onLocationDeleted(LocationDeletedEvent event) {
		Long locationId = event.getId();
		connectionRepository.deleteByLocationId(locationId);
	}
}

