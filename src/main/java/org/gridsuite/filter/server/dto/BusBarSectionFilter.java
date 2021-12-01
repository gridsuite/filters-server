/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.gridsuite.filter.server.utils.EquipmentType;

import java.util.SortedSet;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Schema(description = "Busbar section Filters", allOf = AbstractInjectionFilter.class)
public class BusBarSectionFilter extends AbstractInjectionFilter {
    public BusBarSectionFilter(String equipmentID, String equipmentName, String substationName, SortedSet<String> countries, NumericalFilter nominalVoltage) {
        super(equipmentID, equipmentName, substationName, countries, nominalVoltage);
    }

    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.BUSBAR_SECTION;
    }
}
