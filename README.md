# AutoTest QA Platform

**A Java-based test automation and quality assurance platform for web applications and REST APIs.**

AutoTest QA Platform is an automation framework designed to execute, monitor and analyse software tests through a centralized dashboard. It combines browser automation, API testing, test execution management and performance analytics to provide visibility into software quality.

## Project Overview

The platform automates software testing using Selenium WebDriver and REST Assured, with TestNG managing test execution.

It also includes a custom web-based dashboard that provides test execution summaries, live monitoring, execution history and performance analytics.

The goal is to make automated testing easier to execute, monitor and understand.

## Key Features

### 1. Web UI Automation
- Automated browser testing using Selenium WebDriver.
- Automated interaction with web application elements.
- Verification of page content and application behaviour.
- Browser-based functional testing.

### 2. REST API Automation
- API testing using REST Assured.
- HTTP request and response validation.
- Verification of API operations.
- Automated API test execution.

### 3. Test Execution Management
- Test execution through TestNG.
- Individual API and Web test suites.
- Combined test execution.
- Execution result tracking.
- Pass, fail and skipped test reporting.

### 4. Live Execution Monitoring
- Real-time execution status.
- Live test activity tracking.
- Execution progress monitoring.
- Execution event logging.

### 5. Quality Assurance Dashboard
- Centralized testing dashboard.
- Total execution statistics.
- Passed, failed and skipped test counts.
- Overall pass-rate calculations.
- Quality status summaries.

### 6. Performance Analytics
- Test execution duration analysis.
- Fastest and slowest test identification.
- Performance trend monitoring.
- Historical execution comparisons.
- CSV-based execution history.

## Technology Stack

| Category | Technology |
|---|---|
| Programming Language | Java |
| UI Automation | Selenium WebDriver |
| API Automation | REST Assured |
| Test Framework | TestNG |
| Build Management | Apache Maven |
| JSON Processing | Jackson |
| Dashboard Backend | Java HTTP Server |
| Frontend | HTML5, CSS3, JavaScript |
| Reporting | ExtentReports |
| Data Storage | CSV and JSON |
| Development Environment | Windows 11 |

## System Architecture

```text
             AUTOTEST QA PLATFORM
                       |
          +------------+------------+
          |                         |
     WEB AUTOMATION             API AUTOMATION
          |                         |
     Selenium WebDriver         REST Assured
          |                         |
          +------------+------------+
                       |
                 TESTNG ENGINE
                       |
              TEST EXECUTION MANAGER
                       |
          +------------+------------+
          |                         |
    LIVE MONITORING           TEST REPORTING
          |                         |
          +------------+------------+
                       |
                QA DASHBOARD
                       |
              PERFORMANCE ANALYTICS
```

## Dashboard Modules

| Module | Description |
|---|---|
| Overview | Displays overall testing statistics. |
| Execution Centre | Starts and manages test suites. |
| Live Monitor | Tracks test execution activity. |
| Execution History | Displays previous test executions. |
| Performance Analytics | Analyses test performance and trends. |

## Project Structure

```text
autotest-qa-platform/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── za/co/autotest/
│   │   │       └── dashboard/
│   │   └── resources/
│   │       └── dashboard/
│   │
│   └── test/
│       ├── java/
│       │   └── za/co/autotest/
│       └── resources/
│           ├── api-suite.xml
│           ├── web-suite.xml
│           └── testng.xml
│
├── test-reports/
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

Install the following:

- Java Development Kit (JDK 25)
- Apache Maven 3.10.0 or compatible version
- Git
- Google Chrome or another supported browser

### 1. Clone the repository

```bash
git clone https://github.com/thabisomadonsela171-crypto/autotest-qa-platform.git
```

### 2. Navigate into the project

```bash
cd autotest-qa-platform
```

### 3. Verify Java

```bash
java -version
```

### 4. Verify Maven

```bash
mvn -version
```

### 5. Build the project

```bash
mvn clean compile
```

### 6. Execute automated tests

```bash
mvn test
```

Use the configured TestNG suite files to execute the relevant API, Web or combined test suites.

## Example Test Scenarios

### Web Testing
- Opening a website.
- Verifying page titles and headings.
- Locating and interacting with page elements.
- Validating product information.
- Checking application navigation.

### API Testing
- Sending HTTP requests.
- Creating records.
- Retrieving records.
- Updating existing records.
- Validating API responses.

## Reporting and Analytics

The platform maintains execution information to support quality analysis, including:

- Execution history.
- Test results.
- Execution durations.
- Pass-rate statistics.
- Live execution activity.
- Performance trends.

The dashboard provides a centralized view of these results to help identify failures and performance issues.

## Engineering Concepts Demonstrated

This project demonstrates practical experience with:

- Object-oriented programming in Java.
- Automated software testing.
- Web browser automation.
- REST API testing.
- Test suite management.
- Maven dependency management.
- Test reporting and analytics.
- Dashboard development.
- Software quality assurance.

## Future Improvements

Potential enhancements include:

- CI/CD pipeline integration.
- Automated scheduled test execution.
- Cross-browser testing.
- Expanded API test coverage.
- Advanced failure diagnostics.
- Database-backed execution history.
- Cloud-based test execution.

## Author

**Thabiso Erick Madonsela**

Software Development | Quality Assurance | Test Automation

GitHub: [@thabisomadonsela171-crypto](https://github.com/thabisomadonsela171-crypto)

## Repository

[AutoTest QA Platform – GitHub](https://github.com/thabisomadonsela171-crypto/autotest-qa-platform)

---

*Developed as a software development and quality assurance portfolio project, demonstrating practical implementation of automated testing, execution monitoring and performance analytics.*
