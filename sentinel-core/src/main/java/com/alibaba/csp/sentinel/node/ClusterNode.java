/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.node;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.ResourceTypeConstants;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.AssertUtil;

/**
 * <p>
 * This class stores summary runtime statistics of the resource, including rt, thread count, qps
 * and so on. Same resource shares the same {@link ClusterNode} globally, no matter in which
 * {@link com.alibaba.csp.sentinel.context.Context}.
 * </p>
 * <p>
 * To distinguish invocation from different origin (declared in
 * {@link ContextUtil#enter(String name, String origin)}),
 * one {@link ClusterNode} holds an {@link #originCountMap}, this map holds {@link StatisticNode}
 * of different origin. Use {@link #getOrCreateOriginNode(String)} to get {@link Node} of the specific
 * origin.<br/>
 * Note that 'origin' usually is Service Consumer's app name.
 * </p>
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class ClusterNode extends StatisticNode {

    private final String name;
    private final int resourceType;
    private final EntryType trafficType;

    public ClusterNode(String name) {
        this(name, ResourceTypeConstants.COMMON, EntryType.IN);
    }

    public ClusterNode(String name, int resourceType) {
        this(name, resourceType, EntryType.IN);
    }

    public ClusterNode(String name, int resourceType, EntryType trafficType) {
        AssertUtil.notEmpty(name, "name cannot be empty");
        this.name = name;
        this.resourceType = resourceType;
        this.trafficType = trafficType;
    }

    /**
     * <p>The origin map holds the pair: (origin, originNode) for one specific resource.</p>
     * <p>
     * The longer the application runs, the more stable this mapping will become.
     * So we didn't use concurrent map here, but a lock, as this lock only happens
     * at the very beginning while concurrent map will hold the lock all the time.
     * </p>
     */
    private Map<String, StatisticNode> originCountMap = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Get resource name of the resource node.
     *
     * @return resource name
     * @since 1.7.0
     */
    public String getName() {
        return name;
    }

    /**
     * Get classification (type) of the resource.
     *
     * @return resource type
     * @since 1.7.0
     */
    public int getResourceType() {
        return resourceType;
    }

    /**
     * <p>Get the {@link StatisticNode} of the specific origin. The origin name is carried in the entrance context.</p>
     * <p>If the statistic node for given origin is absent, a new {@link StatisticNode} for the origin will be created.
     * Note that the origin node amount for this resource should not exceed the {@code maxOriginAmount} configured in
     * {@code SentinelConfig}, or statistic nodes for further new origins will not be created (return null instead)</p>
     *
     * @param origin The caller's name, which is designated in the {@code parameter} parameter
     *               {@link ContextUtil#enter(String name, String origin)}.
     * @return the {@link Node} of the specific origin
     */
    public Node getOrCreateOriginNode(String origin) {
        StatisticNode statisticNode = originCountMap.get(origin);
        if (statisticNode == null) {
            int maxOriginLimit = SentinelConfig.maxOriginAmount();
            if (originCountMap.size() >= maxOriginLimit) {
                return null;
            }

            lock.lock();
            try {
                statisticNode = originCountMap.get(origin);
                if (statisticNode == null) {
                    if (originCountMap.size() >= maxOriginLimit) {
                        return null;
                    }
                    // The node is absent, create a new node for the origin.
                    statisticNode = new StatisticNode();
                    HashMap<String, StatisticNode> newMap = new HashMap<>(originCountMap.size() + 1);
                    newMap.putAll(originCountMap);
                    newMap.put(origin, statisticNode);
                    originCountMap = newMap;
                }
            } finally {
                lock.unlock();
            }
        }
        return statisticNode;
    }

    /**
     * Clear the origin map.
     *
     * @since 1.7.1
     */
    public void clearOriginMap() {
        lock.lock();
        try {
            this.originCountMap = new HashMap<String, StatisticNode>(16);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, StatisticNode> getOriginCountMap() {
        return originCountMap;
    }

    /**
     * Add exception count only when given {@code throwable} is not a {@link BlockException}.
     *
     * @param throwable target exception
     * @param count     count to add
     */
    public void trace(Throwable throwable, int count) {
        if (count <= 0) {
            return;
        }
        if (!BlockException.isBlockException(throwable)) {
            this.increaseExceptionQps(count);
        }
    }

    public EntryType getTrafficType() {
        return trafficType;
    }
}
