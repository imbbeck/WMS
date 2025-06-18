package com.wms.location.interfaces;

import com.wms.location.application.LocationConnectionService;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.dto.LocationConnectionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/location-connections")
@RequiredArgsConstructor
public class LocationConnectionController {

    private final LocationConnectionService connectionService;

    @PostMapping
    public ResponseEntity<LocationConnection> createConnection(@RequestBody @Valid LocationConnectionDTO.CreateReq request) {
        LocationConnection createdConnection = connectionService.createConnection(request);
        return new ResponseEntity<>(createdConnection, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationConnection> getConnectionById(@PathVariable Long id) {
        LocationConnection connection = connectionService.getConnection(id);
        return ResponseEntity.ok(connection);
    }

    @GetMapping
    public ResponseEntity<List<LocationConnection>> getAllConnections() {
        List<LocationConnection> connections = connectionService.getAllConnections();
        return ResponseEntity.ok(connections);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationConnection> updateConnection(@PathVariable Long id, @RequestBody @Valid LocationConnectionDTO.UpdateReq request) {
        LocationConnection updatedConnection = connectionService.updateConnection(id, request);
        return ResponseEntity.ok(updatedConnection);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }
}