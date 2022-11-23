/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.filter.server;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.filter.server.dto.AbstractEquipmentFilterForm;
import org.gridsuite.filter.server.dto.AbstractFilter;
import org.gridsuite.filter.server.dto.ManualFilterEquipmentAttributes;
import org.gridsuite.filter.server.dto.ManualFilter;
import org.gridsuite.filter.server.entities.AbstractFilterEntity;
import org.gridsuite.filter.server.entities.ManualFilterEntity;
import org.gridsuite.filter.server.entities.ManualFilterEquipmentEntity;
import org.gridsuite.filter.server.repositories.ManualFilterRepository;
import org.gridsuite.filter.server.utils.EquipmentType;
import org.gridsuite.filter.server.utils.FilterType;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Seddik Yengui <seddik.yengui at rte-france.com>
 */

public class ManualFilterRepositoryProxy extends AbstractFilterRepositoryProxy<ManualFilterEntity, ManualFilterRepository> {
    private final ManualFilterRepository manualFilterRepository;

    public ManualFilterRepositoryProxy(ManualFilterRepository manualFilterRepository) {
        this.manualFilterRepository = manualFilterRepository;
    }

    @Override
    ManualFilterRepository getRepository() {
        return manualFilterRepository;
    }

    @Override
    AbstractFilter toDto(ManualFilterEntity filterEntity) {
        return new ManualFilter(filterEntity.getId(),
                filterEntity.getModificationDate(),
                filterEntity.getEquipmentType(),
                filterEntity.getFilterEquipmentEntityList()
                        .stream()
                        .map(entity -> new ManualFilterEquipmentAttributes(entity.getEquipmentId(),
                                                                     entity.getDistributionKey()))
                        .collect(Collectors.toList()));
    }

    @Override
    ManualFilterEntity fromDto(AbstractFilter dto) {
        if (dto instanceof ManualFilter) {
            var filter = (ManualFilter) dto;
            var manualFilterEntityBuilder = ManualFilterEntity.builder()
                    .equipmentType(filter.getEquipmentType())
                    .filterEquipmentEntityList(filter.getFilterEquipmentsAttributes()
                            .stream()
                            .map(attributes -> ManualFilterEquipmentEntity.builder()
                                    .id(UUID.randomUUID())
                                    .equipmentId(attributes.getEquipmentID())
                                    .distributionKey(attributes.getDistributionKey())
                                    .build())
                            .collect(Collectors.toList()));

            buildAbstractFilter(manualFilterEntityBuilder, filter);
            return manualFilterEntityBuilder.build();
        }
        throw new PowsyblException(WRONG_FILTER_TYPE);
    }

    @Override
    FilterType getFilterType() {
        return FilterType.MANUAL;
    }

    @Override
    public EquipmentType getEquipmentType() {
        throw new UnsupportedOperationException("A filter id must be provided to get equipment type !!");
    }

    @Override
    public EquipmentType getEquipmentType(UUID id) {
        return manualFilterRepository.findById(id)
            .map(ManualFilterEntity::getEquipmentType)
            .orElseThrow(() -> new PowsyblException("Manual filter " + id + " not found"));
    }

    @Override
    public AbstractEquipmentFilterForm buildEquipmentFormFilter(AbstractFilterEntity entity) {
        return null;
    }
}
