
// ==========================================
// AUTOTEST QA PLATFORM
// PROFESSIONAL LIVE EXECUTION MONITOR
// ==========================================


// ==========================================
// BACKEND API ENDPOINTS
// ==========================================

const LIVE_MONITOR_API_URL = "/api/executions/live";

const LIVE_ACTIVITY_API_URL = "/api/executions/activity";


// ==========================================
// GLOBAL VARIABLES
// ==========================================

let liveElements = {};

let activityEntries = [];

let latestExecution = null;

let pollingInterval = null;

let activityInterval = null;

let durationInterval = null;

let isFetchingLive = false;

let isFetchingActivity = false;


// ==========================================
// GET DASHBOARD ELEMENTS
// ==========================================

function getLiveElements() {

    liveElements = {

        status: document.getElementById("liveStatusBadge"),

        suite: document.getElementById("liveSuiteName"),

        currentTest: document.getElementById("liveCurrentTest"),

        executionMessage: document.getElementById("liveExecutionMessage"),

        progressText: document.getElementById("liveProgressText"),

        progressPercentage: document.getElementById("liveProgressPercentage"),

        completedText: document.getElementById("liveCompletedText"),

        progressFill: document.getElementById("liveProgressFill"),

        progressNote: document.getElementById("liveProgressNote"),

        total: document.getElementById("liveTotal"),

        passed: document.getElementById("livePassed"),

        failed: document.getElementById("liveFailed"),

        skipped: document.getElementById("liveSkipped"),

        duration: document.getElementById("liveDuration"),

        activityLog: document.getElementById("liveActivityLog")

    };

}


// ==========================================
// FORMAT DURATION
// ==========================================

function formatLiveDuration(milliseconds) {

    const seconds = Math.max(
        0,
        Number(milliseconds) || 0
    ) / 1000;

    if (seconds >= 60) {

        const minutes = Math.floor(seconds / 60);

        const remainingSeconds = (seconds % 60).toFixed(1);

        return `${minutes} min ${remainingSeconds} sec`;

    }

    return `${seconds.toFixed(2)} sec`;

}


// ==========================================
// FORMAT ACTIVITY TIME
// ==========================================

function formatActivityTime(timestamp) {

    if (!timestamp) {

        return "Unknown time";

    }

    const date = new Date(timestamp);

    if (Number.isNaN(date.getTime())) {

        return "Unknown time";

    }

    return date.toLocaleTimeString([], {

        hour: "2-digit",

        minute: "2-digit",

        second: "2-digit"

    });

}


// ==========================================
// GET SHORT TEST NAME
// ==========================================

function getShortTestName(testName) {

    if (!testName) {

        return "Unknown test";

    }

    return testName.includes(".")

        ? testName.split(".").pop()

        : testName;

}


// ==========================================
// ACTIVITY EVENT INFORMATION
// ==========================================

function getActivityInformation(event) {

    const eventType = (event.event || "").toUpperCase();

    const testName = event.testName || "Unknown test";

    const displayName = getShortTestName(testName);


    switch (eventType) {

        case "EXECUTION_STARTED":

            return {

                title: "Execution Started",

                message: `Starting ${testName}`,

                type: "info"

            };


        case "TEST_STARTED":

            return {

                title: "Test Running",

                message: `Executing ${displayName}`,

                type: "running"

            };


        case "TEST_PASSED":

            return {

                title: "Test Passed",

                message: `${displayName} completed successfully`,

                type: "success"

            };


        case "TEST_FAILED":

            return {

                title: "Test Failed",

                message: `${displayName}: ${event.message || "Test failed"}`,

                type: "failed"

            };


        case "TEST_SKIPPED":

            return {

                title: "Test Skipped",

                message: `${displayName} was skipped`,

                type: "skipped"

            };


        case "EXECUTION_COMPLETED":

            return {

                title: "Execution Completed",

                message: event.message || "All tests finished successfully.",

                type: "success"

            };


        case "EXECUTION_FAILED":

            return {

                title: "Execution Failed",

                message: event.message || "Review the execution report.",

                type: "failed"

            };


        default:

            return {

                title: eventType || "Activity",

                message: event.message || "Unknown activity",

                type: "info"

            };

    }

}


// ==========================================
// CREATE ACTIVITY EMPTY STATE
// ==========================================

function renderActivityEmptyState() {

    const container = liveElements.activityLog;

    if (!container) return;


    const emptyState = document.createElement("div");

    emptyState.className = "activity-empty";


    const icon = document.createElement("span");

    icon.className = "activity-empty-icon";

    icon.textContent = "◷";


    const title = document.createElement("strong");

    title.textContent = "No execution activity yet";


    const description = document.createElement("p");

    description.textContent =
        "Test events will appear here when an automation suite starts.";


    emptyState.appendChild(icon);

    emptyState.appendChild(title);

    emptyState.appendChild(description);


    container.replaceChildren(emptyState);

}


// ==========================================
// RENDER ACTIVITY LOG
// ==========================================

function renderActivityLog() {

    const container = liveElements.activityLog;

    if (!container) return;


    // Clear previous activity.

    container.replaceChildren();


    // Display empty state when no events exist.

    if (activityEntries.length === 0) {

        renderActivityEmptyState();

        return;

    }


    // Show the latest 100 events, newest first.

    const latestEvents = activityEntries

        .slice(-100)

        .reverse();


    latestEvents.forEach(event => {

        const information = getActivityInformation(event);


        // Activity container.

        const item = document.createElement("div");

        item.className = `activity-entry ${information.type}`;


        // Status indicator.

        const dot = document.createElement("span");

        dot.className =
            `activity-dot ${information.type}-dot`;


        // Activity content.

        const content = document.createElement("div");

        content.className = "activity-content";


        // Activity title.

        const title = document.createElement("strong");

        title.className = "activity-title";

        title.textContent = information.title;


        // Activity message.

        const message = document.createElement("span");

        message.className = "activity-message";

        message.textContent = information.message;


        // Activity metadata.

        const metadata = document.createElement("div");

        metadata.className = "activity-meta";


        // Timestamp.

        const time = document.createElement("small");

        time.className = "activity-time";

        time.textContent = formatActivityTime(event.timestamp);


        // Test duration.

        const duration = Number(event.duration) || 0;


        metadata.appendChild(time);


        if (duration > 0) {

            const durationElement = document.createElement("small");

            durationElement.className = "activity-duration";

            durationElement.textContent =
                formatLiveDuration(duration);

            metadata.appendChild(durationElement);

        }


        // Assemble activity content.

        content.appendChild(title);

        content.appendChild(message);

        content.appendChild(metadata);


        item.appendChild(dot);

        item.appendChild(content);


        container.appendChild(item);

    });

}


// ==========================================
// FETCH REAL ACTIVITY EVENTS
// ==========================================

async function fetchAndRenderActivity() {

    if (isFetchingActivity) return;

    isFetchingActivity = true;


    try {

        const response = await fetch(LIVE_ACTIVITY_API_URL, {

            method: "GET",

            cache: "no-store",

            headers: {

                "Accept": "application/json"

            }

        });


        if (!response.ok) {

            throw new Error(`HTTP ${response.status}`);

        }


        const events = await response.json();


        if (!Array.isArray(events)) {

            throw new Error("Invalid activity response.");

        }


        activityEntries = events;

        renderActivityLog();


    } catch (error) {

        console.error(

            "Could not load test activity:",

            error

        );

    } finally {

        isFetchingActivity = false;

    }

}


// ==========================================
// UPDATE LIVE STATUS BADGE
// ==========================================

function updateLiveStatus(status) {

    const element = liveElements.status;

    if (!element) return;


    const normalizedStatus = (status || "IDLE").toUpperCase();


    const allowedStatuses = [

        "IDLE",

        "RUNNING",

        "COMPLETED",

        "FAILED",

        "OFFLINE"

    ];


    const safeStatus = allowedStatuses.includes(normalizedStatus)

        ? normalizedStatus

        : "IDLE";


    element.textContent = safeStatus;

    element.className =
        `live-status-badge ${safeStatus.toLowerCase()}`;

}


// ==========================================
// UPDATE EXECUTION MESSAGE
// ==========================================

function updateExecutionMessage(data) {

    const element = liveElements.executionMessage;

    if (!element) return;


    const status = (data.status || "IDLE").toUpperCase();


    switch (status) {

        case "RUNNING":

            element.textContent =
                "Automated tests are currently executing.";

            break;


        case "COMPLETED":

            element.textContent =
                "Execution completed successfully.";

            break;


        case "FAILED":

            element.textContent =
                "Execution finished with errors.";

            break;


        case "OFFLINE":

            element.textContent =
                "Unable to connect to the execution service.";

            break;


        default:

            element.textContent =
                "Waiting for automated execution.";

    }

}


// ==========================================
// UPDATE EXECUTION PROGRESS
// ==========================================

function updateProgress(data) {

    const total = Number(data.total) || 0;

    const passed = Number(data.passed) || 0;

    const failed = Number(data.failed) || 0;

    const skipped = Number(data.skipped) || 0;


    const completedTests = passed + failed + skipped;


    const status = (data.status || "IDLE").toUpperCase();


    let percentage = total > 0

        ? (completedTests / total) * 100

        : 0;


    percentage = Math.min(100, Math.max(0, percentage));


    // A successfully completed execution is always 100%.

    if (status === "COMPLETED") {

        percentage = 100;

    }


    // Update progress bar.

    if (liveElements.progressFill) {

        liveElements.progressFill.style.width =
            `${percentage}%`;


        if (status === "RUNNING") {

            liveElements.progressFill.classList.add("running");

        } else {

            liveElements.progressFill.classList.remove("running");

        }

    }


    // Update percentage.

    const percentageText = `${Math.round(percentage)}%`;


    if (liveElements.progressText) {

        liveElements.progressText.textContent =
            percentageText;

    }


    if (liveElements.progressPercentage) {

        liveElements.progressPercentage.textContent =
            percentageText;

    }


    // Update completed test count.

    if (liveElements.completedText) {

        liveElements.completedText.textContent =
            `${completedTests} of ${total} tests completed`;

    }


    // Update progress description.

    if (liveElements.progressNote) {

        switch (status) {

            case "RUNNING":

                liveElements.progressNote.textContent =
                    "Automated tests are currently executing.";

                break;


            case "COMPLETED":

                liveElements.progressNote.textContent =
                    "Execution finished. Final test results are displayed below.";

                break;


            case "FAILED":

                liveElements.progressNote.textContent =
                    "Execution failed. Review the test results and execution report.";

                break;


            default:

                liveElements.progressNote.textContent =
                    "Start a test suite to see live execution activity.";

        }

    }

}


// ==========================================
// UPDATE EXECUTION DURATION
// ==========================================

function updateDuration(data) {

    const element = liveElements.duration;

    if (!element) return;


    const status = (data.status || "IDLE").toUpperCase();


    const startTime = Number(data.startTime) || 0;

    const endTime = Number(data.endTime) || 0;

    const recordedDuration = Number(data.duration) || 0;


    if (status === "COMPLETED" || status === "FAILED") {

        const finalDuration = recordedDuration > 0

            ? recordedDuration

            : (

                startTime > 0 && endTime > 0

                    ? endTime - startTime

                    : 0

            );


        element.textContent =
            formatLiveDuration(finalDuration);

        return;

    }


    if (status === "RUNNING" && startTime > 0) {

        element.textContent =

            formatLiveDuration(Date.now() - startTime);

        return;

    }


    element.textContent = "0.00 sec";

}


// ==========================================
// RENDER LIVE EXECUTION DATA
// ==========================================

function renderLiveExecution(data) {

    if (!data || typeof data !== "object") return;


    latestExecution = data;


    const status = (data.status || "IDLE").toUpperCase();


    // --------------------------------------
    // TEST SUITE
    // --------------------------------------

    if (liveElements.suite) {

        liveElements.suite.textContent =

            data.suiteName || "Waiting for execution";

    }


    // --------------------------------------
    // CURRENT TEST
    // --------------------------------------

    if (liveElements.currentTest) {

        liveElements.currentTest.textContent =

            status === "COMPLETED"

                ? "Execution finished"

                : status === "FAILED"

                    ? "Execution stopped"

                    : data.currentTest || "No test running";

    }


    // --------------------------------------
    // TEST COUNTERS
    // --------------------------------------

    if (liveElements.total) {

        liveElements.total.textContent =

            Number(data.total) || 0;

    }


    if (liveElements.passed) {

        liveElements.passed.textContent =

            Number(data.passed) || 0;

    }


    if (liveElements.failed) {

        liveElements.failed.textContent =

            Number(data.failed) || 0;

    }


    if (liveElements.skipped) {

        liveElements.skipped.textContent =

            Number(data.skipped) || 0;

    }


    // --------------------------------------
    // UPDATE MONITOR COMPONENTS
    // --------------------------------------

    updateLiveStatus(status);

    updateExecutionMessage(data);

    updateProgress(data);

    updateDuration(data);

}


// ==========================================
// FETCH LIVE EXECUTION DATA
// ==========================================

async function fetchAndRenderLiveExecution() {

    if (isFetchingLive) return;

    isFetchingLive = true;


    try {

        const response = await fetch(LIVE_MONITOR_API_URL, {

            method: "GET",

            cache: "no-store",

            headers: {

                "Accept": "application/json"

            }

        });


        if (!response.ok) {

            throw new Error(`HTTP ${response.status}`);

        }


        const data = await response.json();


        renderLiveExecution(data);


    } catch (error) {

        console.error(

            "Could not load live execution data:",

            error

        );


        updateLiveStatus("OFFLINE");


        if (liveElements.executionMessage) {

            liveElements.executionMessage.textContent =

                "Unable to connect to the live execution service.";

        }


        if (liveElements.progressNote) {

            liveElements.progressNote.textContent =

                "Check that the Java dashboard backend is running.";

        }

    } finally {

        isFetchingLive = false;

    }

}


// ==========================================
// INITIALISE LIVE MONITOR
// ==========================================

function initialiseLiveMonitor() {

    getLiveElements();


    // --------------------------------------
    // INITIAL DATA LOAD
    // --------------------------------------

    fetchAndRenderLiveExecution();

    fetchAndRenderActivity();


    // --------------------------------------
    // CLEAR EXISTING TIMERS
    // --------------------------------------

    if (pollingInterval) {

        clearInterval(pollingInterval);

    }


    if (activityInterval) {

        clearInterval(activityInterval);

    }


    if (durationInterval) {

        clearInterval(durationInterval);

    }


    // --------------------------------------
    // LIVE EXECUTION POLLING
    // Every 1 second
    // --------------------------------------

    pollingInterval = setInterval(() => {

        fetchAndRenderLiveExecution();

    }, 1000);


    // --------------------------------------
    // ACTIVITY FEED POLLING
    // Every 1 second
    // --------------------------------------

    activityInterval = setInterval(() => {

        fetchAndRenderActivity();

    }, 1000);


    // --------------------------------------
    // EXECUTION TIMER
    // Every 250 milliseconds
    // --------------------------------------

    durationInterval = setInterval(() => {

        if (

            latestExecution &&

            (latestExecution.status || "").toUpperCase() === "RUNNING"

        ) {

            updateDuration(latestExecution);

        }

    }, 250);


    console.log(

        "AutoTest Professional Live Monitor initialised successfully."

    );

}


// ==========================================
// START MONITOR
// ==========================================

if (document.readyState === "loading") {

    document.addEventListener(

        "DOMContentLoaded",

        initialiseLiveMonitor,

        { once: true }

    );

} else {

    initialiseLiveMonitor();

}
