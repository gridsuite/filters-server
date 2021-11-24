/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.filter.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.gridsuite.filter.server.dto.*;
import org.gridsuite.filter.server.entities.NumericFilterEntity;
import org.gridsuite.filter.server.utils.EquipmentType;
import org.gridsuite.filter.server.utils.FilterType;
import org.gridsuite.filter.server.utils.RangeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {FilterApplication.class})
public class FilterEntityControllerTest {

    public static final String URL_TEMPLATE = "/" + FilterApi.API_VERSION + "/filters/";
    @Autowired
    private MockMvc mvc;

    @Autowired
    private FilterService filterService;

    @Before
    public void setUp() {
        Configuration.defaultConfiguration();
        MockitoAnnotations.initMocks(this);
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider(objectMapper);
            private final MappingProvider mappingProvider = new JacksonMappingProvider(objectMapper);

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    @After
    public void cleanUp() {
        filterService.deleteAll();
    }

    ObjectMapper objectMapper = new ObjectMapper();

    public String joinWithComma(Object... array) {
        return join(array, ",");
    }

    @Test
    public void testLineFilter() throws Exception {
        UUID filterId1 = UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e");
        UUID filterId2 = UUID.fromString("42b70a4d-e0c4-413a-8e3e-78e9027d300f");

        Date creationDate = new Date();
        Date modificationDate = new Date();

        // test all fields
        String lineFilter = "{" + joinWithComma(
            jsonVal("id", filterId1.toString()),
            jsonVal("type", FilterType.FORM.name()),
            jsonVal("equipmentType", EquipmentType.LINE.name()),
            jsonVal("substationName1", "ragala"),
            jsonVal("substationName2", "miamMiam"),
            jsonVal("equipmentID", "vazy"),
            jsonVal("equipmentName", "tata"),
            numericalRange("nominalVoltage1", RangeType.RANGE, 5., 8.),
            numericalRange("nominalVoltage2", RangeType.EQUALITY, 6., null),
            jsonSet("countries1", Set.of("yoyo")),
            jsonSet("countries2", Set.of("smurf", "schtroumph"))) + "}";

        insertFilter(filterId1, lineFilter);
        List<FilterAttributes> filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(filterId1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });

        Date dateCreation = filterAttributes.get(0).getCreationDate();
        Date dateModification = filterAttributes.get(0).getModificationDate();

        // test replace with same filter type and null value (country set & numerical range)
        String minimalLineFilter = "{" + joinWithComma(
            jsonVal("id", filterId1.toString()),
            jsonVal("type", FilterType.FORM.name()),
            jsonVal("equipmentType", EquipmentType.LINE.name()))
            + "}";

        modifyFilter(filterId1, minimalLineFilter);

        // script filter
        String scriptFilter = "{" + joinWithComma(
            jsonVal("id", filterId2.toString()),
            jsonVal("type", FilterType.SCRIPT.name()),
            jsonVal("equipmentType", EquipmentType.NONE.name()),
            jsonVal("script", "test")) +
            "}";

        insertFilter(filterId2, scriptFilter);

        var res = mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        filterAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        assertEquals(2, filterAttributes.size());
        if (!filterAttributes.get(0).getId().equals(filterId1)) {
            Collections.reverse(filterAttributes);
        }

        matchFilterInfos(filterAttributes.get(0), filterId1, FilterType.FORM, EquipmentType.LINE, creationDate, modificationDate);
        matchFilterInfos(filterAttributes.get(1), filterId2, FilterType.SCRIPT, EquipmentType.NONE, creationDate, modificationDate);

        filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(filterId1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });
        assertEquals(1, filterAttributes.size());
        assertEquals(dateCreation, filterAttributes.get(0).getCreationDate());
        assertTrue(dateModification.getTime() < filterAttributes.get(0).getModificationDate().getTime());

        // test replace line filter with other filter type
        String generatorFilter = "{" + joinWithComma(
            jsonVal("type", FilterType.FORM.name()),
            jsonVal("equipmentType", EquipmentType.GENERATOR.name()),
            jsonVal("substationName", "s1"),
            jsonVal("equipmentID", "eqId1"),
            jsonVal("equipmentName", "gen1"),
            numericalRange("nominalVoltage", RangeType.APPROX, 225., 3.),
            jsonSet("countries", Set.of("FR", "BE"))) + "}";

        modifyFilter(filterId1, generatorFilter);

        filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(filterId1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });
        assertEquals(1, filterAttributes.size());
        assertEquals(dateCreation, filterAttributes.get(0).getCreationDate());
        assertTrue(dateModification.getTime() < filterAttributes.get(0).getModificationDate().getTime());
        assertEquals(filterId1, filterAttributes.get(0).getId());
        assertEquals(FilterType.FORM, filterAttributes.get(0).getType());

        // delete
        mvc.perform(delete(URL_TEMPLATE + filterId2)).andExpect(status().isOk());

        mvc.perform(delete(URL_TEMPLATE + filterId2)).andExpect(status().isNotFound());

        mvc.perform(get(URL_TEMPLATE + filterId2)).andExpect(status().isNotFound());

        mvc.perform(put(URL_TEMPLATE + filterId2).contentType(APPLICATION_JSON).content(scriptFilter)).andExpect(status().isNotFound());

        filterService.deleteAll();
    }

    @Test
    public void testGeneratorFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.GENERATOR, UUID.fromString("42b70a4d-e0c4-413a-8e3e-78e9027d300f"),
                        "genId1", "genName", "s1", Set.of("FR", "IT"), RangeType.RANGE, 210., 240.);
    }

    @Test
    public void testLoadFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.LOAD, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "loadId1", "loadName", "s2", Set.of("BE", "NL"), RangeType.APPROX, 225., 5.);
    }

    @Test
    public void testShuntCompensatorFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.SHUNT_COMPENSATOR, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "shuntId1", "shuntName", "s3", Set.of("ES"), RangeType.EQUALITY, 150., null);
    }

    @Test
    public void testStaticVarCompensatorFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.STATIC_VAR_COMPENSATOR, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "staticVarCompensatorId1", "staticVarCompensatorName", "s1", null, null, null, null);
    }

    @Test
    public void testBatteryFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.BATTERY, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "batteryId1", "batteryName", null, Set.of("FR"), RangeType.RANGE, 45., 65.);
    }

    @Test
    public void testBusBarSectionFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.BUSBAR_SECTION, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            null, "batteryName", null, Set.of("DE"), RangeType.EQUALITY, 380., null);
    }

    @Test
    public void testDanglingLineFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.DANGLING_LINE, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "danglingLineId1", null, "s2", Set.of("FR"), RangeType.APPROX, 150., 8.);
    }

    @Test
    public void testLccConverterStationFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.LCC_CONVERTER_STATION, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "lccId1", "lccName1", "s3", Set.of("FR", "BE", "NL", "DE", "IT"), RangeType.RANGE, 20., 400.);
    }

    @Test
    public void testVscConverterStationFilter() throws Exception {
        insertInjectionFilter(FilterType.FORM, EquipmentType.VSC_CONVERTER_STATION, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "vscId1", "vscName1", "s2", null, RangeType.EQUALITY, 225., null);
    }

    @Test
    public void testHvdcLineFilter() throws Exception {
        insertHvdcLineFilter(FilterType.FORM, EquipmentType.HVDC_LINE, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "hvdcId1", "hvdcName1", "s1", "s2", Set.of("FR"), Set.of("UK"), RangeType.EQUALITY, 380., null);
    }

    @Test
    public void testTwoWindingsTransformerFilter() throws Exception {
        List<RangeType> rangeTypes = new ArrayList<>();
        rangeTypes.add(RangeType.EQUALITY);
        rangeTypes.add(RangeType.APPROX);
        List<Double> values1 = new ArrayList<>();
        values1.add(225.);
        values1.add(380.);
        List<Double> values2 = new ArrayList<>();
        values2.add(null);
        values2.add(5.);

        insertTransformerFilter(FilterType.FORM, EquipmentType.TWO_WINDINGS_TRANSFORMER, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "2wtId1", "2wtName1", "s1", Set.of("FR", "BE", "NL"), rangeTypes, values1, values2);
    }

    @Test
    public void testThreeWindingsTransformerFilter() throws Exception {
        List<RangeType> rangeTypes = new ArrayList<>();
        rangeTypes.add(RangeType.RANGE);
        rangeTypes.add(RangeType.EQUALITY);
        rangeTypes.add(RangeType.APPROX);
        List<Double> values1 = new ArrayList<>();
        values1.add(210.);
        values1.add(150.);
        values1.add(380.);
        List<Double> values2 = new ArrayList<>();
        values2.add(240.);
        values2.add(null);
        values2.add(5.);

        insertTransformerFilter(FilterType.FORM, EquipmentType.THREE_WINDINGS_TRANSFORMER, UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e"),
            "3wtId1", "3wtName1", "s2", Set.of("IT", "CH"), rangeTypes, values1, values2);
    }

    @Test
    public void testFilterToScript() throws Exception {
        UUID filterId1 = UUID.fromString("77614d91-c168-4f89-8fb9-77a23729e88e");
        UUID filterId2 = UUID.fromString("42b70a4d-e0c4-413a-8e3e-78e9027d300f");
        UUID filterId3 = UUID.fromString("99999999-e0c4-413a-8e3e-78e9027d300f");

        String lineFilter = "{" + joinWithComma(
            jsonVal("id", filterId1.toString()),
            jsonVal("type", FilterType.FORM.name()),
            jsonVal("equipmentType", EquipmentType.LINE.name()),
            jsonVal("substationName1", "ragala"),
            jsonVal("substationName2", "miamMiam"),
            jsonVal("equipmentID", "vazy"),
            jsonVal("equipmentName", "tata"),
            numericalRange("nominalVoltage1", RangeType.RANGE, 5., 8.),
            numericalRange("nominalVoltage2", RangeType.EQUALITY, 6., null),
            jsonSet("countries1", Set.of("yoyo")),
            jsonSet("countries2", Set.of("smurf", "schtroumph"))) + "}";

        insertFilter(filterId1, lineFilter);

        mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andExpect(content().json("[{\"equipmentType\":\"LINE\"}]"));

        // new script from filter
        mvc.perform(post(URL_TEMPLATE + filterId1 + "/new-script?newId=" + UUID.randomUUID())).andExpect(status().isOk());

        mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andExpect(content().json("[{\"equipmentType\":\"LINE\"}, {\"type\":\"SCRIPT\"}]"));

        // replace filter with script
        mvc.perform(put(URL_TEMPLATE + filterId1 + "/replace-with-script")).andExpect(status().isOk());

        mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andExpect(content().json("[{\"type\":\"SCRIPT\"}, {\"type\":\"SCRIPT\"}]"));

        String scriptFilter = "{" + joinWithComma(
            jsonVal("id", filterId2.toString()),
            jsonVal("type", FilterType.SCRIPT.name()),
            jsonVal("equipmentType", EquipmentType.NONE.name()),
            jsonVal("script", "test2"))
            + "}";
        insertFilter(filterId2, scriptFilter);

        assertThrows("Wrong filter type, should never happen", Exception.class, () -> mvc.perform(post(URL_TEMPLATE + filterId2 + "/new-script?newId=" + UUID.randomUUID())));
        assertThrows("Wrong filter type, should never happen", Exception.class, () -> mvc.perform(put(URL_TEMPLATE + filterId2 + "/replace-with-script")));
        mvc.perform(post(URL_TEMPLATE + filterId3 + "/new-script?newId=" + filterId2)).andExpect(status().isNotFound());
        mvc.perform(put(URL_TEMPLATE + filterId3 + "/replace-with-script")).andExpect(status().isNotFound());
    }

    private void matchFilterInfos(IFilterAttributes filterAttribute, UUID id, FilterType type, EquipmentType equipmentType, Date creationDate, Date modificationDate) {
        assertEquals(filterAttribute.getId(), id);
        assertEquals(filterAttribute.getType(), type);
        assertEquals(filterAttribute.getEquipmentType(), equipmentType);
        assertTrue((creationDate.getTime() - filterAttribute.getCreationDate().getTime()) < 1000);
        assertTrue((modificationDate.getTime() - filterAttribute.getModificationDate().getTime()) < 100);
    }

    private void insertFilter(UUID filterId, String content) throws Exception {
        String strRes = mvc.perform(post(URL_TEMPLATE).param("id", filterId.toString())
            .content(content)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(content, strRes, JSONCompareMode.LENIENT);

        mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        MvcResult mockResponse = mvc.perform(get(URL_TEMPLATE + filterId)).andExpect(status().isOk()).andReturn();
        // Check we didn't miss anything
        JSONAssert.assertEquals(content, strRes, JSONCompareMode.LENIENT);
    }

    private void insertFilter(UUID filterId, AbstractFilter filter) throws Exception {
        String tmp = objectMapper.writeValueAsString(filter);
        String strRes = mvc.perform(post(URL_TEMPLATE).param("id", filterId.toString())
                        .content(objectMapper.writeValueAsString(filter))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(objectMapper.writeValueAsString(filter), strRes, JSONCompareMode.LENIENT);

        mvc.perform(get(URL_TEMPLATE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        MvcResult mockResponse = mvc.perform(get(URL_TEMPLATE + filterId)).andExpect(status().isOk()).andReturn();
        // Check we didn't miss anything
        JSONAssert.assertEquals(String.valueOf(filter), strRes, JSONCompareMode.LENIENT);
    }

    private void modifyFilter(UUID filterId, String content) throws Exception {
        mvc.perform(put(URL_TEMPLATE + filterId)
            .content(content)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String strRes = mvc.perform(get(URL_TEMPLATE + filterId)).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(content, strRes, JSONCompareMode.LENIENT);

        mvc.perform(get(URL_TEMPLATE))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        MvcResult mockResponse = mvc.perform(get(URL_TEMPLATE + filterId)).andExpect(status().isOk()).andReturn();
        // Check we didn't miss anything
        JSONAssert.assertEquals(content, strRes, JSONCompareMode.LENIENT);
    }

    public StringBuilder jsonVal(String id, String val) {
        return new StringBuilder("\"").append(id).append("\": \"").append(val).append("\"");
    }

    public StringBuilder jsonDouble(String id, Double val) {
        return new StringBuilder("\"").append(id).append("\": ").append(val);
    }

    public StringBuilder jsonSet(String id, Set<String> set) {
        return new StringBuilder("\"").append(id).append("\": ").append("[")
            .append(!set.isEmpty() ? "\"" + join(set, "\",\"") + "\"" : "").append("]");
    }

    private StringBuilder numericalRange(String id, RangeType range, Double value1, Double value2) {
        return new StringBuilder("\"").append(id).append("\": ")
            .append("{").append(joinWithComma(
                jsonDouble("value1", value1),
                jsonDouble("value2", value2),
                jsonVal("type", range.name()))
            ).append("}");
    }

    private void insertInjectionFilter(FilterType type, EquipmentType equipmentType, UUID id, String equipmentID, String equipmentName,
                                       String substationName, Set<String> countries,
                                       RangeType rangeType, Double value1, Double value2)  throws Exception {
        String filter = "{" + joinWithComma(
            jsonVal("id", id.toString()),
            jsonVal("type", type.name()),
            jsonVal("equipmentType", equipmentType.name()));

        if (equipmentID != null) {
            filter += ", " + jsonVal("equipmentID", equipmentID);
        }
        if (equipmentName != null) {
            filter += ", " + jsonVal("equipmentName", equipmentName);
        }
        if (substationName != null) {
            filter += ", " + jsonVal("substationName", substationName);
        }
        if (rangeType != null) {
            filter += ", " + numericalRange("nominalVoltage", rangeType, value1, value2);
        }
        if (countries != null) {
            filter += ", " + jsonSet("countries", countries);
        }
        filter += "}";

        insertFilter(id, filter);

        List<FilterAttributes> filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(id))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });

        assertEquals(1, filterAttributes.size());
        assertEquals(id, filterAttributes.get(0).getId());
        assertEquals(FilterType.FORM, filterAttributes.get(0).getType());

        mvc.perform(delete(URL_TEMPLATE + id)).andExpect(status().isOk());
    }

    private void insertTransformerFilter(FilterType type, EquipmentType equipmentType, UUID id, String equipmentID, String equipmentName,
                                         String substationName, Set<String> countries,
                                         List<RangeType> rangeTypes, List<Double> values1, List<Double> values2)  throws Exception {
        String filter = "{" + joinWithComma(
            jsonVal("id", id.toString()),
            jsonVal("type", type.name()),
            jsonVal("equipmentType", equipmentType.name()));

        if (equipmentID != null) {
            filter += ", " + jsonVal("equipmentID", equipmentID);
        }
        if (equipmentName != null) {
            filter += ", " + jsonVal("equipmentName", equipmentName);
        }
        if (substationName != null) {
            filter += ", " + jsonVal("substationName", substationName);
        }
        if (rangeTypes != null) {
            for (int i = 0; i < rangeTypes.size(); ++i) {
                filter += ", " + numericalRange("nominalVoltage" + (i + 1), rangeTypes.get(i), values1.get(i), values2.get(i));
            }
        }
        if (countries != null) {
            filter += ", " + jsonSet("countries", countries);
        }
        filter += "}";

        insertFilter(id, filter);

        List<FilterAttributes> filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(id))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });

        assertEquals(1, filterAttributes.size());
        assertEquals(id, filterAttributes.get(0).getId());
        assertEquals(FilterType.FORM, filterAttributes.get(0).getType());

        mvc.perform(delete(URL_TEMPLATE + id)).andExpect(status().isOk());
    }

    private void insertHvdcLineFilter(FilterType type, EquipmentType equipmentType, UUID id, String equipmentID, String equipmentName,
                                      String substationName1, String substationName2, Set<String> countries1,
                                      Set<String> countries2, RangeType rangeType, Double value1, Double value2)  throws Exception {
        AbstractFilter hvdcLineFilter = new FormFilter(
                id,
                java.sql.Date.from(Instant.now()),
                java.sql.Date.from(Instant.now()),
                new HvdcLineFilter(
                        equipmentID,
                        equipmentName,
                        substationName1,
                        substationName2,
                        countries1,
                        countries2,
                        new NumericalFilter(rangeType, value1, value2)
                )
        );
//        String filter = "{" + joinWithComma(
//            jsonVal("id", id.toString()),
//            jsonVal("type", type.name()),
//            jsonVal("equipmentType", equipmentType.name()));
//
//        if (equipmentID != null) {
//            filter += ", " + jsonVal("equipmentID", equipmentID);
//        }
//        if (equipmentName != null) {
//            filter += ", " + jsonVal("equipmentName", equipmentName);
//        }
//        if (substationName1 != null) {
//            filter += ", " + jsonVal("substationName1", substationName1);
//        }
//        if (substationName2 != null) {
//            filter += ", " + jsonVal("substationName2", substationName2);
//        }
//        if (rangeType != null) {
//            filter += ", " + numericalRange("nominalVoltage", rangeType, value1, value2);
//        }
//        if (countries1 != null) {
//            filter += ", " + jsonSet("countries1", countries1);
//        }
//        if (countries2 != null) {
//            filter += ", " + jsonSet("countries2", countries2);
//        }
//        filter += "}";

        insertFilter(id, hvdcLineFilter);

        List<FilterAttributes> filterAttributes = objectMapper.readValue(
            mvc.perform(post("/" + FilterApi.API_VERSION + "/filters/metadata")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(id))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() {
            });

        assertEquals(1, filterAttributes.size());
        assertEquals(id, filterAttributes.get(0).getId());
        assertEquals(FilterType.FORM, filterAttributes.get(0).getType());

        mvc.perform(delete(URL_TEMPLATE + id)).andExpect(status().isOk());
    }
}
