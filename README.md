Parallel Testing Module Plan
What it will do:

Run the same test scenarios against two environments simultaneously (e.g., staging vs production, or legacy vs cloud)
Capture responses from both environments
Compare outputs automatically and flag differences
Generate a comparison report via Allure

Classes we'll create:

EnvironmentConfig.java — holds config for two environments (baseUrl1, baseUrl2)
ParallelExecutor.java — runs tests against both environments using Java threads
ResponseComparator.java — compares outputs and logs differences
ParallelBaseTest.java — base test class for parallel execution
ParallelLoginTest.java — actual side-by-side test using SauceDemo

File Placement Guide
FileLocation in 
Projectconfig.properties    src/test/resources/config/config.properties (replace existing)
EnvironmentConfig.java      src/main/java/config/
ParallelTestResult.java     src/main/java/parallel/
ResponseComparator.java     src/main/java/parallel/
ParallelExecutor.java       src/main/java/parallel/
ParallelLoginTest.java      src/test/java/tests/parallel/

What This Module Does

Runs two browser sessions simultaneously against ENV1 (standard_user) and ENV2 (performance_glitch_user)
Compares outputs — page titles, product counts, pass/fail status
Flags SLA breaches if any environment takes >5 seconds
Generates Allure report with a detailed comparison attachment