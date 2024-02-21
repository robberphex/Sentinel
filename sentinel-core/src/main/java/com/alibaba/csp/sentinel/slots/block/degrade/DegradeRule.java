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
package com.alibaba.csp.sentinel.slots.block.degrade;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;

import java.util.Objects;

/**
 * <p>
 * Circuit breaking can be useful when the resources are in an unstable state (e.g. slow or error triggered).
 * There are several ways to measure whether a resource is stable or not:
 * </p>
 * <ul>
 * <li>
 * Average response time ({@code DEGRADE_GRADE_RT}): When
 * the average RT exceeds the threshold ('count' in 'DegradeRule', in milliseconds), the
 * resource enters a quasi-degraded state. If the RT of next coming 5
 * requests still exceed this threshold, this resource will be downgraded, which
 * means that in the next time window (defined in 'timeWindow', in seconds) all the
 * access to this resource will be blocked.
 * </li>
 * <li>
 * Error Ratio: Circuit breaking by the error ratio (error count / total completed count).
 * </li>
 * <li>
 * Error Count: Circuit breaking by the number of exceptions.
 * </li>
 * </ul>
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class DegradeRule extends AbstractRule {

    public DegradeRule() {}

    public DegradeRule(String resourceName) {
        setResource(resourceName);
    }

    /**
     * Circuit breaking strategy (0: average RT, 1: exception ratio, 2: exception count).
     */
    private int grade = RuleConstant.DEGRADE_GRADE_RT;

    /**
     * Threshold count. The exact meaning depends on the field of grade.
     * <ul>
     *     <li>In average RT mode, it means the maximum response time(RT) in milliseconds.</li>
     *     <li>In exception ratio mode, it means exception ratio which between 0.0 and 1.0.</li>
     *     <li>In exception count mode, it means exception count</li>
     * <ul/>
     */
    private double count;

    /**
     * Recovery timeout (in seconds) when circuit breaker opens. After the timeout, the circuit breaker will
     * transform to half-open state for trying a few requests.
     */
    private int timeWindow;

    /**
     * Minimum number of requests (in an active statistic time span) that can trigger circuit breaking.
     *
     * @since 1.7.0
     */
    private int minRequestAmount = RuleConstant.DEGRADE_DEFAULT_MIN_REQUEST_AMOUNT;

    /**
     * The threshold of slow request ratio in RT mode.
     *
     * @since 1.8.0
     */
    private double slowRatioThreshold = 1.0d;

    /**
     * The interval statistics duration in millisecond.
     *
     * @since 1.8.0
     */
    private int statIntervalMs = 1000;

    private int halfOpenBaseAmountPerStep = 5;
    private int halfOpenRecoveryStepNum = 1;

    public int getGrade() {
        return grade;
    }

    public DegradeRule setGrade(int grade) {
        this.grade = grade;
        return this;
    }

    public double getCount() {
        return count;
    }

    public DegradeRule setCount(double count) {
        this.count = count;
        return this;
    }

    public int getTimeWindow() {
        return timeWindow;
    }

    public DegradeRule setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
        return this;
    }

    public int getMinRequestAmount() {
        return minRequestAmount;
    }

    public DegradeRule setMinRequestAmount(int minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
        return this;
    }

    public double getSlowRatioThreshold() {
        return slowRatioThreshold;
    }

    public DegradeRule setSlowRatioThreshold(double slowRatioThreshold) {
        this.slowRatioThreshold = slowRatioThreshold;
        return this;
    }

    public int getStatIntervalMs() {
        return statIntervalMs;
    }

    public DegradeRule setStatIntervalMs(int statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
        return this;
    }

    public int getHalfOpenBaseAmountPerStep() {
        return halfOpenBaseAmountPerStep;
    }

    public DegradeRule setHalfOpenBaseAmountPerStep(int halfOpenBaseAmountPerStep) {
        this.halfOpenBaseAmountPerStep = halfOpenBaseAmountPerStep;
        return this;
    }

    public int getHalfOpenRecoveryStepNum() {
        return halfOpenRecoveryStepNum;
    }

    public DegradeRule setHalfOpenRecoveryStepNum(int halfOpenRecoveryStepNum) {
        this.halfOpenRecoveryStepNum = halfOpenRecoveryStepNum;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        DegradeRule that = (DegradeRule) o;

        if (grade != that.grade) { return false; }
        if (Double.compare(that.count, count) != 0) { return false; }
        if (timeWindow != that.timeWindow) { return false; }
        if (minRequestAmount != that.minRequestAmount) { return false; }
        if (Double.compare(that.slowRatioThreshold, slowRatioThreshold) != 0) { return false; }
        if (statIntervalMs != that.statIntervalMs) { return false; }
        if (halfOpenBaseAmountPerStep != that.halfOpenBaseAmountPerStep) { return false; }
        return halfOpenRecoveryStepNum == that.halfOpenRecoveryStepNum;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + grade;
        temp = Double.doubleToLongBits(count);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + timeWindow;
        result = 31 * result + minRequestAmount;
        temp = Double.doubleToLongBits(slowRatioThreshold);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + statIntervalMs;
        result = 31 * result + halfOpenBaseAmountPerStep;
        result = 31 * result + halfOpenRecoveryStepNum;
        return result;
    }

    @Override
    public String toString() {
        return "DegradeRule{" +
            "resource=" + getResource() +
            ", grade=" + grade +
            ", count=" + count +
            ", limitApp=" + getLimitApp() +
            ", timeWindow=" + timeWindow +
            ", minRequestAmount=" + minRequestAmount +
            ", slowRatioThreshold=" + slowRatioThreshold +
            ", statIntervalMs=" + statIntervalMs +
            ", halfOpenBaseAmountPerStep=" + halfOpenBaseAmountPerStep +
            ", halfOpenRecoveryStepNum=" + halfOpenRecoveryStepNum +
            '}';
    }
}
