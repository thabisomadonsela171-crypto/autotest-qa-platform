/* =========================================================
   AUTOTEST QA PLATFORM
   PHASE 5 - TEST INTELLIGENCE
   PREMIUM PERFORMANCE ANALYTICS DASHBOARD
   ========================================================= */

"use strict";

/* ================= API CONFIGURATION ================= */

const PERFORMANCE_API = "/api/analytics/performance";
const TRENDS_API = "/api/analytics/trends";

/* ================= PERFORMANCE SETTINGS ================= */

const FAST_THRESHOLD = 1000;
const SLOW_THRESHOLD = 3000;
const MAX_HISTORY_RECORDS = 50;

/* ================= DASHBOARD STATE ================= */

let performanceData = null;
let trendData = null;
let analyticsLoading = false;
let analyticsInitialised = false;
let analyticsResizeTimeout = null;

/* ================= DOM HELPERS ================= */

function getAnalyticsElement(id) {
    return document.getElementById(id);
}

function setAnalyticsText(id, value) {
    const element = getAnalyticsElement(id);

    if (element) {
        element.textContent = value;
    }
}

function formatNumber(value, decimals = 0) {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "0";
    }

    return number.toLocaleString("en-ZA", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals
    });
}

function formatDuration(milliseconds) {
    const duration = Number(milliseconds);

    if (!Number.isFinite(duration) || duration < 0) {
        return "0 ms";
    }

    if (duration >= 60000) {
        return `${(duration / 60000).toFixed(2)} min`;
    }

    if (duration >= 1000) {
        return `${(duration / 1000).toFixed(2)} sec`;
    }

    return `${Math.round(duration)} ms`;
}

function formatPercentage(value) {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "0.00%";
    }

    return `${number.toFixed(2)}%`;
}

/* ================= STATUS MANAGEMENT ================= */

function setAnalyticsStatus(message, isError = false) {
    const status = getAnalyticsElement("analyticsStatus");

    if (!status) {
        return;
    }

    status.textContent = message;

    status.classList.toggle("error", isError);
}

/* ================= API COMMUNICATION ================= */

async function fetchAnalyticsEndpoint(endpoint) {

    const response = await fetch(endpoint, {
        method: "GET",

        headers: {
            "Accept": "application/json"
        },

        cache: "no-store"
    });

    if (!response.ok) {
        throw new Error(
            `Request failed: ${response.status} ${response.statusText}`
        );
    }

    const data = await response.json();

    if (!data || typeof data !== "object" || Array.isArray(data)) {
        throw new Error("Invalid analytics response received.");
    }

    return data;
}

/* ================= LOAD ANALYTICS ================= */

async function loadPerformanceAnalytics() {

    if (analyticsLoading) {
        return;
    }

    analyticsLoading = true;

    const refreshButton =
        getAnalyticsElement("refreshAnalyticsButton");

    if (refreshButton) {
        refreshButton.disabled = true;
        refreshButton.textContent = "Refreshing...";
        refreshButton.setAttribute("aria-busy", "true");
    }

    setAnalyticsStatus("LOADING PERFORMANCE ANALYTICS...");

    try {

        const [performance, trends] = await Promise.all([

            fetchAnalyticsEndpoint(PERFORMANCE_API),

            fetchAnalyticsEndpoint(TRENDS_API)

        ]);

        performanceData = performance;
        trendData = trends;

        /* Update dashboard components */

        renderPerformanceMetrics(performanceData);

        renderPerformanceTrends(trendData);

        renderSlowTests(performanceData);

        renderPerformanceHistory(performanceData);

        setAnalyticsStatus("ANALYTICS LOADED SUCCESSFULLY");

    } catch (error) {

        console.error(
            "Performance analytics error:",
            error
        );

        setAnalyticsStatus(
            `ANALYTICS ERROR: ${error.message}`,
            true
        );

    } finally {

        analyticsLoading = false;

        if (refreshButton) {

            refreshButton.disabled = false;

            refreshButton.textContent = "Refresh Analytics";

            refreshButton.removeAttribute("aria-busy");
        }
    }
}

/* ================= PERFORMANCE METRICS ================= */

function renderPerformanceMetrics(data) {

    if (!data) {
        return;
    }

    const records = Array.isArray(data.records)
        ? data.records
        : [];

    /* Total recorded tests */

    const totalTests = Number(data.totalTests);

    setAnalyticsText(
        "analyticsTotalTests",
        formatNumber(
            Number.isFinite(totalTests)
                ? totalTests
                : records.length
        )
    );

    /* Average test duration */

    setAnalyticsText(
        "analyticsAverageDuration",
        formatDuration(data.averageDurationMs)
    );

    /* Calculate pass rate if the API does not provide it */

    let passRate = Number(data.passRate);

    if (!Number.isFinite(passRate)) {

        const passed = Number(data.passed) || 0;

        const total = Number(data.totalTests) || 0;

        passRate = total > 0
            ? (passed / total) * 100
            : 0;
    }

    setAnalyticsText(
        "analyticsPassRate",
        formatPercentage(passRate)
    );

    /* Slow test count */

    let slowTestCount = Number(data.slowTestCount);

    if (!Number.isFinite(slowTestCount)) {

        slowTestCount = records.filter(record => {

            return Number(record.durationMs) >= SLOW_THRESHOLD;

        }).length;
    }

    setAnalyticsText(
        "analyticsSlowTests",
        formatNumber(slowTestCount)
    );
}

/* ================= PERFORMANCE TRENDS ================= */

function renderPerformanceTrends(data) {

    if (!data) {
        return;
    }

    const statusElement =
        getAnalyticsElement("analyticsTrendStatus");

    const trendStatus = String(
        data.trendStatus || "INSUFFICIENT_DATA"
    ).toUpperCase();

    if (statusElement) {

        statusElement.textContent = trendStatus;

        statusElement.classList.remove(
            "faster",
            "slower",
            "stable",
            "insufficient-data"
        );

        switch (trendStatus) {

            case "FASTER":

                statusElement.classList.add("faster");

                break;

            case "SLOWER":

                statusElement.classList.add("slower");

                break;

            case "STABLE":

                statusElement.classList.add("stable");

                break;

            default:

                statusElement.classList.add("insufficient-data");

                break;
        }
    }

    let trendMessage =
        "Not enough execution data to compare runs.";

    const executionCount = Number(data.executionCount);

    const percentageChange = Number(data.percentageChange);

    if (
        Number.isFinite(executionCount) &&
        executionCount >= 2 &&
        data.percentageChange !== null &&
        data.percentageChange !== undefined &&
        Number.isFinite(percentageChange)
    ) {

        if (percentageChange > 0) {

            trendMessage =
                `Average execution time increased by ${percentageChange.toFixed(2)}% compared with the previous run.`;

        } else if (percentageChange < 0) {

            trendMessage =
                `Average execution time improved by ${Math.abs(percentageChange).toFixed(2)}% compared with the previous run.`;

        } else {

            trendMessage =
                "Average execution time remained unchanged.";
        }
    }

    setAnalyticsText(
        "analyticsTrendChange",
        trendMessage
    );

    drawPerformanceTrendChart(
        Array.isArray(data.executions)
            ? data.executions
            : []
    );
}

/* ================= PERFORMANCE TREND CHART ================= */

function drawPerformanceTrendChart(executions) {

    const canvas =
        getAnalyticsElement("performanceTrendChart");

    if (!canvas) {
        return;
    }

    const context = canvas.getContext("2d");

    if (!context) {
        return;
    }

    const bounds = canvas.getBoundingClientRect();

    const width = Math.max(
        Math.round(bounds.width),
        300
    );

    const height = Math.max(
        canvas.clientHeight || 260,
        220
    );

    const pixelRatio = Math.max(
        window.devicePixelRatio || 1,
        1
    );

    canvas.width = Math.round(width * pixelRatio);

    canvas.height = Math.round(height * pixelRatio);

    context.setTransform(
        pixelRatio,
        0,
        0,
        pixelRatio,
        0,
        0
    );

    context.clearRect(0, 0, width, height);

    const padding = {
        top: 25,
        right: 22,
        bottom: 45,
        left: 75
    };

    const chartWidth =
        width - padding.left - padding.right;

    const chartHeight =
        height - padding.top - padding.bottom;

    context.font = "11px Arial";

    context.textAlign = "center";

    context.textBaseline = "middle";

    /* Empty chart */

    if (!Array.isArray(executions) || executions.length === 0) {

        context.fillStyle = "#8192aa";

        context.fillText(
            "No execution trend data available",
            width / 2,
            height / 2
        );

        return;
    }

    /* Prepare chart values */

    const values = executions.map(item => {

        const value = Number(item.averageDurationMs);

        return Number.isFinite(value)
            ? Math.max(0, value)
            : 0;
    });

    const maximum = Math.max(...values, 1000);

    const chartMaximum = maximum * 1.2;

    /* Draw horizontal grid lines */

    context.strokeStyle =
        "rgba(148, 163, 184, 0.14)";

    context.lineWidth = 1;

    context.fillStyle = "#8192aa";

    context.textAlign = "right";

    for (let i = 0; i <= 4; i++) {

        const y =
            padding.top + (chartHeight / 4) * i;

        context.beginPath();

        context.moveTo(
            padding.left,
            y
        );

        context.lineTo(
            width - padding.right,
            y
        );

        context.stroke();

        const value =
            chartMaximum - (chartMaximum / 4) * i;

        context.fillText(
            formatDuration(value),
            padding.left - 10,
            y
        );
    }

    /* Calculate chart coordinates */

    const points = values.map((value, index) => {

        const x = values.length === 1
            ? padding.left + chartWidth / 2
            : padding.left +
              (chartWidth / (values.length - 1)) * index;

        const y =
            padding.top +
            chartHeight -
            (value / chartMaximum) * chartHeight;

        return {
            x,
            y,
            value,
            index
        };
    });

    /* Draw gradient area */

    if (points.length > 1) {

        const gradient =
            context.createLinearGradient(
                0,
                padding.top,
                0,
                height - padding.bottom
            );

        gradient.addColorStop(
            0,
            "rgba(0, 229, 255, 0.22)"
        );

        gradient.addColorStop(
            1,
            "rgba(0, 229, 255, 0.005)"
        );

        context.beginPath();

        context.moveTo(
            points[0].x,
            height - padding.bottom
        );

        points.forEach(point => {

            context.lineTo(
                point.x,
                point.y
            );
        });

        context.lineTo(
            points[points.length - 1].x,
            height - padding.bottom
        );

        context.closePath();

        context.fillStyle = gradient;

        context.fill();
    }

    /* Draw trend line */

    if (points.length > 1) {

        context.beginPath();

        points.forEach((point, index) => {

            if (index === 0) {

                context.moveTo(
                    point.x,
                    point.y
                );

            } else {

                context.lineTo(
                    point.x,
                    point.y
                );
            }
        });

        context.strokeStyle = "#00e5ff";

        context.lineWidth = 3;

        context.lineJoin = "round";

        context.lineCap = "round";

        context.shadowColor =
            "rgba(0, 229, 255, 0.35)";

        context.shadowBlur = 8;

        context.stroke();

        context.shadowBlur = 0;
    }

    /* Draw data points */

    points.forEach(point => {

        context.beginPath();

        context.arc(
            point.x,
            point.y,
            5,
            0,
            Math.PI * 2
        );

        context.fillStyle = "#00e5ff";

        context.fill();

        context.strokeStyle = "#0f1b2d";

        context.lineWidth = 2;

        context.stroke();

        context.fillStyle = "#e2e8f0";

        context.textAlign = "center";

        context.fillText(
            `Run ${point.index + 1}`,
            point.x,
            height - 18
        );
    });
}

/* ================= SLOW TEST DETECTION ================= */

function renderSlowTests(data) {

    const container =
        getAnalyticsElement("slowTestsList");

    if (!container) {
        return;
    }

    container.replaceChildren();

    const records = Array.isArray(data.records)
        ? data.records
        : [];

    let slowTests = Array.isArray(data.slowTests)
        ? data.slowTests
        : [];

    /*
     * If the API does not provide a separate slowTests
     * array, derive it from the individual test records.
     */

    if (slowTests.length === 0 && records.length > 0) {

        slowTests = records.filter(test => {

            return Number(test.durationMs) >= SLOW_THRESHOLD;

        });
    }

    if (slowTests.length === 0) {

        const empty = document.createElement("div");

        empty.className = "analytics-empty";

        empty.textContent =
            "No slow tests detected. Excellent performance!";

        container.appendChild(empty);

        return;
    }

    /* Sort slowest tests first */

    const sortedTests = [...slowTests].sort(
        (a, b) =>
            Number(b.durationMs || 0) -
            Number(a.durationMs || 0)
    );

    sortedTests.slice(0, 10).forEach(test => {

        const item = document.createElement("div");

        item.className = "slow-test-item";

        /* Test name */

        const name = document.createElement("div");

        name.className = "slow-test-name";

        name.textContent =
            test.testName || "Unnamed test";

        /* Test details */

        const details = document.createElement("div");

        details.className = "slow-test-details";

        const suite = document.createElement("span");

        suite.textContent =
            test.suiteName || "Unknown suite";

        const duration = document.createElement("span");

        duration.className = "slow-test-duration";

        duration.textContent =
            formatDuration(test.durationMs);

        details.append(
            suite,
            duration
        );

        item.append(
            name,
            details
        );

        container.appendChild(item);
    });
}

/* ================= TEST PERFORMANCE CLASSIFICATION ================= */

function getTestPerformance(durationMs) {

    const duration = Number(durationMs) || 0;

    if (duration >= SLOW_THRESHOLD) {

        return {
            label: "SLOW",
            className: "slow",
            color: "#fb923c",
            background: "rgba(251, 146, 60, 0.12)"
        };
    }

    if (duration >= FAST_THRESHOLD) {

        return {
            label: "NORMAL",
            className: "normal",
            color: "#38bdf8",
            background: "rgba(56, 189, 248, 0.12)"
        };
    }

    return {
        label: "FAST",
        className: "fast",
        color: "#4ade80",
        background: "rgba(74, 222, 128, 0.12)"
    };
}

/* ================= PERFORMANCE BAR ================= */

function createPerformanceIndicator(durationMs) {

    const performance =
        getTestPerformance(durationMs);

    const duration = Math.max(
        0,
        Number(durationMs) || 0
    );

    const cell = document.createElement("td");

    cell.className = "analytics-performance-cell";

    const wrapper = document.createElement("div");

    wrapper.className = "analytics-performance-wrapper";

    const badge = document.createElement("span");

    badge.className =
        `analytics-performance-badge ${performance.className}`;

    badge.textContent = performance.label;

    const track = document.createElement("div");

    track.className = "analytics-performance-track";

    const fill = document.createElement("div");

    fill.className = "analytics-performance-fill";

    const percentage = Math.min(
        (duration / 5000) * 100,
        100
    );

    fill.style.width = `${percentage}%`;

    fill.style.background = performance.color;

    fill.style.boxShadow =
        `0 0 9px ${performance.color}66`;

    track.appendChild(fill);

    wrapper.append(
        badge,
        track
    );

    cell.appendChild(wrapper);

    return cell;
}

/* ================= PERFORMANCE HISTORY ================= */

function renderPerformanceHistory(data) {

    const table =
        getAnalyticsElement("analyticsHistoryTable");

    if (!table || table.tagName !== "TABLE") {

        console.error(
            "Individual performance history table not found. Check the analyticsHistoryTable ID in index.html."
        );

        return;
    }

    let tableHead = table.querySelector("thead");

    let tableBody = table.querySelector("tbody");

    /* Create table sections if missing */

    if (!tableHead) {

        tableHead = document.createElement("thead");

        table.insertBefore(
            tableHead,
            table.firstChild
        );
    }

    if (!tableBody) {

        tableBody = document.createElement("tbody");

        table.appendChild(tableBody);
    }

    /* Clear old headings and rows */

    tableHead.replaceChildren();

    tableBody.replaceChildren();

    const headings = [
        "TEST NAME",
        "TEST CLASS",
        "SUITE",
        "STATUS",
        "DURATION",
        "PERFORMANCE"
    ];

    /* Create table header */

    const headerRow = document.createElement("tr");

    headings.forEach(heading => {

        const th = document.createElement("th");

        th.textContent = heading;

        headerRow.appendChild(th);
    });

    tableHead.appendChild(headerRow);

    /* Read real API records */

    const records = Array.isArray(data.records)
        ? data.records
        : [];

    setAnalyticsText(
        "analyticsRecordCount",
        `${formatNumber(records.length)} RECORDS`
    );

    /* Empty history state */

    if (records.length === 0) {

        const row = document.createElement("tr");

        const cell = document.createElement("td");

        cell.colSpan = headings.length;

        cell.textContent =
            "No individual test performance records found.";

        cell.className = "analytics-empty-state";

        row.appendChild(cell);

        tableBody.appendChild(row);

        return;
    }

    /* Newest records first */

    const recentRecords = [...records]
        .reverse()
        .slice(0, MAX_HISTORY_RECORDS);

    /* Create individual test rows */

    recentRecords.forEach(record => {

        const row = document.createElement("tr");

        /* Test name */

        const testName = document.createElement("td");

        testName.className = "analytics-test-name-cell";

        testName.textContent =
            record.testName || "Unknown test";

        /* Test class */

        const testClass = document.createElement("td");

        testClass.className = "analytics-test-class-cell";

        testClass.textContent =
            record.testClass ||
            record.className ||
            "Unknown class";

        /* Suite */

        const suite = document.createElement("td");

        suite.className = "analytics-suite-cell";

        suite.textContent =
            record.suiteName ||
            record.suite ||
            "Unknown suite";

        /* Test status */

        const statusCell = document.createElement("td");

        statusCell.className = "analytics-status-cell";

        const badge = document.createElement("span");

        const status = String(
            record.status || "UNKNOWN"
        ).toUpperCase();

        badge.className = "analytics-result-badge";

        if (status === "PASS" || status === "PASSED") {

            badge.classList.add("pass");

        } else if (status === "FAIL" || status === "FAILED") {

            badge.classList.add("fail");

        } else {

            badge.classList.add("skip");
        }

        badge.textContent = status;

        statusCell.appendChild(badge);

        /* Duration */

        const duration = document.createElement("td");

        duration.className = "analytics-duration-cell";

        duration.textContent =
            formatDuration(record.durationMs);

        /* Performance indicator */

        const performanceCell =
            createPerformanceIndicator(record.durationMs);

        /* Add all six columns */

        row.append(
            testName,
            testClass,
            suite,
            statusCell,
            duration,
            performanceCell
        );

        tableBody.appendChild(row);
    });
}

/* ================= EVENT LISTENERS ================= */

function initialisePerformanceAnalytics() {

    if (analyticsInitialised) {
        return;
    }

    analyticsInitialised = true;

    const refreshButton =
        getAnalyticsElement("refreshAnalyticsButton");

    if (refreshButton) {

        refreshButton.addEventListener(
            "click",
            loadPerformanceAnalytics
        );
    }

    loadPerformanceAnalytics();
}

/* ================= RESPONSIVE CHART ================= */

window.addEventListener("resize", () => {

    if (!trendData) {
        return;
    }

    window.clearTimeout(analyticsResizeTimeout);

    analyticsResizeTimeout = window.setTimeout(() => {

        drawPerformanceTrendChart(
            Array.isArray(trendData.executions)
                ? trendData.executions
                : []
        );

    }, 150);
});

/* ================= START DASHBOARD ================= */

if (document.readyState === "loading") {

    document.addEventListener(
        "DOMContentLoaded",
        initialisePerformanceAnalytics,
        { once: true }
    );

} else {

    initialisePerformanceAnalytics();
}