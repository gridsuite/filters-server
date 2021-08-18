/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.filter.server;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.filter.server.dto.AbstractFilter;
import org.gridsuite.filter.server.dto.GeneratorFilter;
import org.gridsuite.filter.server.entities.GeneratorFilterEntity;
import org.gridsuite.filter.server.repositories.GeneratorFilterRepository;
import org.gridsuite.filter.server.utils.FilterType;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */

class GeneratorFilterRepositoryProxy extends AbstractFilterRepositoryProxy<GeneratorFilterEntity, GeneratorFilterRepository> {

    private final GeneratorFilterRepository generatorFilterRepository;

    public GeneratorFilterRepositoryProxy(GeneratorFilterRepository generatorFilterRepository) {
        this.generatorFilterRepository = generatorFilterRepository;
    }

    @Override
    public FilterType getRepositoryType() {
        return FilterType.GENERATOR;
    }

    @Override
    public GeneratorFilterRepository getRepository() {
        return generatorFilterRepository;
    }

    @Override
    public AbstractFilter toDto(GeneratorFilterEntity entity) {
        return buildInjectionFilter(
            GeneratorFilter.builder(), entity).build();
    }

    @Override
    public GeneratorFilterEntity fromDto(AbstractFilter dto) {
        if (dto instanceof GeneratorFilter) {
            var generatorFilterEntityBuilder = GeneratorFilterEntity.builder();
            buildInjectionFilter(generatorFilterEntityBuilder, (GeneratorFilter) dto);
            return generatorFilterEntityBuilder.build();
        }
        throw new PowsyblException(WRONG_FILTER_TYPE);
    }
}