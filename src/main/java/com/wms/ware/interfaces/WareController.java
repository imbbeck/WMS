package com.wms.ware.interfaces;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.ware.application.WareService;
import com.wms.ware.dto.WareDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "Ware Management", description = "APIs for managing Ware")
public class WareController {

    private final WareService wareService;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public WareDTO.Res createWare(@Valid @RequestBody WareDTO.CreateReq request) {
        return new WareDTO.Res(wareService.createWare(request));
    }

    @GetMapping("/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    public WareDTO.Res getWare(@PathVariable Long id) {
        return new WareDTO.Res(wareService.getWare(id));
    }

    @GetMapping
    @ResponseStatus(value = HttpStatus.OK)
    public List<WareDTO.Res> getWares() {
        return wareService.getWares().stream()
                .map(WareDTO.Res::new)
                .toList();
    }

    @GetMapping("/type")
    @ResponseStatus(value = HttpStatus.OK)
    public List<WareDTO.Res> getWaresByType(@RequestParam(value = "type") String type) {
        return wareService.getWaresByType(type).stream()
                .map(WareDTO.Res::new)
                .toList();
    }

    @PutMapping("/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    public WareDTO.Res updateWare(
            @PathVariable Long id,
            @Valid @RequestBody WareDTO.UpdateReq request) {
        return new WareDTO.Res(wareService.updateWare(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteWare(@PathVariable Long id) {
        wareService.deleteWare(id);
    }

    private final DomainCacheManager<Long, String> wareCacheManager;

    @GetMapping("/id_name_pair")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get ware ID-name pairs", description = "Retrieves a map of ware IDs to ware names for reference")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved ware ID-name pairs")
    public Map<Long, String> getIdNamePair() {
        return  wareCacheManager.getIdNamePair();
    }
} 