/*
 * Copyright 1999-2024 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.webflow.param;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.function.Predicate;
import com.alibaba.csp.sentinel.webflow.SentinelWebFlowConstants;
import com.alibaba.csp.sentinel.webflow.rule.WebFlowRule;
import com.alibaba.csp.sentinel.webflow.rule.WebFlowRuleManager;
import com.alibaba.csp.sentinel.webflow.rule.WebParamItem;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebParamParserTest {

    @Test
    public void testParseParameterFor() {
        RequestItemParser<Object> itemParser = mock(RequestItemParser.class);
        WebParamParser<Object> parser = new WebParamParser<Object>(itemParser);
        // Create a fake request.
        Object request = new Object();
        // Prepare gateway rules.
        Set<WebFlowRule> rules = new HashSet<WebFlowRule>();
        String routeId1 = "my_test_route_A";
        rules.add(new WebFlowRule(routeId1)
                .setCount(5d)
                .setIntervalMs(1)
        );
        rules.add(new WebFlowRule(routeId1)
                .setCount(10d)
                .setControlBehavior(2)
                .setMaxQueueingTimeoutMs(1000)
        );
        WebFlowRuleManager.loadRules(rules);

        assertEquals(rules, WebFlowRuleManager.getRules());
    }

    @Test
    public void testParseParametersWithItems() {
        RequestItemParser<Object> itemParser = mock(RequestItemParser.class);
        WebParamParser<Object> paramParser = new WebParamParser<Object>(itemParser);
        // Create a fake request.
        Object request = new Object();

        // Prepare gateway rules.
        Set<WebFlowRule> rules = new HashSet<WebFlowRule>();
        final String routeId1 = "my_test_route_A";
        final String api1 = "my_test_route_B";
        final String headerName = "X-Sentinel-Flag";
        final String paramName = "p";
        final String cookieName = "myCookie";
        WebFlowRule routeRuleNoParam = new WebFlowRule(routeId1)
                .setCount(10d);
        WebFlowRule routeRule1 = new WebFlowRule(routeId1)
                .setCount(2d)
                .setBurst(2)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_CLIENT_IP)
                );
        WebFlowRule routeRule2 = new WebFlowRule(routeId1)
                .setCount(10d)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER)
                .setMaxQueueingTimeoutMs(600)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HEADER)
                        .setFieldName(headerName)
                );
        WebFlowRule routeRule3 = new WebFlowRule(routeId1)
                .setCount(20d)
                .setBurst(5)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName(paramName)
                );
        WebFlowRule routeRule4 = new WebFlowRule(routeId1)
                .setCount(120d)
                .setBurst(30)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HOST)
                );
        WebFlowRule routeRule5 = new WebFlowRule(routeId1)
                .setCount(50d)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_COOKIE)
                        .setFieldName(cookieName)
                );
        WebFlowRule apiRule1 = new WebFlowRule(api1)
                .setResourceMode(SentinelWebFlowConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(5d)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName(paramName)
                );
        rules.add(routeRule1);
        rules.add(routeRule2);
        rules.add(routeRule3);
        rules.add(routeRule4);
        rules.add(routeRule5);
        rules.add(routeRuleNoParam);
        rules.add(apiRule1);
        WebFlowRuleManager.loadRules(rules);

        final String expectedHost = "hello.test.sentinel";
        final String expectedAddress = "66.77.88.99";
        final String expectedHeaderValue1 = "Sentinel";
        final String expectedUrlParamValue1 = "17";
        final String expectedCookieValue1 = "Sentinel-Foo";

        mockClientHostAddress(itemParser, expectedAddress);
        Map<String, String> expectedHeaders = new HashMap<String, String>() {{
            put(headerName, expectedHeaderValue1); put("Host", expectedHost);
        }};
        mockHeaders(itemParser, expectedHeaders);
        mockSingleUrlParam(itemParser, paramName, expectedUrlParamValue1);
        mockSingleCookie(itemParser, cookieName, expectedCookieValue1);

        String expectedUrlParamValue2 = "fs";
        mockSingleUrlParam(itemParser, paramName, expectedUrlParamValue2);

    }

    @Test
    public void testParseParametersWithEmptyItemPattern() {
        RequestItemParser<Object> itemParser = mock(RequestItemParser.class);
        WebParamParser<Object> paramParser = new WebParamParser<Object>(itemParser);
        // Create a fake request.
        Object request = new Object();
        // Prepare gateway rules.
        Set<WebFlowRule> rules = new HashSet<WebFlowRule>();
        final String routeId = "my_test_route_DS(*H";
        final String headerName = "X-Sentinel-Flag";
        WebFlowRule routeRule1 = new WebFlowRule(routeId)
                .setCount(10d)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HEADER)
                        .setFieldName(headerName)
                        .setPattern("")
                        .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_EXACT)
                );
        rules.add(routeRule1);
        WebFlowRuleManager.loadRules(rules);

        mockSingleHeader(itemParser, headerName, "Sent1nel");

    }

    @Test
    public void testParseParametersWithItemPatternMatching() {
        RequestItemParser<Object> itemParser = mock(RequestItemParser.class);
        WebParamParser<Object> paramParser = new WebParamParser<Object>(itemParser);
        // Create a fake request.
        Object request = new Object();

        // Prepare gateway rules.
        Set<WebFlowRule> rules = new HashSet<WebFlowRule>();
        final String routeId1 = "my_test_route_F&@";
        final String api1 = "my_test_route_E5K";
        final String headerName = "X-Sentinel-Flag";
        final String paramName = "p";

        String nameEquals = "Wow";
        String nameContains = "warn";
        String valueRegex = "\\d+";
        WebFlowRule routeRule1 = new WebFlowRule(routeId1)
                .setCount(10d)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER)
                .setMaxQueueingTimeoutMs(600)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HEADER)
                        .setFieldName(headerName)
                        .setPattern(nameEquals)
                        .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_EXACT)
                );
        WebFlowRule routeRule2 = new WebFlowRule(routeId1)
                .setCount(20d)
                .setBurst(5)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName(paramName)
                        .setPattern(nameContains)
                        .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_CONTAINS)
                );
        WebFlowRule apiRule1 = new WebFlowRule(api1)
                .setResourceMode(SentinelWebFlowConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(5d)
                .setParamItem(new WebParamItem()
                        .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName(paramName)
                        .setPattern(valueRegex)
                        .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_REGEX)
                );
        rules.add(routeRule1);
        rules.add(routeRule2);
        rules.add(apiRule1);
        WebFlowRuleManager.loadRules(rules);

        mockSingleHeader(itemParser, headerName, nameEquals);
        mockSingleUrlParam(itemParser, paramName, nameContains);


        mockSingleHeader(itemParser, headerName, nameEquals + "_foo");
        mockSingleUrlParam(itemParser, paramName, nameContains + "_foo");


        mockSingleHeader(itemParser, headerName, "foo");
        mockSingleUrlParam(itemParser, paramName, "foo");


        mockSingleUrlParam(itemParser, paramName, "23");


        mockSingleUrlParam(itemParser, paramName, "some233");

    }

    private void mockClientHostAddress(/*@Mock*/ RequestItemParser parser, String address) {
        when(parser.getRemoteAddress(any())).thenReturn(address);
    }

    private void mockHeaders(/*@Mock*/ RequestItemParser parser, Map<String, String> headerMap) {
        for (Map.Entry<String, String> e : headerMap.entrySet()) {
            when(parser.getHeader(any(), eq(e.getKey()))).thenReturn(e.getValue());
        }
    }

    private void mockUrlParams(/*@Mock*/ RequestItemParser parser, Map<String, String> paramMap) {
        for (Map.Entry<String, String> e : paramMap.entrySet()) {
            when(parser.getUrlParam(any(), eq(e.getKey()))).thenReturn(e.getValue());
        }
    }

    private void mockSingleUrlParam(/*@Mock*/ RequestItemParser parser, String key, String value) {
        when(parser.getUrlParam(any(), eq(key))).thenReturn(value);
    }

    private void mockSingleHeader(/*@Mock*/ RequestItemParser parser, String key, String value) {
        when(parser.getHeader(any(), eq(key))).thenReturn(value);
    }

    private void mockSingleCookie(/*@Mock*/ RequestItemParser parser, String key, String value) {
        when(parser.getCookieValue(any(), eq(key))).thenReturn(value);
    }

    class MockRequestItemParser implements RequestItemParser<Object> {
        @Override
        public Object parseRequestItem(Object request, WebParamItem paramItem) {
            return null;
        }
        @Override
        public Predicate<WebParamItem> getParamItemPredicate() {
            return null;
        }

        @Override
        public String getPath(Object request) {
            return null;
        }

        @Override
        public String getRemoteAddress(Object request) {
            return null;
        }

        @Override
        public String getHeader(Object request, String key) {
            return null;
        }

        @Override
        public String getUrlParam(Object request, String paramName) {
            return null;
        }

        @Override
        public String getCookieValue(Object request, String cookieName) {
            return null;
        }

        @Override
        public String getBodyValue(Object request, String bodyName) {
            return null;
        }

        @Override
        public String getPathValue(Object request, String pathName) {
            return null;
        }
    }

}
