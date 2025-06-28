package com.wms.logisticTemplate.interfaces;

import com.wms.logisticTemplate.application.LogisticTemplateService;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/logistic-templates")
@Tag(name = "물류 템플릿 관리", description = "물류 템플릿 관리 API")
public class LogisticTemplateController {

	private final LogisticTemplateService logisticTemplateService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "새 물류 템플릿 생성", description = "제공된 정보로 새로운 물류 템플릿을 생성합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "물류 템플릿 생성 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	public LogisticTemplateDTO.Res create(@Valid @RequestBody LogisticTemplateDTO.CreateReq request) {
		LogisticTemplate template = logisticTemplateService.create(request);
		return new LogisticTemplateDTO.Res(template);
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "물류 템플릿 수정", description = "ID로 물류 템플릿을 수정합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "물류 템플릿 수정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "404", description = "물류 템플릿을 찾을 수 없음")
	})
	public LogisticTemplateDTO.Res update(
			@Parameter(description = "물류 템플릿 ID") @PathVariable Long id, 
			@Valid @RequestBody LogisticTemplateDTO.UpdateReq request) {
		LogisticTemplate updated = logisticTemplateService.update(id, request);
		return new LogisticTemplateDTO.Res(updated);
	}

	@GetMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "물류 템플릿 조회", description = "ID로 물류 템플릿을 가져옵니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "물류 템플릿 조회 성공"),
			@ApiResponse(responseCode = "404", description = "물류 템플릿을 찾을 수 없음")
	})
	public LogisticTemplateDTO.Res getById(@Parameter(description = "물류 템플릿 ID") @PathVariable Long id) {
		return new LogisticTemplateDTO.Res(logisticTemplateService.findById(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "물류 템플릿 삭제", description = "ID로 물류 템플릿을 삭제합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "물류 템플릿 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "물류 템플릿을 찾을 수 없음")
	})
	public void delete(@Parameter(description = "물류 템플릿 ID") @PathVariable Long id) {
		logisticTemplateService.delete(id);
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "페이지별 전체 물류 템플릿 조회", description = "페이징을 적용하여 모든 물류 템플릿을 조회합니다")
	@ApiResponse(responseCode = "200", description = "물류 템플릿 목록 조회 성공")
	public Page<LogisticTemplateDTO.Res> getAllTemplates(Pageable pageable) {
		return logisticTemplateService.getTemplates(pageable)
				.map(LogisticTemplateDTO.Res::new);
	}

	@GetMapping("/type")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "타입별 물류 템플릿 조회", description = "특정 타입의 물류 템플릿을 조회합니다")
	@ApiResponse(responseCode = "200", description = "타입별 물류 템플릿 조회 성공")
	public Page<LogisticTemplateDTO.Res> getTemplatesByType(
			@Parameter(description = "물류 타입") @RequestParam LogisticType type, 
			Pageable pageable) {
		return logisticTemplateService.getTemplatesByType(type, pageable)
				.map(LogisticTemplateDTO.Res::new);
	}

	@GetMapping("/ware")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "물품별 물류 템플릿 조회", description = "물품 ID로 물류 템플릿을 조회합니다")
	@ApiResponse(responseCode = "200", description = "물품별 물류 템플릿 조회 성공")
	public Page<LogisticTemplateDTO.Res> getTemplatesByWareId(
			@Parameter(description = "물품 ID") @RequestParam Long wareId, 
			Pageable pageable) {
		return logisticTemplateService.getTemplatesByWareId(wareId, pageable)
				.map(LogisticTemplateDTO.Res::new);
	}
}
