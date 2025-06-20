package com.wms.logisticTemplate.interfaces;

import com.wms.logisticTemplate.application.LogisticTemplateService;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "LogisticTemplate Management", description = "APIs for managing LogisticTemplate")
public class LogisticTemplateController {

	private final LogisticTemplateService logisticTemplateService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new LogisticTemplate", description = "Creates a new logistic template with the provided details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "LogisticTemplate created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public LogisticTemplateDTO.Res create(@Valid @RequestBody LogisticTemplateDTO.CreateReq request) {
		LogisticTemplate template = logisticTemplateService.create(request);
		return new LogisticTemplateDTO.Res(template);
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Update a LogisticTemplate", description = "Updates the logistic template by ID")
	public LogisticTemplateDTO.Res update(@PathVariable Long id, @Valid @RequestBody LogisticTemplateDTO.UpdateReq request) {
		LogisticTemplate updated = logisticTemplateService.update(id, request);
		return new LogisticTemplateDTO.Res(updated);
	}

	@GetMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get a LogisticTemplate", description = "Fetches logistic template by ID")
	public LogisticTemplateDTO.Res getById(@PathVariable Long id) {
		return new LogisticTemplateDTO.Res(logisticTemplateService.findById(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a LogisticTemplate", description = "Deletes logistic template by ID")
	public void delete(@PathVariable Long id) {
		logisticTemplateService.delete(id);
	}

	// 전체 조회 (페이징)
	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get all logistic templates with pagination")
	public Page<LogisticTemplateDTO.Res> getAllTemplates(Pageable pageable) {
		return logisticTemplateService.getTemplates(pageable)
				.map(LogisticTemplateDTO.Res::new);
	}

	// 타입별 조회 (페이징)
	@GetMapping("/type")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get logistic templates by type")
	public Page<LogisticTemplateDTO.Res> getTemplatesByType(@RequestParam LogisticType type, Pageable pageable) {
		return logisticTemplateService.getTemplatesByType(type, pageable)
				.map(LogisticTemplateDTO.Res::new);
	}

	// wareId별 조회 (페이징)
	@GetMapping("/ware")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get logistic templates by ware ID")
	public Page<LogisticTemplateDTO.Res> getTemplatesByWareId(@RequestParam Long wareId, Pageable pageable) {
		return logisticTemplateService.getTemplatesByWareId(wareId, pageable)
				.map(LogisticTemplateDTO.Res::new);
	}
}
