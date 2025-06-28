package com.wms.ware.interfaces;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.ware.application.WareService;
import com.wms.ware.dto.WareDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wares")
@RequiredArgsConstructor
@Tag(name = "물품 관리", description = "물품 정보 관리 API")
public class WareController {

    private final WareService wareService;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    @Operation(summary = "물품 생성", description = "새로운 물품을 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "물품 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public WareDTO.Res createWare(@Valid @RequestBody WareDTO.CreateReq request) {
        return new WareDTO.Res(wareService.createWare(request));
    }

    @GetMapping("/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "물품 단건 조회", description = "ID로 특정 물품을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물품 조회 성공"),
            @ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
    })
    public WareDTO.Res getWare(@Parameter(description = "물품 ID") @PathVariable Long id) {
        return new WareDTO.Res(wareService.getWareById(id));
    }

    @GetMapping
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "전체 물품 조회", description = "모든 물품 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "물품 목록 조회 성공")
    public List<WareDTO.Res> getWares() {
        return wareService.getAllWares().stream()
                .map(WareDTO.Res::new)
                .toList();
    }

    @GetMapping("/type")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "타입별 물품 조회", description = "특정 타입의 물품들을 조회합니다")
    @ApiResponse(responseCode = "200", description = "타입별 물품 조회 성공")
    public List<WareDTO.Res> getWaresByType(
            @Parameter(description = "물품 타입") @RequestParam(value = "type") String type) {
        return wareService.getWaresByType(type).stream()
                .map(WareDTO.Res::new)
                .toList();
    }

    @PutMapping("/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "물품 수정", description = "기존 물품 정보를 수정합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물품 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
    })
    public WareDTO.Res updateWare(
            @Parameter(description = "물품 ID") @PathVariable Long id,
            @Valid @RequestBody WareDTO.UpdateReq request) {
        return new WareDTO.Res(wareService.updateWare(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Operation(summary = "물품 삭제", description = "기존 물품을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "물품 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
    })
    public void deleteWare(@Parameter(description = "물품 ID") @PathVariable Long id) {
        wareService.deleteWare(id);
    }

    private final DomainCacheManager<Long, String> wareCacheManager;

    @GetMapping("/id_name_pair")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "물품 ID-이름 쌍 조회", description = "참조용 물품 ID와 이름의 매핑 정보를 조회합니다")
    @ApiResponse(responseCode = "200", description = "물품 ID-이름 쌍 조회 성공")
    public Map<Long, String> getIdNamePair() {
        return  wareCacheManager.getIdNamePair();
    }
} 