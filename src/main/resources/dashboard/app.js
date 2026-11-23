
const API_URL = "/api/dashboard";
const LIVE_API_URL = "/api/executions/live";
const STATUS_API_URL = "/api/executions/status";

const refreshButton = document.getElementById("refreshButton");
const errorMessage = document.getElementById("errorMessage");

// ==========================================
// GENERAL HELPER FUNCTIONS
// ==========================================

function formatDuration(milliseconds) {
    const seconds = Number(milliseconds || 0) / 1000;
    return seconds.toFixed(2) + " sec";
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => {
        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return entities[character];
    });
}

function showError(message) {
    if (!errorMessage) return;

    errorMessage.textContent = message;
    errorMessage.classList.remove("hidden");
}

function hideError() {
    if (!errorMessage) return;

    errorMessage.classList.add("hidden");
}

// ==========================================
// REPORTS CENTRE SUMMARY
// ==========================================

function updateReportsSummary(data) {

    const setReportValue = (elementId, value) => {

        const element = document.getElementById(elementId);

        if (element) {
            element.textContent = value;
        }
    };

    // Total recorded executions
    setReportValue(
        "reportTotalExecutions",
        Number(data.totalExecutions ?? 0).toLocaleString()
    );

    // Total test cases
    setReportValue(
        "reportTotalTests",
        Number(data.totalTests ?? 0).toLocaleString()
    );

    // Passed test cases
    setReportValue(
        "reportPassedTests",
        Number(data.passed ?? 0).toLocaleString()
    );

    // Failed test cases
    setReportValue(
        "reportFailedTests",
        Number(data.failed ?? 0).toLocaleString()
    );

    // Overall pass rate
    setReportValue(
        "reportPassRate",
        Number(data.passRate ?? 0).toFixed(2) + "%"
    );

    // Average execution duration
    setReportValue(
        "reportAverageDuration",
        formatDuration(data.averageDuration)
    );
}

// ==========================================
// PHASE 2: REPORTS CENTRE ANALYTICS
// ==========================================

function updateReportsAnalytics(data) {

    const passed = Number(data.passed || 0);
    const failed = Number(data.failed || 0);
    const skipped = Number(data.skipped || 0);

    const total = passed + failed + skipped;

    const setText = (id, value) => {

        const element = document.getElementById(id);

        if (element) {
            element.textContent = value;
        }
    };

    const setBarWidth = (id, value) => {

        const element = document.getElementById(id);

        if (!element) {
            return;
        }

        const percentage = total > 0
            ? (value / total) * 100
            : 0;

        element.style.width = percentage + "%";
    };

    // Update result counts.
    setText(
        "chartPassedCount",
        passed.toLocaleString()
    );

    setText(
        "chartFailedCount",
        failed.toLocaleString()
    );

    setText(
        "chartSkippedCount",
        skipped.toLocaleString()
    );

    setText(
        "chartTotalCount",
        total.toLocaleString()
    );

    // Update result bars.
    setBarWidth("chartPassedBar", passed);
    setBarWidth("chartFailedBar", failed);
    setBarWidth("chartSkippedBar", skipped);

    // Update execution trends.
    renderReportsExecutionTrend(data.history || []);
}

// ==========================================
// PHASE 2: EXECUTION TREND CHART
// ==========================================

function renderReportsExecutionTrend(history) {

    const chart = document.getElementById(
        "reportsExecutionTrend"
    );

    if (!chart) {
        return;
    }

    if (!Array.isArray(history) || history.length === 0) {

        chart.innerHTML = `
            <p class="chart-empty-message">
                No execution history available yet.
            </p>
        `;

        return;
    }

    // Display the eight most recent executions.
    const recentExecutions = history.slice(-8);

    const maximumTotal = Math.max(
        ...recentExecutions.map(execution =>
            Number(execution.total || 0)
        ),
        1
    );

    chart.innerHTML = recentExecutions.map((execution, index) => {

        const passed = Number(execution.passed || 0);
        const failed = Number(execution.failed || 0);
        const skipped = Number(execution.skipped || 0);
        const total = Number(execution.total || 0);

        const barHeight = Math.max(
            4,
            (total / maximumTotal) * 150
        );

        const passedHeight = total > 0
            ? (passed / total) * 100
            : 0;

        const failedHeight = total > 0
            ? (failed / total) * 100
            : 0;

        const skippedHeight = total > 0
            ? (skipped / total) * 100
            : 0;

        const executionLabel = escapeHtml(
            String(execution.executionId || `Run ${index + 1}`)
                .slice(0, 8)
        );

        return `
            <div class="trend-column">

                <strong class="trend-total">
                    ${total}
                </strong>

                <div class="trend-stack"
                     style="height: ${barHeight}px"
                     title="Passed: ${passed}, Failed: ${failed}, Skipped: ${skipped}">

                    <div class="trend-segment trend-passed"
                         style="height: ${passedHeight}%">
                    </div>

                    <div class="trend-segment trend-failed"
                         style="height: ${failedHeight}%">
                    </div>

                    <div class="trend-segment trend-skipped"
                         style="height: ${skippedHeight}%">
                    </div>

                </div>

                <span class="trend-label">
                    ${executionLabel}
                </span>

            </div>
        `;

    }).join("");
}

// ==========================================
// DASHBOARD OVERVIEW
// ==========================================

function updateDashboard(data) {

    // Update Reports Centre summary.
    updateReportsSummary(data);

    // Update Reports Centre analytics.
    updateReportsAnalytics(data);

    document.getElementById("totalExecutions").textContent =
        data.totalExecutions;

    document.getElementById("totalTests").textContent =
        data.totalTests;

    document.getElementById("passedTests").textContent =
        data.passed;

    document.getElementById("failedTests").textContent =
        data.failed;

    document.getElementById("passRate").textContent =
        Number(data.passRate).toFixed(2) + "%";

    document.getElementById("passLegend").textContent =
        data.passed;

    document.getElementById("failLegend").textContent =
        data.failed;

    document.getElementById("skipLegend").textContent =
        data.skipped;

    document.getElementById("skippedTests").textContent =
        data.skipped;

    document.getElementById("averageDuration").textContent =
        formatDuration(data.averageDuration);

    document.getElementById("latestExecution").textContent =
        data.latestExecutionId;

    const ring = document.getElementById("passRing");

    const passRate = Math.min(
        100,
        Math.max(0, Number(data.passRate))
    );

    ring.style.background =
        `conic-gradient(var(--green) ${passRate}%, #26344a ${passRate}%)`;

    const qualityStatus = document.getElementById("qualityStatus");

    qualityStatus.textContent = data.qualityStatus;

    if (data.qualityStatus === "EXCELLENT") {

        qualityStatus.style.color = "var(--green)";
        qualityStatus.style.borderColor = "#176e59";
        qualityStatus.style.background = "#10382f";

    } else {

        qualityStatus.style.color = "var(--yellow)";
        qualityStatus.style.borderColor = "#8b6a29";
        qualityStatus.style.background = "#3b301b";
    }

    let description = "Review the latest automated testing results.";

    if (data.qualityStatus === "EXCELLENT") {

        description =
            "All recorded tests passed without failures or skips.";

    } else if (data.qualityStatus === "NO DATA") {

        description =
            "No test execution history has been recorded yet.";

    } else if (data.failed > 0) {

        description =
            "Some tests failed. Review the execution history.";

    } else if (data.skipped > 0) {

        description =
            "Some tests were skipped. Review the test configuration.";
    }

    document.getElementById("qualityDescription").textContent =
        description;

    document.getElementById("historyCount").textContent =
        data.totalExecutions + " EXECUTIONS";

    renderHistory(data.history || []);

    document.getElementById("lastUpdated").textContent =
        "Last updated: " + new Date().toLocaleTimeString();
}

// ==========================================
// EXECUTION HISTORY
// ==========================================

let executionHistoryData = [];
// ==========================================
// PHASE 3.8: INDIVIDUAL TEST COMPARISON
// ==========================================

let individualTestRecords = [];
let individualTestAnalyticsLoaded = false;

async function loadIndividualTestAnalytics() {

    const message = document.getElementById(
        "individualComparisonMessage"
    );

    try {

        const response = await fetch(
            "/api/analytics/performance",
            {
                method: "GET",
                cache: "no-store"
            }
        );

        if (!response.ok) {
            throw new Error(
                "Performance analytics returned HTTP " +
                response.status
            );
        }

        const data = await response.json();

        if (!data || !Array.isArray(data.records)) {
            throw new Error(
                "The analytics response does not contain a valid records array."
            );
        }

        individualTestRecords = data.records;
        individualTestAnalyticsLoaded = true;

        if (message) {
            message.textContent =
                individualTestRecords.length +
                " individual test records loaded successfully. " +
                "Select two executions and compare their tests.";
        }

        console.info(
            "Phase 3.8: Loaded individual test records:",
            individualTestRecords.length
        );

        return true;

    } catch (error) {

        individualTestAnalyticsLoaded = false;

        console.error(
            "Phase 3.8 analytics loading error:",
            error
        );

        if (message) {
            message.textContent =
                "Could not load individual test records. " +
                "Check the performance analytics endpoint and try again.";
        }

        return false;
    }
}

function getIndividualTestKey(record) {

    const suiteName = String(
        record.suiteName || ""
    ).trim();

    const testClass = String(
        record.testClass || ""
    ).trim();

    const testName = String(
        record.testName || ""
    ).trim();

    if (!testName) {
        return "";
    }

    return JSON.stringify([
        suiteName,
        testClass,
        testName
    ]);
}

function normalizeIndividualStatus(status) {

    const value = String(
        status || "UNKNOWN"
    ).trim().toUpperCase();

    if (value === "PASSED") return "PASS";
    if (value === "FAILED") return "FAIL";
    if (value === "SKIPPED") return "SKIP";

    return value;
}

function getIndividualStatusBadge(status) {

    const normalized = normalizeIndividualStatus(status);

    let cssClass = "unknown";

    if (normalized === "PASS") {
        cssClass = "pass";
    } else if (normalized === "FAIL") {
        cssClass = "fail";
    } else if (normalized === "SKIP") {
        cssClass = "skipped";
    }

    return `
        <span class="individual-status-badge ${cssClass}">
            ${escapeHtml(normalized)}
        </span>
    `;
}

function getIndividualDuration(record) {

    const duration = Number(record.durationMs);

    return Number.isFinite(duration) && duration >= 0
        ? duration
        : 0;
}

function getIndividualComparisonResult(
    firstStatus,
    secondStatus,
    durationDifference
) {

    if (firstStatus !== secondStatus) {
        return {
            label: "STATUS CHANGED",
            cssClass: "status-changed"
        };
    }

    if (durationDifference < 0) {
        return {
            label: "FASTER",
            cssClass: "faster"
        };
    }

    if (durationDifference > 0) {
        return {
            label: "SLOWER",
            cssClass: "slower"
        };
    }

    return {
        label: "MATCHED",
        cssClass: "matched"
    };
}

function updateIndividualComparisonSummary(counts) {

    const values = {
        individualMatchedCount: counts.matched,
        individualStatusChangedCount: counts.statusChanged,
        individualFasterCount: counts.faster,
        individualSlowerCount: counts.slower,
        individualNewCount: counts.newTests,
        individualMissingCount: counts.missing
    };

    Object.entries(values).forEach(([id, value]) => {

        const element = document.getElementById(id);

        if (element) {
            element.textContent = String(value);
        }
    });

    const summary = document.getElementById(
        "individualComparisonSummary"
    );

    if (summary) {
        summary.hidden = false;
    }
}

function createIndividualComparisonRow(
    firstRecord,
    secondRecord
) {

    const record = secondRecord || firstRecord;

    const testName = String(
        record.testName || "Unknown test"
    );

    const testClass = String(
        record.testClass || "Unknown class"
    );

    const firstStatus = firstRecord
        ? normalizeIndividualStatus(firstRecord.status)
        : "NOT PRESENT";

    const secondStatus = secondRecord
        ? normalizeIndividualStatus(secondRecord.status)
        : "NOT PRESENT";

    const firstDuration = firstRecord
        ? getIndividualDuration(firstRecord)
        : null;

    const secondDuration = secondRecord
        ? getIndividualDuration(secondRecord)
        : null;

    let durationText = "N/A";
    let resultLabel = "";
    let resultClass = "";

    if (!firstRecord) {

        resultLabel = "NEW TEST";
        resultClass = "new";

    } else if (!secondRecord) {

        resultLabel = "MISSING TEST";
        resultClass = "missing";

    } else {

        const difference =
            secondDuration - firstDuration;

        if (difference < 0) {
            durationText =
                "-" + formatDuration(Math.abs(difference)) +
                " (faster)";
        } else if (difference > 0) {
            durationText =
                "+" + formatDuration(difference) +
                " (slower)";
        } else {
            durationText = "No change";
        }

        const comparison = getIndividualComparisonResult(
            firstStatus,
            secondStatus,
            difference
        );

        resultLabel = comparison.label;
        resultClass = comparison.cssClass;
    }

    return `
        <tr class="individual-comparison-row ${resultClass}">
            <td>${escapeHtml(testName)}</td>
            <td>${escapeHtml(testClass)}</td>
            <td>
                ${firstRecord
                    ? getIndividualStatusBadge(firstStatus)
                    : '<span class="individual-na">N/A</span>'}
            </td>
            <td>
                ${secondRecord
                    ? getIndividualStatusBadge(secondStatus)
                    : '<span class="individual-na">N/A</span>'}
            </td>
            <td>
                ${firstDuration !== null
                    ? escapeHtml(formatDuration(firstDuration))
                    : "N/A"}
            </td>
            <td>
                ${secondDuration !== null
                    ? escapeHtml(formatDuration(secondDuration))
                    : "N/A"}
            </td>
            <td>${escapeHtml(durationText)}</td>
            <td>
                <span class="individual-result-badge ${resultClass}">
                    ${escapeHtml(resultLabel)}
                </span>
            </td>
        </tr>
    `;
}

async function renderIndividualTestComparison() {

    const selectA = document.getElementById(
        "comparisonExecutionA"
    );

    const selectB = document.getElementById(
        "comparisonExecutionB"
    );

    const message = document.getElementById(
        "individualComparisonMessage"
    );

    const resultsWrapper = document.getElementById(
        "individualComparisonResultsWrapper"
    );

    const resultsTable = document.getElementById(
        "individualComparisonResultsTable"
    );

    const legend = document.getElementById(
        "individualComparisonLegend"
    );

    if (
        !selectA ||
        !selectB ||
        !message ||
        !resultsWrapper ||
        !resultsTable
    ) {
        console.error(
            "Phase 3.8: Required comparison HTML elements were not found."
        );
        return;
    }

    const firstExecutionId = selectA.value;
    const secondExecutionId = selectB.value;

    if (!firstExecutionId || !secondExecutionId) {

        message.textContent =
            "Please select both executions before comparing individual tests.";

        return;
    }

    if (firstExecutionId === secondExecutionId) {

        message.textContent =
            "Please select two different executions.";

        return;
    }

    message.textContent =
        "Loading individual test comparison...";

    resultsWrapper.hidden = true;

    if (legend) {
        legend.hidden = true;
    }

    const loaded = await loadIndividualTestAnalytics();

    if (!loaded) {
        return;
    }

    const firstRecords = individualTestRecords.filter(record =>
        String(record.executionId) === firstExecutionId
    );

    const secondRecords = individualTestRecords.filter(record =>
        String(record.executionId) === secondExecutionId
    );

    if (firstRecords.length === 0 || secondRecords.length === 0) {

        message.textContent =
            "Individual test records were not found for both executions. " +
            "First execution: " + firstRecords.length +
            " records. Second execution: " + secondRecords.length +
            " records.";

        return;
    }

    const firstMap = new Map();
    const secondMap = new Map();

    firstRecords.forEach(record => {

        const key = getIndividualTestKey(record);

        if (key) {
            firstMap.set(key, record);
        }
    });

    secondRecords.forEach(record => {

        const key = getIndividualTestKey(record);

        if (key) {
            secondMap.set(key, record);
        }
    });

    const counts = {
        matched: 0,
        statusChanged: 0,
        faster: 0,
        slower: 0,
        newTests: 0,
        missing: 0
    };

    const rows = [];

    firstMap.forEach((firstRecord, key) => {

        const secondRecord = secondMap.get(key);

        if (!secondRecord) {

            counts.missing++;

            rows.push(
                createIndividualComparisonRow(
                    firstRecord,
                    null
                )
            );

            return;
        }

        counts.matched++;

        const firstStatus =
            normalizeIndividualStatus(firstRecord.status);

        const secondStatus =
            normalizeIndividualStatus(secondRecord.status);

        if (firstStatus !== secondStatus) {
            counts.statusChanged++;
        }

        const difference =
            getIndividualDuration(secondRecord) -
            getIndividualDuration(firstRecord);

        if (difference < 0) {
            counts.faster++;
        } else if (difference > 0) {
            counts.slower++;
        }

        rows.push(
            createIndividualComparisonRow(
                firstRecord,
                secondRecord
            )
        );
    });

    secondMap.forEach((secondRecord, key) => {

        if (!firstMap.has(key)) {

            counts.newTests++;

            rows.push(
                createIndividualComparisonRow(
                    null,
                    secondRecord
                )
            );
        }
    });

    if (rows.length === 0) {

        message.textContent =
            "No comparable individual test records were found.";

        return;
    }

    resultsTable.innerHTML = rows.join("");

    updateIndividualComparisonSummary(counts);

    resultsWrapper.hidden = false;

    if (legend) {
        legend.hidden = false;
    }

    message.textContent =
        "Individual comparison completed. " +
        firstRecords.length + " tests in the first execution and " +
        secondRecords.length + " tests in the second execution. " +
        "Matched: " + counts.matched +
        ", status changes: " + counts.statusChanged +
        ", new: " + counts.newTests +
        ", missing: " + counts.missing + ".";
}

// ==========================================
// PHASE 3.4: EXECUTION COMPARISON
// ==========================================

function populateComparisonExecutions() {

    const selectA = document.getElementById("comparisonExecutionA");
    const selectB = document.getElementById("comparisonExecutionB");

    if (!selectA || !selectB) {
        return;
    }

    const previousA = selectA.value;
    const previousB = selectB.value;

    const latestFirst = [...executionHistoryData].reverse();

    const options = latestFirst.map(execution => {

        const id = String(execution.executionId || "");
        const suite = String(execution.suiteName || "");
        const date = String(execution.startTime || "");

        return `
            <option value="${escapeHtml(id)}">
                ${escapeHtml(id)} | ${escapeHtml(suite)} | ${escapeHtml(date)}
            </option>
        `;

    }).join("");

    selectA.innerHTML =
        '<option value="">Select first execution</option>' + options;

    selectB.innerHTML =
        '<option value="">Select second execution</option>' + options;

    if (previousA && latestFirst.some(item =>
        String(item.executionId) === previousA
    )) {
        selectA.value = previousA;
    } else if (latestFirst.length > 0) {
        selectA.value = String(latestFirst[0].executionId);
    }

    if (previousB && latestFirst.some(item =>
        String(item.executionId) === previousB
    )) {
        selectB.value = previousB;
    } else if (latestFirst.length > 1) {
        selectB.value = String(latestFirst[1].executionId);
    }
}

function findComparisonExecution(executionId) {

    return executionHistoryData.find(execution =>
        String(execution.executionId) === String(executionId)
    );
}

function calculateComparisonPassRate(execution) {

    const total = Number(execution.total || 0);
    const passed = Number(execution.passed || 0);

    if (total === 0) {
        return 0;
    }

    return (passed / total) * 100;
}

function renderComparisonCharts(first, second) {

    const chartsWrapper =
        document.getElementById("comparisonChartsWrapper");

    const durationChart =
        document.getElementById("comparisonDurationChart");

    const resultsChart =
        document.getElementById("comparisonResultsChart");

    if (!chartsWrapper || !durationChart || !resultsChart) {
        console.error("Comparison chart containers were not found.");
        return;
    }

    const firstDuration = Math.max(0, Number(first.duration || 0));
    const secondDuration = Math.max(0, Number(second.duration || 0));

    const firstTotal = Math.max(0, Number(first.total || 0));
    const secondTotal = Math.max(0, Number(second.total || 0));

    const firstPassed = Math.max(0, Number(first.passed || 0));
    const secondPassed = Math.max(0, Number(second.passed || 0));

    const firstFailed = Math.max(0, Number(first.failed || 0));
    const secondFailed = Math.max(0, Number(second.failed || 0));

    const firstSkipped = Math.max(0, Number(first.skipped || 0));
    const secondSkipped = Math.max(0, Number(second.skipped || 0));

    const firstLabel = escapeHtml(
        String(first.executionId || "First execution")
    );

    const secondLabel = escapeHtml(
        String(second.executionId || "Second execution")
    );

    const renderBar = (label, value, maximum, colourClass) => {

        const percentage = maximum > 0
            ? Math.min(100, (value / maximum) * 100)
            : 0;

        return `
            <div class="comparison-chart-row">

                <div class="comparison-chart-label">
                    ${label}
                </div>

                <div class="comparison-bar-track">
                    <div class="comparison-bar-fill ${colourClass}"
                         style="width: ${percentage.toFixed(2)}%">
                    </div>
                </div>

                <div class="comparison-chart-value">
                    ${value.toLocaleString()}
                </div>

            </div>
        `;
    };

    const maximumDuration = Math.max(
        firstDuration,
        secondDuration,
        1
    );

    durationChart.innerHTML = `

        <div class="comparison-chart-legend">
            <span class="comparison-legend-item">
                <span class="comparison-legend-dot first"></span>
                First execution
            </span>

            <span class="comparison-legend-item">
                <span class="comparison-legend-dot second"></span>
                Second execution
            </span>
        </div>

        <div class="comparison-chart-group">

            <h4>First: ${firstLabel}</h4>

            ${renderBar(
                "Duration",
                firstDuration,
                maximumDuration,
                "first"
            )}

            <p class="comparison-chart-caption">
                ${formatDuration(firstDuration)}
            </p>

        </div>

        <div class="comparison-chart-group">

            <h4>Second: ${secondLabel}</h4>

            ${renderBar(
                "Duration",
                secondDuration,
                maximumDuration,
                "second"
            )}

            <p class="comparison-chart-caption">
                ${formatDuration(secondDuration)}
            </p>

        </div>
    `;

    const maximumResults = Math.max(
        firstTotal,
        secondTotal,
        firstPassed,
        secondPassed,
        firstFailed,
        secondFailed,
        firstSkipped,
        secondSkipped,
        1
    );

    const resultCategories = [
        {
            label: "Passed",
            first: firstPassed,
            second: secondPassed,
            colour: "passed"
        },
        {
            label: "Failed",
            first: firstFailed,
            second: secondFailed,
            colour: "failed"
        },
        {
            label: "Skipped",
            first: firstSkipped,
            second: secondSkipped,
            colour: "skipped"
        }
    ];

    resultsChart.innerHTML = `

        <div class="comparison-chart-legend">
            <span class="comparison-legend-item">
                <span class="comparison-legend-dot first"></span>
                First execution
            </span>

            <span class="comparison-legend-item">
                <span class="comparison-legend-dot second"></span>
                Second execution
            </span>
        </div>

        ${resultCategories.map(category => `

            <div class="comparison-result-category">

                <h4>${category.label}</h4>

                ${renderBar(
                    "First",
                    category.first,
                    maximumResults,
                    "first " + category.colour
                )}

                ${renderBar(
                    "Second",
                    category.second,
                    maximumResults,
                    "second " + category.colour
                )}

            </div>

        `).join("")}

    `;

    chartsWrapper.hidden = false;
}
function assessComparisonQuality(execution) {

    const total = Math.max(0, Number(execution.total || 0));
    const passed = Math.max(0, Number(execution.passed || 0));
    const failed = Math.max(0, Number(execution.failed || 0));

    if (total <= 0) {
        return {
            label: "UNAVAILABLE",
            description: "No test results were recorded."
        };
    }

    const passRate = calculateComparisonPassRate(execution);

    if (failed > 0 || passRate < 80) {
        return {
            label: "NEEDS ATTENTION",
            description: passRate.toFixed(2) +
                "% passed, with " + failed + " failed test(s)."
        };
    }

    if (passRate >= 95 && failed === 0) {
        return {
            label: "EXCELLENT",
            description: passRate.toFixed(2) +
                "% passed, with no failed tests."
        };
    }

    if (passRate >= 85) {
        return {
            label: "GOOD",
            description: passRate.toFixed(2) +
                "% passed."
        };
    }

    return {
        label: "FAIR",
        description: passRate.toFixed(2) +
            "% passed. Further improvement is possible."
    };
}


function renderAdvancedComparison(first, second) {

    const advancedWrapper =
        document.getElementById("comparisonAdvancedWrapper");

    const performanceValue =
        document.getElementById("comparisonPerformanceValue");

    const performanceStatus =
        document.getElementById("comparisonPerformanceStatus");

    const firstQuality =
        document.getElementById("comparisonFirstQuality");

    const secondQuality =
        document.getElementById("comparisonSecondQuality");

    const qualitySummary =
        document.getElementById("comparisonQualitySummary");

    const automaticSummary =
        document.getElementById("comparisonAutomaticSummary");

    if (
        !advancedWrapper ||
        !performanceValue ||
        !performanceStatus ||
        !firstQuality ||
        !secondQuality ||
        !qualitySummary ||
        !automaticSummary
    ) {
        console.error("Advanced comparison elements were not found.");
        return;
    }

    const firstDuration = Math.max(0, Number(first.duration || 0));
    const secondDuration = Math.max(0, Number(second.duration || 0));

    const firstPassRate = calculateComparisonPassRate(first);
    const secondPassRate = calculateComparisonPassRate(second);

    const firstFailed = Math.max(0, Number(first.failed || 0));
    const secondFailed = Math.max(0, Number(second.failed || 0));

    const firstTotal = Math.max(0, Number(first.total || 0));
    const secondTotal = Math.max(0, Number(second.total || 0));

    const firstAssessment = assessComparisonQuality(first);
    const secondAssessment = assessComparisonQuality(second);

    let improvementPercentage = null;
    let performanceLabel = "DURATION UNAVAILABLE";
    let performanceClass = "neutral";

    if (firstDuration > 0) {

        improvementPercentage =
            ((firstDuration - secondDuration) / firstDuration) * 100;

        if (improvementPercentage > 0.005) {
            performanceLabel = "SECOND EXECUTION FASTER";
            performanceClass = "positive";
        } else if (improvementPercentage < -0.005) {
            performanceLabel = "SECOND EXECUTION SLOWER";
            performanceClass = "negative";
        } else {
            improvementPercentage = 0;
            performanceLabel = "SIMILAR EXECUTION SPEED";
            performanceClass = "neutral";
        }

        performanceValue.textContent =
            Math.abs(improvementPercentage).toFixed(2) + "%";

    } else {

        performanceValue.textContent = "N/A";

    }

    performanceStatus.textContent = performanceLabel;
    performanceStatus.className =
        "comparison-status " + performanceClass;

    firstQuality.textContent = firstAssessment.label;
    secondQuality.textContent = secondAssessment.label;

    firstQuality.className =
        "comparison-advanced-value " +
        (firstAssessment.label === "EXCELLENT" ? "quality-excellent" : "");

    secondQuality.className =
        "comparison-advanced-value " +
        (secondAssessment.label === "EXCELLENT" ? "quality-excellent" : "");

    qualitySummary.textContent =
        "First execution: " + firstAssessment.description +
        " Second execution: " + secondAssessment.description;

    const passRateDifference = secondPassRate - firstPassRate;
    const failedDifference = secondFailed - firstFailed;

    let performanceSentence;

    if (improvementPercentage === null) {
        performanceSentence =
            "A performance percentage cannot be calculated because the first execution duration is zero.";
    } else if (improvementPercentage > 0) {
        performanceSentence =
            "The second execution was " +
            improvementPercentage.toFixed(2) +
            "% faster than the first execution.";
    } else if (improvementPercentage < 0) {
        performanceSentence =
            "The second execution was " +
            Math.abs(improvementPercentage).toFixed(2) +
            "% slower than the first execution.";
    } else {
        performanceSentence =
            "Both executions had the same recorded duration.";
    }

    let qualitySentence;

    if (passRateDifference > 0) {
        qualitySentence =
            "The pass rate improved by " +
            passRateDifference.toFixed(2) +
            " percentage points.";
    } else if (passRateDifference < 0) {
        qualitySentence =
            "The pass rate decreased by " +
            Math.abs(passRateDifference).toFixed(2) +
            " percentage points.";
    } else {
        qualitySentence =
            "The pass rate remained unchanged.";
    }

    let failureSentence;

    if (failedDifference > 0) {
        failureSentence =
            "Failed tests increased by " + failedDifference + ".";
    } else if (failedDifference < 0) {
        failureSentence =
            "Failed tests decreased by " + Math.abs(failedDifference) + ".";
    } else {
        failureSentence =
            "The number of failed tests remained unchanged.";
    }

    automaticSummary.textContent =
        performanceSentence + " " +
        qualitySentence + " " +
        failureSentence + " " +
        "The first execution recorded " + firstTotal +
        " test(s), while the second recorded " + secondTotal + " test(s).";

    advancedWrapper.hidden = false;
}


function exportExecutionComparisonCsv() {

    const selectA =
        document.getElementById("comparisonExecutionA");

    const selectB =
        document.getElementById("comparisonExecutionB");

    if (!selectA || !selectB || !selectA.value || !selectB.value) {
        alert("Please select two executions before exporting.");
        return;
    }

    if (selectA.value === selectB.value) {
        alert("Please select two different executions.");
        return;
    }

    const first = findComparisonExecution(selectA.value);
    const second = findComparisonExecution(selectB.value);

    if (!first || !second) {
        alert("The selected execution records could not be found.");
        return;
    }

    const firstDuration = Math.max(0, Number(first.duration || 0));
    const secondDuration = Math.max(0, Number(second.duration || 0));

    const improvementPercentage = firstDuration > 0
        ? ((firstDuration - secondDuration) / firstDuration) * 100
        : null;

    const firstAssessment = assessComparisonQuality(first);
    const secondAssessment = assessComparisonQuality(second);

    const csvEscape = value => {
        const text = String(value ?? "");
        return '"' + text.replace(/"/g, '""') + '"';
    };

    const rows = [
        ["Metric", "First Execution", "Second Execution", "Difference"],
        ["Execution ID", first.executionId || "", second.executionId || "", ""],
        ["Test Suite", first.suite || "", second.suite || "", ""],
        ["Total Tests", first.total || 0, second.total || 0,
            Number(second.total || 0) - Number(first.total || 0)],
        ["Passed Tests", first.passed || 0, second.passed || 0,
            Number(second.passed || 0) - Number(first.passed || 0)],
        ["Failed Tests", first.failed || 0, second.failed || 0,
            Number(second.failed || 0) - Number(first.failed || 0)],
        ["Skipped Tests", first.skipped || 0, second.skipped || 0,
            Number(second.skipped || 0) - Number(first.skipped || 0)],
        ["Pass Rate (%)",
            calculateComparisonPassRate(first).toFixed(2),
            calculateComparisonPassRate(second).toFixed(2),
            (calculateComparisonPassRate(second) -
                calculateComparisonPassRate(first)).toFixed(2)],
        ["Duration (milliseconds)", firstDuration, secondDuration,
            secondDuration - firstDuration],
        ["Performance Improvement (%)",
            "",
            improvementPercentage === null
                ? "N/A"
                : improvementPercentage.toFixed(2),
            ""],
        ["First Quality Assessment", firstAssessment.label, "", ""],
        ["Second Quality Assessment", "", secondAssessment.label, ""],
        ["Report Generated", new Date().toLocaleString(), "", ""]
    ];

    const csvContent = rows
        .map(row => row.map(csvEscape).join(","))
        .join("\r\n");

    const blob = new Blob(
        ["\uFEFF" + csvContent],
        { type: "text/csv;charset=utf-8;" }
    );

    const downloadUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");

    const firstId = String(first.executionId || "first")
        .replace(/[^a-zA-Z0-9_-]/g, "");

    const secondId = String(second.executionId || "second")
        .replace(/[^a-zA-Z0-9_-]/g, "");

    link.href = downloadUrl;
    link.download =
        "execution-comparison-" + firstId + "-vs-" + secondId + ".csv";

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(downloadUrl);
}


// ==========================================
// PHASE 3.7: ADVANCED COMPARISON INTELLIGENCE
// ==========================================

function renderComparisonIntelligence(first, second) {

    const statusElement =
        document.getElementById("comparisonRegressionStatus");

    const recommendationElement =
        document.getElementById("comparisonRecommendations");

    const warningElement =
        document.getElementById("comparisonQualityWarnings");

    const historicalElement =
        document.getElementById("comparisonHistoricalInsights");

    const actionElement =
        document.getElementById("comparisonActionableSummary");

    if (
        !statusElement ||
        !recommendationElement ||
        !warningElement ||
        !historicalElement ||
        !actionElement
    ) {
        console.error("Phase 3.7 intelligence elements were not found.");
        return;
    }

    const firstDuration = Math.max(0, Number(first.duration || 0));
    const secondDuration = Math.max(0, Number(second.duration || 0));

    const firstFailed = Math.max(0, Number(first.failed || 0));
    const secondFailed = Math.max(0, Number(second.failed || 0));

    const firstSkipped = Math.max(0, Number(first.skipped || 0));
    const secondSkipped = Math.max(0, Number(second.skipped || 0));

    const firstPassRate = calculateComparisonPassRate(first);
    const secondPassRate = calculateComparisonPassRate(second);

    const durationChange = firstDuration > 0
        ? ((secondDuration - firstDuration) / firstDuration) * 100
        : null;

    const passRateChange = secondPassRate - firstPassRate;
    const failedChange = secondFailed - firstFailed;
    const skippedChange = secondSkipped - firstSkipped;

    // --------------------------------------
    // 1. REGRESSION DETECTION
    // --------------------------------------

    const regressions = [];
    const improvements = [];

    if (durationChange !== null) {

        if (durationChange > 5) {
            regressions.push(
                "Execution duration increased by " +
                durationChange.toFixed(2) + "%."
            );

        } else if (durationChange < -5) {
            improvements.push(
                "Execution duration improved by " +
                Math.abs(durationChange).toFixed(2) + "%."
            );
        }
    }

    if (passRateChange < -0.001) {
        regressions.push(
            "Pass rate decreased by " +
            Math.abs(passRateChange).toFixed(2) +
            " percentage points."
        );

    } else if (passRateChange > 0.001) {
        improvements.push(
            "Pass rate improved by " +
            passRateChange.toFixed(2) +
            " percentage points."
        );
    }

    if (failedChange > 0) {
        regressions.push(
            failedChange + " additional test(s) failed."
        );

    } else if (failedChange < 0) {
        improvements.push(
            Math.abs(failedChange) +
            " fewer test(s) failed."
        );
    }

    if (regressions.length > 0) {

        statusElement.textContent =
            "POTENTIAL REGRESSION DETECTED";

        statusElement.className =
            "comparison-intelligence-status negative";

    } else if (improvements.length > 0) {

        statusElement.textContent =
            "IMPROVEMENT DETECTED";

        statusElement.className =
            "comparison-intelligence-status positive";

    } else {

        statusElement.textContent =
            "NO SIGNIFICANT CHANGE DETECTED";

        statusElement.className =
            "comparison-intelligence-status neutral";
    }

    // --------------------------------------
    // 2. PERFORMANCE RECOMMENDATIONS
    // --------------------------------------

    const recommendations = [];

    if (durationChange === null) {

        recommendations.push(
            "Performance comparison is unavailable because the first execution has no recorded duration."
        );

    } else if (durationChange > 5) {

        recommendations.push(
            "Investigate the slower execution and identify tests with increased running times."
        );

    } else if (durationChange < -5) {

        recommendations.push(
            "The second execution was faster. Continue monitoring future runs to confirm this improvement."
        );

    } else {

        recommendations.push(
            "Execution duration remained within the 5% comparison threshold."
        );
    }

    if (passRateChange < -0.001) {

        recommendations.push(
            "Review recently failing tests and investigate possible changes affecting test reliability."
        );

    } else if (passRateChange > 0.001) {

        recommendations.push(
            "The improved pass rate is encouraging. Continue monitoring test stability."
        );
    }

    if (secondSkipped > 0) {

        recommendations.push(
            "Review skipped tests to determine whether they were intentionally excluded or require attention."
        );
    }

    if (recommendations.length === 0) {
        recommendations.push(
            "Continue monitoring execution performance and test quality."
        );
    }

    recommendationElement.textContent =
        recommendations.map((item, index) =>
            (index + 1) + ". " + item
        ).join("\n");

    // --------------------------------------
    // 3. QUALITY WARNINGS
    // --------------------------------------

    const warnings = [];

    if (failedChange > 0) {
        warnings.push(
            "WARNING: Failed tests increased by " +
            failedChange + "."
        );
    }

    if (skippedChange > 0) {
        warnings.push(
            "WARNING: Skipped tests increased by " +
            skippedChange + "."
        );
    }

    if (passRateChange < -0.001) {
        warnings.push(
            "WARNING: Overall pass rate declined."
        );
    }

    if (warnings.length === 0) {
        warningElement.textContent =
            "No increases in failed or skipped tests were detected.";
    } else {
        warningElement.textContent = warnings.join("\n");
    }

    // --------------------------------------
    // 4. HISTORICAL INSIGHTS
    // --------------------------------------

    const firstSuite = String(
        first.suiteName || first.suite || ""
    ).trim().toLowerCase();

    const historicalExecutions = executionHistoryData.filter(execution => {

        const executionId = String(
            execution.executionId || ""
        );

        const executionSuite = String(
            execution.suiteName || execution.suite || ""
        ).trim().toLowerCase();

        return executionId !== String(first.executionId) &&
               executionId !== String(second.executionId) &&
               executionSuite === firstSuite;
    });

    let historicalDurationAverage = null;

    if (historicalExecutions.length === 0) {

        historicalElement.textContent =
            "There are no other recorded executions for this test suite. " +
            "More historical runs are required for a meaningful baseline.";

    } else {

        const validDurations = historicalExecutions
            .map(execution => Number(execution.duration))
            .filter(duration =>
                Number.isFinite(duration) && duration >= 0
            );

        const validPassRates = historicalExecutions
            .filter(execution => Number(execution.total) > 0)
            .map(execution =>
                calculateComparisonPassRate(execution)
            );

        const durationAverage = validDurations.length > 0
            ? validDurations.reduce((sum, value) => sum + value, 0) /
              validDurations.length
            : null;
        historicalDurationAverage = durationAverage;

        const passRateAverage = validPassRates.length > 0
            ? validPassRates.reduce((sum, value) => sum + value, 0) /
              validPassRates.length
            : null;

        let historicalText =
            "Historical executions available for this suite: " +
            historicalExecutions.length + ". ";

        if (durationAverage !== null) {

            historicalText +=
                "Average historical duration: " +
                formatDuration(durationAverage) + ". ";

            if (secondDuration > durationAverage * 1.05) {

                historicalText +=
                    "The second execution is more than 5% slower than the historical average. ";

            } else if (secondDuration < durationAverage * 0.95) {

                historicalText +=
                    "The second execution is more than 5% faster than the historical average. ";

            } else {

                historicalText +=
                    "The second execution is within 5% of the historical average. ";
            }
        }

        if (passRateAverage !== null) {

            historicalText +=
                "Historical average pass rate: " +
                passRateAverage.toFixed(2) + "%. ";

            if (secondPassRate < passRateAverage - 0.001) {

                historicalText +=
                    "The second execution is below the historical pass rate.";

            } else if (secondPassRate > passRateAverage + 0.001) {

                historicalText +=
                    "The second execution is above the historical pass rate.";

            } else {

                historicalText +=
                    "The second execution matches the historical pass rate.";
            }
        }

        historicalElement.textContent = historicalText;
    }

    // --------------------------------------
    // 5. ACTIONABLE SUMMARY
    // --------------------------------------

    let actionSummary = "";

    const slowerThanHistoricalBaseline =
        historicalDurationAverage !== null &&
        secondDuration > historicalDurationAverage * 1.05;

    if (regressions.length > 0) {

        actionSummary =
            "REGRESSION: The second execution requires investigation. " +
            regressions.join(" ");

    } else if (
        improvements.length > 0 &&
        slowerThanHistoricalBaseline
    ) {

        actionSummary =
            "MIXED RESULTS: The second execution improved against the " +
            "selected comparison execution, but remains more than 5% " +
            "slower than the historical average. " +
            improvements.join(" ");

    } else if (improvements.length > 0) {

        actionSummary =
            "IMPROVEMENT: The second execution shows positive changes. " +
            improvements.join(" ");

    } else {

        actionSummary =
            "NO SIGNIFICANT CHANGE: The second execution shows no " +
            "significant changes under the current comparison rules.";
    }
    if (secondFailed > 0 || secondSkipped > 0) {

        actionSummary +=
            " Review the test results and skipped tests before treating the run as fully healthy.";
    }

    actionElement.textContent = actionSummary;
}

function renderExecutionComparison() {

    const selectA = document.getElementById("comparisonExecutionA");
    const selectB = document.getElementById("comparisonExecutionB");

    const message = document.getElementById("comparisonMessage");
    const wrapper = document.getElementById("comparisonResultsWrapper");
    const table = document.getElementById("comparisonResultsTable");

    if (!selectA || !selectB || !message || !wrapper || !table) {
        return;
    }

    const idA = selectA.value;
    const idB = selectB.value;

    wrapper.hidden = true;
    table.innerHTML = "";

    const chartsWrapper =
        document.getElementById("comparisonChartsWrapper");

    if (chartsWrapper) {
        chartsWrapper.hidden = true;
    }

    const advancedWrapper =
        document.getElementById("comparisonAdvancedWrapper");

    if (advancedWrapper) {
        advancedWrapper.hidden = true;
    }

    if (!idA || !idB) {
        message.textContent = "Please select two executions.";
        return;
    }

    if (idA === idB) {
        message.textContent = "Please select two different executions.";
        return;
    }

    const first = findComparisonExecution(idA);
    const second = findComparisonExecution(idB);

    if (!first || !second) {
        message.textContent = "One or both executions could not be found.";
        return;
    }

    const firstTotal = Number(first.total || 0);
    const secondTotal = Number(second.total || 0);

    const firstPassed = Number(first.passed || 0);
    const secondPassed = Number(second.passed || 0);

    const firstFailed = Number(first.failed || 0);
    const secondFailed = Number(second.failed || 0);

    const firstSkipped = Number(first.skipped || 0);
    const secondSkipped = Number(second.skipped || 0);

    const firstDuration = Number(first.duration || 0);
    const secondDuration = Number(second.duration || 0);

    const firstPassRate = calculateComparisonPassRate(first);
    const secondPassRate = calculateComparisonPassRate(second);

    const durationDifference = secondDuration - firstDuration;
    const passRateDifference = secondPassRate - firstPassRate;

    let performanceMessage = "Same execution duration";

    if (durationDifference < 0) {
        performanceMessage =
            "Second execution was faster by " +
            formatDuration(Math.abs(durationDifference));
    } else if (durationDifference > 0) {
        performanceMessage =
            "Second execution was slower by " +
            formatDuration(durationDifference);
    }

    const formatDifference = (difference, suffix = "") => {

        if (difference > 0) {
            return "+" + difference.toFixed(2) + suffix;
        }

        return difference.toFixed(2) + suffix;
    };

    const rows = [
        {
            metric: "Total Tests",
            first: firstTotal,
            second: secondTotal,
            difference: formatDifference(secondTotal - firstTotal)
        },
        {
            metric: "Passed Tests",
            first: firstPassed,
            second: secondPassed,
            difference: formatDifference(secondPassed - firstPassed)
        },
        {
            metric: "Failed Tests",
            first: firstFailed,
            second: secondFailed,
            difference: formatDifference(secondFailed - firstFailed)
        },
        {
            metric: "Skipped Tests",
            first: firstSkipped,
            second: secondSkipped,
            difference: formatDifference(secondSkipped - firstSkipped)
        },
        {
            metric: "Pass Rate",
            first: firstPassRate.toFixed(2) + "%",
            second: secondPassRate.toFixed(2) + "%",
            difference: formatDifference(passRateDifference, " percentage points")
        },
        {
            metric: "Duration",
            first: formatDuration(firstDuration),
            second: formatDuration(secondDuration),
            difference: performanceMessage
        }
    ];

    table.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.metric)}</td>
            <td>${escapeHtml(String(row.first))}</td>
            <td>${escapeHtml(String(row.second))}</td>
            <td>${escapeHtml(String(row.difference))}</td>
        </tr>
    `).join("");

    renderComparisonCharts(first, second);
    renderAdvancedComparison(first, second);
    renderComparisonIntelligence(first, second);

    message.textContent =
        "Comparing " + idA + " against " + idB + ".";

    wrapper.hidden = false;
}

// Connect the comparison button.
const compareExecutionsButton =
    document.getElementById("compareExecutionsButton");

if (compareExecutionsButton) {
    compareExecutionsButton.addEventListener(
        "click",
        renderExecutionComparison
    );
}
// ==========================================
// PHASE 3.8: INDIVIDUAL COMPARISON BUTTON
// ==========================================

const compareIndividualTestsButton =
    document.getElementById("compareIndividualTestsButton");

if (compareIndividualTestsButton) {

    compareIndividualTestsButton.addEventListener(
        "click",
        renderIndividualTestComparison
    );
}
const exportComparisonCsvButton =
    document.getElementById("exportComparisonCsvButton");

if (exportComparisonCsvButton) {
    exportComparisonCsvButton.addEventListener(
        "click",
        exportExecutionComparisonCsv
    );
}
function renderHistory(history) {

    executionHistoryData = Array.isArray(history) ? history : [];

    populateComparisonExecutions();

    renderFilteredHistory();
}

function renderFilteredHistory() {

    const table = document.getElementById("historyTable");
    const filter = document.getElementById("executionSuiteFilter");
    const historyCount = document.getElementById("historyCount");
    const filteredHistoryCount = document.getElementById("filteredHistoryCount");

    const startDateInput = document.getElementById("historyStartDate");
    const endDateInput = document.getElementById("historyEndDate");
    const searchInput = document.getElementById("historySearchInput");

    if (!table) {
        console.error("Execution history table was not found.");
        return;
    }

    const selectedFilter = filter ? filter.value : "ALL_SUITES";
    const startDate = startDateInput ? startDateInput.value : "";
    const endDate = endDateInput ? endDateInput.value : "";

    // Validate the selected date range.
    if (startDate && endDate && startDate > endDate) {

        table.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">
                    Invalid date range. The From Date must be earlier than or equal to the To Date.
                </td>
            </tr>
        `;

        if (historyCount) {
            historyCount.textContent = "INVALID DATE RANGE";
        }

        if (filteredHistoryCount) {
            filteredHistoryCount.textContent = "Please correct your dates";
        }

        return;
    }

    // Start with all available execution records.
    let filteredHistory = executionHistoryData;

    // Apply the selected test suite filter.
    if (selectedFilter === "API") {

        filteredHistory = filteredHistory.filter(execution =>
            String(execution.suiteName || "").toUpperCase().includes("API")
        );

    } else if (selectedFilter === "WEB") {

        filteredHistory = filteredHistory.filter(execution =>
            String(execution.suiteName || "").toUpperCase().includes("WEB")
        );

    } else if (selectedFilter === "ALL") {

        filteredHistory = filteredHistory.filter(execution =>
            String(execution.suiteName || "").toUpperCase()
                .includes("COMPLETE AUTOMATION TESTS")
        );
    }

    // Apply the inclusive date range.
    if (startDate || endDate) {

        filteredHistory = filteredHistory.filter(execution => {

            const executionDate = String(execution.startTime || "").substring(0, 10);

            if (!executionDate) {
                return false;
            }

            const afterStartDate =
                !startDate || executionDate >= startDate;

            const beforeEndDate =
                !endDate || executionDate <= endDate;

            return afterStartDate && beforeEndDate;
        });
    }

    // Apply execution ID and test suite search.
    const searchTerm = searchInput
        ? searchInput.value.trim().toLowerCase()
        : "";

    if (searchTerm) {

        filteredHistory = filteredHistory.filter(execution => {

            const executionId = String(execution.executionId || "").toLowerCase();
            const suiteName = String(execution.suiteName || "").toLowerCase();

            return executionId.includes(searchTerm) ||
                   suiteName.includes(searchTerm);
        });
    }
    // Update the execution counters.
    if (historyCount) {
        historyCount.textContent =
            filteredHistory.length + " EXECUTIONS";
    }

    if (filteredHistoryCount) {
        filteredHistoryCount.textContent =
            "Showing " + filteredHistory.length +
            " of " + executionHistoryData.length + " executions";
    }

    // Display an empty-state message when no records match.
    if (filteredHistory.length === 0) {

        table.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">
                    No execution history found for the selected filters.
                </td>
            </tr>
        `;

        return;
    }

    // Display the newest execution first.
    const latestFirst = [...filteredHistory].reverse();

    table.innerHTML = latestFirst.map(execution => {

        const result = Number(execution.failed) === 0
            ? "PASSED"
            : "FAILED";

        const resultClass = result === "PASSED" ? "" : "failed";

        return `
            <tr>
                <td>${escapeHtml(execution.executionId)}</td>
                <td>${escapeHtml(execution.suiteName)}</td>
                <td>${escapeHtml(execution.startTime)}</td>
                <td>${execution.total}</td>
                <td>${execution.passed}</td>
                <td>${execution.failed}</td>
                <td>${formatDuration(execution.duration)}</td>
                <td>
                    <span class="result-badge ${resultClass}">
                        ${result}
                    </span>
                </td>
            </tr>
        `;

    }).join("");
}

// Suite filter listener.
const historySearchInput =
    document.getElementById("historySearchInput");

if (historySearchInput) {
    historySearchInput.addEventListener("input", renderFilteredHistory);
}
const executionSuiteFilter =
    document.getElementById("executionSuiteFilter");

if (executionSuiteFilter) {
    executionSuiteFilter.addEventListener("change", renderFilteredHistory);
}

// Date filter listeners.
const historyStartDate =
    document.getElementById("historyStartDate");

const historyEndDate =
    document.getElementById("historyEndDate");

if (historyStartDate) {
    historyStartDate.addEventListener("change", renderFilteredHistory);
}

if (historyEndDate) {
    historyEndDate.addEventListener("change", renderFilteredHistory);
}

// Clear both dates without changing the selected suite.
const clearHistoryDateFilter =
    document.getElementById("clearHistoryDateFilter");

if (clearHistoryDateFilter) {

    clearHistoryDateFilter.addEventListener("click", function () {

        if (historyStartDate) {
            historyStartDate.value = "";
        }

        if (historyEndDate) {
            historyEndDate.value = "";
        }

        renderFilteredHistory();
    });
}
// LOAD DASHBOARD DATA
// ==========================================

async function loadDashboard() {

    if (!refreshButton) return;

    refreshButton.disabled = true;
    refreshButton.textContent = "Loading...";

    try {

        const response = await fetch(API_URL, {
            method: "GET",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error("Server returned HTTP " + response.status);
        }

        const data = await response.json();

        updateDashboard(data);

        // Phase 3.8: Load individual test analytics separately.
        // Its own error handler protects the main dashboard.

        await loadIndividualTestAnalytics();

        hideError();

    } catch (error) {

        console.error("Dashboard loading error:", error);

        showError(
            "Could not load dashboard data. Check that the Java server is running."
        );

    } finally {

        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh Data";
    }
}

if (refreshButton) {
    refreshButton.addEventListener("click", loadDashboard);
}

loadDashboard();

// ==========================================
// TEST EXECUTION CENTRE
// ==========================================

const executionButtons = document.querySelectorAll(".run-button");

const executionStatus = document.getElementById("executionStatus");

const executionStatusTitle =
    document.getElementById("executionStatusTitle");

const executionStatusMessage =
    document.getElementById("executionStatusMessage");

let previousExecutionStatus = "IDLE";

// ==========================================
// FETCH LIVE DATA FOR EXECUTION STATUS
// ==========================================

// Unique function name prevents conflicts with live-monitor.js.

async function fetchLiveExecutionForStatus() {

    try {

        const response = await fetch(LIVE_API_URL, {
            method: "GET",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(
                "Live execution request failed: HTTP " + response.status
            );
        }

        return await response.json();

    } catch (error) {

        console.error("Live execution tracking error:", error);

        return null;
    }
}

// ==========================================
// LIVE TEST PROGRESS MESSAGE
// ==========================================

function createLiveProgressMessage(liveData) {

    if (!liveData || liveData.status !== "RUNNING") {
        return "";
    }

    const total = Number(liveData.total || 0);
    const passed = Number(liveData.passed || 0);
    const failed = Number(liveData.failed || 0);
    const skipped = Number(liveData.skipped || 0);

    const currentTest = liveData.currentTest || "Preparing tests";

    return (
        "\n\nLIVE TEST PROGRESS" +
        "\nCurrent test: " + currentTest +
        "\nTotal started: " + total +
        "\nPassed: " + passed +
        "\nFailed: " + failed +
        "\nSkipped: " + skipped
    );
}

// ==========================================
// FINAL RESULTS MESSAGE
// ==========================================

function createFinalResultsMessage(liveData) {

    if (!liveData || liveData.status !== "COMPLETED") {
        return "";
    }

    return (
        "\n\nFINAL TEST RESULTS" +
        "\nTotal tests: " + Number(liveData.total || 0) +
        "\nPassed: " + Number(liveData.passed || 0) +
        "\nFailed: " + Number(liveData.failed || 0) +
        "\nSkipped: " + Number(liveData.skipped || 0) +
        "\nDuration: " + formatDuration(liveData.duration)
    );
}

// ==========================================
// UPDATE EXECUTION DISPLAY
// ==========================================

function updateExecutionDisplay(data, liveData = null) {

    const status = data.status || "IDLE";

    executionStatus.classList.remove(
        "running",
        "success",
        "failed"
    );

    if (status === "RUNNING") {

        executionStatus.classList.add("running");

        executionStatusTitle.textContent =
            "Tests are running";

        executionStatusMessage.textContent =
            (data.message || "Automation is currently executing.") +
            createLiveProgressMessage(liveData);

    } else if (status === "COMPLETED") {

        executionStatus.classList.add("success");

        executionStatusTitle.textContent =
            "Execution completed";

        executionStatusMessage.textContent =
            (data.message || "Test execution completed.") +
            " Exit code: " + data.exitCode +
            createFinalResultsMessage(liveData);

    } else if (status === "FAILED") {

        executionStatus.classList.add("failed");

        executionStatusTitle.textContent =
            "Execution failed";

        executionStatusMessage.textContent =
            (data.message || "Test execution failed.") +
            " Check test-reports/dashboard-execution.log." +
            createFinalResultsMessage(liveData);

    } else {

        executionStatusTitle.textContent =
            "Ready to execute";

        executionStatusMessage.textContent =
            "Select a test suite to begin automation.";
    }

    executionButtons.forEach(button => {
        button.disabled = Boolean(data.running);
    });

    if (
        previousExecutionStatus === "RUNNING" &&
        status !== "RUNNING"
    ) {

        loadDashboard();
    }

    previousExecutionStatus = status;
}

// ==========================================
// FETCH EXECUTION STATUS
// ==========================================

async function loadExecutionStatus() {

    try {

        const response = await fetch(STATUS_API_URL, {
            method: "GET",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(
                "Status request failed: HTTP " + response.status
            );
        }

        const statusData = await response.json();

        const liveData = await fetchLiveExecutionForStatus();

        updateExecutionDisplay(statusData, liveData);

    } catch (error) {

        console.error("Execution status error:", error);

        executionStatusTitle.textContent =
            "Backend unavailable";

        executionStatusMessage.textContent =
            "Start the Java dashboard server to monitor test executions.";
    }
}

// ==========================================
// START TEST EXECUTION
// ==========================================

async function startTestExecution(suite) {

    executionButtons.forEach(button => {
        button.disabled = true;
    });

    executionStatus.classList.remove("success", "failed");

    executionStatus.classList.add("running");

    executionStatusTitle.textContent =
        "Submitting test execution...";

    executionStatusMessage.textContent =
        "Sending the " + suite +
        " suite request to the Java backend.";

    try {

        const response = await fetch("/api/executions", {

            method: "POST",

            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },

            body: new URLSearchParams({
                suite: suite
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.error || "Could not start test execution."
            );
        }

        executionStatusTitle.textContent =
            "Test execution started";

        executionStatusMessage.textContent =
            suite + " suite has been submitted successfully.";

        await loadExecutionStatus();

    } catch (error) {

        console.error("Test execution error:", error);

        executionStatus.classList.remove("running");

        executionStatus.classList.add("failed");

        executionStatusTitle.textContent =
            "Could not start tests";

        executionStatusMessage.textContent =
            error.message;

        executionButtons.forEach(button => {
            button.disabled = false;
        });
    }
}

// ==========================================
// EXECUTION BUTTON EVENTS
// ==========================================

executionButtons.forEach(button => {

    button.addEventListener("click", () => {

        const selectedSuite = button.dataset.suite;

        if (!["API", "WEB", "ALL"].includes(selectedSuite)) {
            return;
        }

        startTestExecution(selectedSuite);
    });
});

// ==========================================
// AUTOMATIC EXECUTION MONITORING
// ==========================================

loadExecutionStatus();

setInterval(loadExecutionStatus, 2000);

// ==========================================
// FINAL REFRESH BUTTON LABEL
// ==========================================

if (refreshButton) {
    refreshButton.textContent = "Refresh Data";
}






