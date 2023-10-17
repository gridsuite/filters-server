/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.filter.server.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.gridsuite.filter.server.utils.CombinatorType;
import org.gridsuite.filter.server.utils.DataType;
import org.gridsuite.filter.server.utils.FieldType;
import org.gridsuite.filter.server.utils.OperatorType;

import java.util.List;
import java.util.UUID;

/**
 * @author Antoine Bouhours <antoine.bouhours at rte-france.com>
 */
@Getter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@Entity
@Table(name = "expert_rule")
public class ExpertRuleEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "combinator")
    private CombinatorType combinator;

    @Column(name = "field")
    private FieldType field;

    @Column(name = "value_")
    private String value;

    @Column(name = "operator")
    private OperatorType operator;

    @Column(name = "dataType")
    private DataType dataType;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentRule")
    private List<ExpertRuleEntity> rules;

    @ManyToOne
    @JoinColumn(name = "parent_rule_id")
    private ExpertRuleEntity parentRule;
}