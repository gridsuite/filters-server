/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.filter.server.dto.*;
import org.gridsuite.filter.server.entities.AbstractFilterEntity;
import org.gridsuite.filter.server.repositories.BatteryFilterRepository;
import org.gridsuite.filter.server.repositories.BusBarSectionFilterRepository;
import org.gridsuite.filter.server.repositories.DanglingLineFilterRepository;
import org.gridsuite.filter.server.repositories.GeneratorFilterRepository;
import org.gridsuite.filter.server.repositories.HvdcLineFilterRepository;
import org.gridsuite.filter.server.repositories.LccConverterStationFilterRepository;
import org.gridsuite.filter.server.repositories.LineFilterRepository;
import org.gridsuite.filter.server.repositories.LoadFilterRepository;
import org.gridsuite.filter.server.repositories.ScriptFilterRepository;
import org.gridsuite.filter.server.repositories.ShuntCompensatorFilterRepository;
import org.gridsuite.filter.server.repositories.StaticVarCompensatorFilterRepository;
import org.gridsuite.filter.server.repositories.ThreeWindingsTransformerFilterRepository;
import org.gridsuite.filter.server.repositories.TwoWindingsTransformerFilterRepository;
import org.gridsuite.filter.server.repositories.VscConverterStationFilterRepository;
import org.gridsuite.filter.server.utils.EquipmentType;
import org.gridsuite.filter.server.utils.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.filter.server.AbstractFilterRepositoryProxy.WRONG_FILTER_TYPE;

/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class FilterService {

    private static final String FILTER_LIST = "Filter list ";
    private static final String NOT_FOUND = " not found";

    private final Map<String, AbstractFilterRepositoryProxy<?, ?>> filterRepositories = new HashMap<>();

    private FiltersToGroovyScript filtersToScript;

    public FilterService(FiltersToGroovyScript filtersToScript,
                         final ScriptFilterRepository scriptFiltersRepository,
                         final LineFilterRepository lineFilterRepository,
                         final GeneratorFilterRepository generatorFilterRepository,
                         final LoadFilterRepository loadFilterRepository,
                         final ShuntCompensatorFilterRepository shuntCompensatorFilterRepository,
                         final StaticVarCompensatorFilterRepository staticVarCompensatorFilterRepository,
                         final BatteryFilterRepository batteryFilterRepository,
                         final BusBarSectionFilterRepository busBarSectionFilterRepository,
                         final DanglingLineFilterRepository danglingLineFilterRepository,
                         final LccConverterStationFilterRepository lccConverterStationFilterRepository,
                         final VscConverterStationFilterRepository vscConverterStationFilterRepository,
                         final TwoWindingsTransformerFilterRepository twoWindingsTransformerFilterRepository,
                         final ThreeWindingsTransformerFilterRepository threeWindingsTransformerFilterRepository,
                         final HvdcLineFilterRepository hvdcLineFilterRepository) {
        this.filtersToScript = filtersToScript;

        filterRepositories.put(EquipmentType.LINE.name(), new LineFilterRepositoryProxy(lineFilterRepository));
        filterRepositories.put(EquipmentType.GENERATOR.name(), new GeneratorFilterRepositoryProxy(generatorFilterRepository));
        filterRepositories.put(EquipmentType.LOAD.name(), new LoadFilterRepositoryProxy(loadFilterRepository));
        filterRepositories.put(EquipmentType.SHUNT_COMPENSATOR.name(), new ShuntCompensatorFilterRepositoryProxy(shuntCompensatorFilterRepository));
        filterRepositories.put(EquipmentType.STATIC_VAR_COMPENSATOR.name(), new StaticVarCompensatorFilterRepositoryProxy(staticVarCompensatorFilterRepository));
        filterRepositories.put(EquipmentType.BATTERY.name(), new BatteryFilterRepositoryProxy(batteryFilterRepository));
        filterRepositories.put(EquipmentType.BUSBAR_SECTION.name(), new BusBarSectionFilterRepositoryProxy(busBarSectionFilterRepository));
        filterRepositories.put(EquipmentType.DANGLING_LINE.name(), new DanglingLineFilterRepositoryProxy(danglingLineFilterRepository));
        filterRepositories.put(EquipmentType.LCC_CONVERTER_STATION.name(), new LccConverterStationFilterRepositoryProxy(lccConverterStationFilterRepository));
        filterRepositories.put(EquipmentType.VSC_CONVERTER_STATION.name(), new VscConverterStationFilterRepositoryProxy(vscConverterStationFilterRepository));
        filterRepositories.put(EquipmentType.TWO_WINDINGS_TRANSFORMER.name(), new TwoWindingsTransformerFilterRepositoryProxy(twoWindingsTransformerFilterRepository));
        filterRepositories.put(EquipmentType.THREE_WINDINGS_TRANSFORMER.name(), new ThreeWindingsTransformerFilterRepositoryProxy(threeWindingsTransformerFilterRepository));
        filterRepositories.put(EquipmentType.HVDC_LINE.name(), new HvdcLineFilterRepositoryProxy(hvdcLineFilterRepository));

        filterRepositories.put(FilterType.SCRIPT.name(), new ScriptFilterRepositoryProxy(scriptFiltersRepository));

    }

    List<IFilterAttributes> getFilters() {
        return filterRepositories.entrySet().stream()
            .flatMap(entry -> entry.getValue().getFiltersAttributes())
            .collect(Collectors.toList());
    }

    List<FilterAttributes> getFiltersMetadata(List<UUID> ids) {
        return filterRepositories.entrySet().stream()
                .flatMap(entry -> entry.getValue().getFiltersAttributes(ids))
                .collect(Collectors.toList());
    }

    Optional<AbstractFilter> getFilter(UUID id) {
        Objects.requireNonNull(id);
        for (AbstractFilterRepositoryProxy<?, ?> repository : filterRepositories.values()) {
            Optional<AbstractFilter> res = repository.getFilter(id);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    Optional<AbstractFilterEntity> getFilterEntity(UUID id) {
        Objects.requireNonNull(id);
        for (AbstractFilterRepositoryProxy<?, ?> repository : filterRepositories.values()) {
            Optional<AbstractFilterEntity> res = (Optional<AbstractFilterEntity>) repository.getFilterEntity(id);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    @Transactional
    public <F extends AbstractFilter> AbstractFilter createFilter(F filter) {
        return getRepository(filter.getType().name(), filter).insert(filter);
    }

    private AbstractFilterRepositoryProxy getRepository(String type, AbstractFilter filter) {
        if (type.equals(FilterType.SCRIPT.name())) {
            return filterRepositories.get(FilterType.SCRIPT.name());
        }
        return filterRepositories.get(((FormFilter) filter).getEquipmentFilterForm().getEquipmentType());
    }

    @Transactional
    public <F extends AbstractFilter> void changeFilter(UUID id, F filter) {
        Optional<AbstractFilter> f = getFilter(id);
        FormFilter formFilter = (FormFilter) filter;
        if (f.isPresent()) {
            FormFilter oldFilter = (FormFilter) f.get();
            if (oldFilter.getEquipmentFilterForm().getEquipmentType() == formFilter.getEquipmentFilterForm().getEquipmentType()) {  // filter type has not changed
                filter.setCreationDate(f.get().getCreationDate());
                getRepository(filter.getType().name(), filter).modify(id, filter);
            } else {  // filter type has changed
                if ((f.get().getType() == FilterType.SCRIPT && filter.getType() != FilterType.SCRIPT) ||
                    (f.get().getType() != FilterType.SCRIPT && filter.getType() == FilterType.SCRIPT)) {
                    throw new PowsyblException(WRONG_FILTER_TYPE);
                } else {
                    getRepository(f.get().getType().name(), f.get()).deleteById(id);
                    filter.setId(id);
                    filter.setCreationDate(f.get().getCreationDate());
                    createFilter(filter);
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, FILTER_LIST + id + NOT_FOUND);
        }
    }

    void deleteFilter(UUID id) {
        Objects.requireNonNull(id);
        if (filterRepositories.values().stream().noneMatch(repository -> repository.deleteById(id))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, FILTER_LIST + id + NOT_FOUND);
        }
    }

    public void deleteAll() {
        filterRepositories.values().forEach(AbstractFilterRepositoryProxy::deleteAll);
    }

    private String generateGroovyScriptFromFilter(AbstractFilter filter) {
        return filtersToScript.generateGroovyScriptFromFilters(filter);
    }

    @Transactional
    public AbstractFilter replaceFilterWithScript(UUID id) {
        Objects.requireNonNull(id);

        Optional<AbstractFilter> filter = getFilter(id);
        if (filter.isPresent()) {
            if (filter.get().getType() == FilterType.SCRIPT) {
                throw new PowsyblException(WRONG_FILTER_TYPE);
            } else {
                String script = generateGroovyScriptFromFilter(filter.get());
                getRepository(filter.get().getType().name(), filter.get()).deleteById(filter.get().getId());
                return getRepository(FilterType.SCRIPT.name(), null).insert(ScriptFilter.builder().id(filter.get().getId()).script(script).build());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, FILTER_LIST + id + NOT_FOUND);
        }
    }

    @Transactional
    public AbstractFilter newScriptFromFilter(UUID filterId, UUID newId) {
        Objects.requireNonNull(filterId);

        Optional<AbstractFilter> filter = getFilter(filterId);
        if (filter.isPresent()) {
            if (filter.get().getType() == FilterType.SCRIPT) {
                throw new PowsyblException(WRONG_FILTER_TYPE);
            } else {
                String script = generateGroovyScriptFromFilter(filter.get());
                return getRepository(FilterType.SCRIPT.name(), null).insert(ScriptFilter.builder().id(newId == null ? UUID.randomUUID() : newId).script(script).build());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, FILTER_LIST + filterId + NOT_FOUND);
        }
    }
}
