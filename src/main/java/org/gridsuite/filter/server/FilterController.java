/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.gridsuite.filter.server.dto.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + FilterApi.API_VERSION)
@Api(value = "Filters server")
@ComponentScan(basePackageClasses = FilterService.class)
public class FilterController {

    private final FilterService service;

    public FilterController(FilterService service) {
        this.service = service;
    }

    @GetMapping(value = "filters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all filters", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All filters")})
    public ResponseEntity<List<FilterAttributes>> getFilters() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getFilters());
    }

    @GetMapping(value = "filters/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get filter by name", response = AbstractFilter.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filter"),
        @ApiResponse(code = 404, message = "The script contingency list does not exists")})
    public ResponseEntity<AbstractFilter> getFilter(@PathVariable("name") String name) {
        return service.getFilter(name).map(filter -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(filter))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "filters/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a filter", response = AbstractFilter.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filters have been successfully created")})
    public void createFilter(@RequestBody(required = true) AbstractFilter filter) {
        service.createFilter(filter);
    }

    @DeleteMapping(value = "filters/{name}")
    @ApiOperation(value = "delete the filter")
    @ApiResponse(code = 200, message = "The filter has been deleted")
    public ResponseEntity<Void> deleteFilter(@PathVariable("name") String name) {
        service.deleteFilter(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "filters/{name}/rename")
    @ApiOperation(value = "Rename filter by name")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filter has been renamed"),
        @ApiResponse(code = 404, message = "The filter does not exists")})
    public void renameFilter(@PathVariable("name") String name,
                             @RequestBody String newName) {
        service.renameFilter(name, newName);
    }
}
