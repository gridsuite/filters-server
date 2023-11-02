/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server.utils.expertfilter;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.Optional;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
public final class ExpertFilterUtils {
    private ExpertFilterUtils() { }

    public static <I extends Identifiable<I>> String getFieldValue(FieldType field, Identifiable<I> identifiable) {
        return switch (identifiable.getType()) {
            case GENERATOR -> getGeneratorFieldValue(field, (Generator) identifiable);
            case LOAD -> getLoadFieldValue(field, (Load) identifiable);
            default -> throw new PowsyblException(identifiable.getType() + " injection type is not implemented with expert filter");
        };
    }

    private static String getLoadFieldValue(FieldType field, Load load) {
        return switch (field) {
            case ID -> load.getId();
            default -> throw new PowsyblException("Field " + field + " with " + load.getType() + " injection type is not implemented with expert filter");
        };
    }

    private static String getGeneratorFieldValue(FieldType field, Generator generator) {
        return switch (field) {
            case ID -> generator.getId();
            case NAME -> generator.getNameOrId();
            case NOMINAL_VOLTAGE -> String.valueOf(generator.getTerminal().getVoltageLevel().getNominalV());
            case COUNTRY -> {
                Optional<Country> country = generator.getTerminal().getVoltageLevel().getSubstation().flatMap(Substation::getCountry);
                yield country.isPresent() ? String.valueOf(country.get()) : "";
            }
            case ENERGY_SOURCE -> String.valueOf(generator.getEnergySource());
            case MIN_P -> String.valueOf(generator.getMinP());
            case MAX_P -> String.valueOf(generator.getMaxP());
            case TARGET_V -> String.valueOf(generator.getTargetV());
            case TARGET_P -> String.valueOf(generator.getTargetP());
            case TARGET_Q -> String.valueOf(generator.getTargetQ());
            case VOLTAGE_REGULATOR_ON -> String.valueOf(generator.isVoltageRegulatorOn());
        };
    }
}
