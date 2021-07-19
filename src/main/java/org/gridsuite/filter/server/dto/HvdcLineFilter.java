/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.gridsuite.filter.server.utils.FilterType;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Getter
@NoArgsConstructor
@SuperBuilder
@ApiModel(description = "Hvdc Filters", parent = AbstractGenericFilter.class)
public class HvdcLineFilter extends AbstractGenericFilter {
    @Override
    public FilterType getType() {
        return FilterType.HVDC_LINE;
    }

    @ApiModelProperty("SubstationName1")
    String substationName1;

    @ApiModelProperty("SubstationName2")
    String substationName2;

    @ApiModelProperty("Countries1")
    private Set<String> countries1;

    @ApiModelProperty("Countries2")
    private Set<String> countries2;

    @ApiModelProperty("Nominal voltage")
    private NumericalFilter nominalVoltage;

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
            && substationName1 == null
            && substationName2 == null
            && CollectionUtils.isEmpty(countries1)
            && CollectionUtils.isEmpty(countries2)
            && nominalVoltage == null;
    }
}
