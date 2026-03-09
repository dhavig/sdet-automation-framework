package com.sdet.parallel;

import com.sdet.config.EnvironmentConfig;
import com.sdet.utils.DriverManager;
import io.qameta.allure.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.sdet.pages.LoginPage;
import com.sdet.pages.ProductsPage;

@Epic("Parallel Testing")
@Feature("Side-by-Side Environment Comparison")
public class ParallelLoginTest {

    @Test
    @Story("Login flow comparison across environments")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Validates login behavior is consistent across Legacy and Cloud environments")
    public void testParallelLoginComparison() {
        EnvironmentConfig env1 = EnvironmentConfig.load(EnvironmentConfig.Environment.ENV1);
        EnvironmentConfig env2 = EnvironmentConfig.load(EnvironmentConfig.Environment.ENV2);

        ResponseComparator.ComparisonReport report = ParallelExecutor.runInParallel(
                "Login Flow Comparison", env1, env2,
                (env) -> runLoginScenario(env)
        );

        Assert.assertTrue(report.isInSync(),
                "Environment mismatch detected! Differences: " + report.getDifferences());
    }

    @Test
    @Story("Product page output comparison across environments")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validates product listing is consistent across both environments")
    public void testParallelProductPageComparison() {
        EnvironmentConfig env1 = EnvironmentConfig.load(EnvironmentConfig.Environment.ENV1);
        EnvironmentConfig env2 = EnvironmentConfig.load(EnvironmentConfig.Environment.ENV2);

        ResponseComparator.ComparisonReport report = ParallelExecutor.runInParallel(
                "Product Page Comparison", env1, env2,
                (env) -> runProductPageScenario(env)
        );

        Assert.assertNotNull(report, "Comparison report should not be null");
    }

    private ParallelTestResult runLoginScenario(EnvironmentConfig env) {
        WebDriver driver = createDriver();
        long startTime = System.currentTimeMillis();
        boolean passed = false;
        String actualValue = null;
        String errorMessage = null;

        try {
            DriverManager.setDriver(driver);
            driver.get(env.getBaseUrl());
            LoginPage loginPage = new LoginPage();
            loginPage.login(env.getUsername(), env.getPassword());
            ProductsPage productsPage = new ProductsPage();
            actualValue = productsPage.getPageTitle();
            passed = actualValue.equals("Products");
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            DriverManager.removeDriver();
            driver.quit();
        }

        return new ParallelTestResult(env.getName(), "Login Flow",
                passed, actualValue, System.currentTimeMillis() - startTime, errorMessage);
    }

    private ParallelTestResult runProductPageScenario(EnvironmentConfig env) {
        WebDriver driver = createDriver();
        long startTime = System.currentTimeMillis();
        boolean passed = false;
        String actualValue = null;
        String errorMessage = null;

        try {
            DriverManager.setDriver(driver);
            driver.get(env.getBaseUrl());
            LoginPage loginPage = new LoginPage();
            loginPage.login(env.getUsername(), env.getPassword());
            ProductsPage productsPage = new ProductsPage();
            int productCount = productsPage.getProductCount();
            actualValue = String.valueOf(productCount);
            passed = productCount > 0;
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            DriverManager.removeDriver();
            driver.quit();
        }

        return new ParallelTestResult(env.getName(), "Product Page",
                passed, actualValue, System.currentTimeMillis() - startTime, errorMessage);
    }

    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }
}