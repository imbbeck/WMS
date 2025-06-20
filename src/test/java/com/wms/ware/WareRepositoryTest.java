package com.wms.ware;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class WareRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WareRepository wareRepository;

    @Test
    @DisplayName("물품을 저장하고 조회할 수 있다.")
    void saveAndFind_Success() {
        // given
        Ware ware = Ware.builder().name("A").type("부품").paletteUnit(10).build();
        entityManager.persistAndFlush(ware);

        // when
        Ware found = wareRepository.findById(ware.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("A");
    }

    @Test
    @DisplayName("이름 중복 여부를 확인할 수 있다.")
    void existsByName_Success() {
        // given
        Ware ware = Ware.builder().name("중복체크").type("부품").paletteUnit(5).build();
        entityManager.persistAndFlush(ware);

        // when
        boolean exists = wareRepository.existsByName("중복체크");
        boolean notExists = wareRepository.existsByName("없는이름");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("타입별로 물품 목록을 조회할 수 있다.")
    void findAllByType_Success() {
        // given
        entityManager.persist(Ware.builder().name("A").type("부품").paletteUnit(1).build());
        entityManager.persist(Ware.builder().name("B").type("전자제품").paletteUnit(2).build());
        entityManager.flush();

        // when
        List<Ware> parts = wareRepository.findAllByType("부품");
        List<Ware> electronics = wareRepository.findAllByType("전자제품");

        // then
        assertThat(parts).extracting("type").containsOnly("부품");
        assertThat(electronics).extracting("type").containsOnly("전자제품");
    }
} 