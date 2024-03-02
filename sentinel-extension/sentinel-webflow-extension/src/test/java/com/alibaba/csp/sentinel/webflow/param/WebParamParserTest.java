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

import com.alibaba.csp.sentinel.util.function.Predicate;
import com.alibaba.csp.sentinel.webflow.SentinelWebFlowConstants;
import com.alibaba.csp.sentinel.webflow.rule.WebFlowRule;
import com.alibaba.csp.sentinel.webflow.rule.WebFlowRuleConverter;
import com.alibaba.csp.sentinel.webflow.rule.WebFlowRuleManager;
import com.alibaba.csp.sentinel.webflow.rule.WebParamItem;
import org.apache.commons.collections4.SetUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;

public class WebParamParserTest {

    private final Predicate<WebFlowRule> routeIdPredicate = new Predicate<WebFlowRule>() {
        @Override
        public boolean test(WebFlowRule e) {
            return e.getResourceMode() == SentinelWebFlowConstants.RESOURCE_MODE_INTERFACE_ID;
        }
    };

    private static WebParamParser<Object> paramParser;

    @BeforeClass
    public static void setUp() {
        RequestItemParser<Object> itemParser = new MockRequestItemParser();
        paramParser = new WebParamParser<>(itemParser);
    }

    private static void updateConvertedParamKey(WebFlowRule rule) {
        try {
            String paramKey = WebFlowRuleConverter.generateParamKeyForWebRule(rule);
            Method setConvertedParamKeyMethod = rule.getParamItem().getClass()
                    .getDeclaredMethod("setConvertedParamKey", String.class);
            setConvertedParamKeyMethod.setAccessible(true);
            setConvertedParamKeyMethod.invoke(rule.getParamItem(), paramKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testParseParameterFor_empty() {
        Map<String, Object> param = paramParser.parseParameterFor("", new Object(), routeIdPredicate);
        assertEquals(new HashMap<>(), param);

        param = paramParser.parseParameterFor("/a", new Object(), routeIdPredicate);
        assertEquals(new HashMap<>(), param);
    }

    @Test
    public void testParseParameterFor_ClientIp() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30001L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_CLIENT_IP)
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "1.2.3.4"), param);
        }
    }

    @Test
    public void testParseParameterFor_Host() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30002L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HOST)
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "sentinel_host"), param);
        }
    }

    @Test
    public void testParseParameterFor_Header() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30003L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_HEADER)
                            .setFieldName("header_key")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "header_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_Cookie() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30005L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_COOKIE)
                            .setFieldName("cookie_key")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "cookie_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_UrlParam() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30004L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                            .setFieldName("url_key")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "url_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_UrlParam_Exact() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30004L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                            .setFieldName("url_key")
                            .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_EXACT)
                            .setPattern("url_val")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "url_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_UrlParam_Contains() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30004L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                            .setFieldName("url_key")
                            .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_CONTAINS)
                            .setPattern("url_")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "url_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_UrlParam_Regex() {
        String pattern = "url_v.*";
        ParamRegexCache.addRegexPattern(pattern);
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30004L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                            .setFieldName("url_key")
                            .setMatchStrategy(SentinelWebFlowConstants.PARAM_MATCH_STRATEGY_REGEX)
                            .setPattern(pattern)
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "url_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_BodyParam() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30006L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_BODY_PARAM)
                            .setFieldName("body_key")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "body_val"), param);
        }
    }

    @Test
    public void testParseParameterFor_PathParam() {
        try (MockedStatic<WebFlowRuleManager> mocked = mockStatic(WebFlowRuleManager.class)) {
            WebFlowRule rule = new WebFlowRule("/WebParamParserTest")
                    .setId(30007L)
                    .setCount(5d)
                    .setIntervalMs(1)
                    .setParamItem(new WebParamItem()
                            .setParseStrategy(SentinelWebFlowConstants.PARAM_PARSE_STRATEGY_PATH_PARAM)
                            .setFieldName("path_key")
                    );
            updateConvertedParamKey(rule);

            mocked.when(() -> WebFlowRuleManager.getRulesForResource(anyString()))
                    .thenReturn(SetUtils.hashSet(rule));

            Map<String, Object> param = paramParser.parseParameterFor(
                    "/WebParamParserTest",
                    new Object(),
                    routeIdPredicate
            );
            String key = rule.getParamItem().getConvertedParamKey();
            assertEquals(mapOf(key, "path_val"), param);
        }
    }

    private Map<String, String> mapOf(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    static class MockRequestItemParser implements RequestItemParser<Object> {
        private Map<String, String> headers = new HashMap<String, String>() {{
            put("Host", "sentinel_host");
            put("header_key", "header_val");
        }};

        private Map<String, String> urlParam = new HashMap<String, String>() {{
            put("url_key", "url_val");
        }};

        private Map<String, String> cookieParam = new HashMap<String, String>() {{
            put("cookie_key", "cookie_val");
        }};

        private Map<String, String> bodyParam = new HashMap<String, String>() {{
            put("body_key", "body_val");
        }};

        private Map<String, String> pathParam = new HashMap<String, String>() {{
            put("path_key", "path_val");
        }};

        @Override
        public String getPath(Object request) {
            return null;
        }

        @Override
        public String getRemoteAddress(Object request) {
            return "1.2.3.4";
        }

        @Override
        public String getHeader(Object request, String key) {
            return headers.get(key);
        }

        @Override
        public String getUrlParam(Object request, String paramName) {
            return urlParam.get(paramName);
        }

        @Override
        public String getCookieValue(Object request, String cookieName) {
            return cookieParam.get(cookieName);
        }

        @Override
        public String getBodyValue(Object request, String bodyName) {
            return bodyParam.get(bodyName);
        }

        @Override
        public String getPathValue(Object request, String pathName) {
            return pathParam.get(pathName);
        }
    }

}
