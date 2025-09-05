package com.wms.applicationInfra.config;

import com.wms.applicationInfra.idnameMapCashing.concrete.LocationCacheManager;
import com.wms.applicationInfra.idnameMapCashing.concrete.UserInfoCacheManager;
import com.wms.applicationInfra.idnameMapCashing.concrete.WareCacheManager;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.applicationInfra.util.OptimisticLockRetryUtil;
import com.wms.stock.application.StockCacheService;
import com.wms.stock.application.TaskEventNotifier;
import com.wms.stock.domain.event.StockEventStreamPublisher;
import com.wms.userInfo.application.JwtProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 테스트용 공통 설정
 * @WebMvcTest에서 필요한 빈들을 MockBean으로 등록
 */
@TestConfiguration
public class TestConfig {

    // Repository MockBeans
    @MockBean
    private LocationRepository locationRepository;

    @MockBean
    private UserInfoRepository userInfoRepository;

    @MockBean
    private WareRepository wareRepository;

    @MockBean
    private StockRepository stockRepository;

    @MockBean
    private StockDailySnapshotRepository stockDailySnapshotRepository;

    @MockBean
    private LogisticTaskRepository logisticTaskRepository;

    @MockBean
    private LogisticTemplateRepository logisticTemplateRepository;

    @MockBean
    private LocationConnectionRepository locationConnectionRepository;

    // Cache Manager MockBeans
    @MockBean
    private LocationCacheManager locationCacheManager;

    @MockBean
    private UserInfoCacheManager userInfoCacheManager;

    @MockBean
    private WareCacheManager wareCacheManager;

    // Service MockBeans (필요한 경우)
    @MockBean
    private StockCacheService stockCacheService;

    @MockBean
    private TaskEventNotifier taskEventNotifier;

    @MockBean
    private StockEventStreamPublisher stockEventStreamPublisher;

    // Utility MockBeans
    @MockBean
    private OptimisticLockRetryUtil optimisticLockRetryUtil;

    // JWT 관련 MockBeans (Spring Security 테스트를 위해 필요)
    @MockBean
    private JwtProvider jwtProvider;
}
