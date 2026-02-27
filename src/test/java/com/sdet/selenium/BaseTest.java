package com.sdet.selenium;

import com.sdet.config.ConfigReader;
import com.sdet.utils.DriverManager;
import com.sdet.utils.ScreenshotUtil;
import io.qameta.allure.Attachment;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    @BeforeMethod
    public void setUp() {
        DriverManager.initDriver();
        DriverManager.getDriver().get(ConfigReader.getBaseUrl());
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        // Take screenshot on failure
        if (result.getStatus() == ITestResult.FAILURE) {
            takeScreenshotOnFailure();
            ScreenshotUtil.takeScreenshot(
                    DriverManager.getDriver(),
                    result.getName()
            );
        }
        DriverManager.quitDriver();
    }

    @Attachment(value = "Screenshot on Failure", type = "image/png")
    public byte[] takeScreenshotOnFailure() {
        return ScreenshotUtil.takeScreenshotAsBytes(DriverManager.getDriver());
    }
}