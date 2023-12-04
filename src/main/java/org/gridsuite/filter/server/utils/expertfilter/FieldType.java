/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server.utils.expertfilter;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
public enum FieldType {
    UNKNOWN, // used for test covering
    ID,
    NAME,
    NOMINAL_VOLTAGE,
    MIN_P,
    MAX_P,
    TARGET_P,
    TARGET_V,
    TARGET_Q,
    ENERGY_SOURCE,
    COUNTRY,
    VOLTAGE_REGULATOR_ON,
    PLANNED_ACTIVE_POWER_SET_POINT,
    VOLTAGE_LEVEL_ID,
}
