/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server.dto.expertrule;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.gridsuite.filter.server.utils.*;

import java.util.List;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "dataType",
        include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StringExpertRule.class, name = "STRING"),
    @JsonSubTypes.Type(value = BooleanExpertRule.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = EnumExpertRule.class, name = "ENUM"),
    @JsonSubTypes.Type(value = NumberExpertRule.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = CombinatorExpertRule.class, name = "COMBINATOR")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder
public abstract class AbstractExpertRule {

    @Schema(description = "Combinator")
    private CombinatorType combinator;

    @Schema(description = "Field")
    private FieldType field;

    @Schema(description = "Operator")
    private OperatorType operator;

    @Schema(description = "Rules")
    private List<AbstractExpertRule> rules;

    public abstract boolean evaluateRule(String identifiableValue);

    public abstract DataType getDataType();

    @JsonIgnore
    public abstract String getStringValue();
}
