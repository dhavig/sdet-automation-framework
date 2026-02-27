package com.sdet.selenium;

import com.sdet.pages.LoginPage;
import com.sdet.pages.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    @Story("User Login")
    @Description("Verify successful login with valid credentials")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidLogin() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("standard_user", "secret_sauce");

        ProductsPage productsPage = new ProductsPage();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(),
                "Products page should be displayed after login");
        Assert.assertEquals(productsPage.getPageTitle(), "Products",
                "Page title should be Products");
    }

    @Test
    @Story("User Login")
    @Description("Verify login fails with invalid credentials")
    @Severity(SeverityLevel.NORMAL)
    public void testInvalidLogin() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("invalid_user", "invalid_password");

        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage()
                        .contains("Username and password do not match"),
                "Error message should match");
    }

    @Test
    @Story("User Login")
    @Description("Verify login fails with empty credentials")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyCredentials() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("", "");

        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage()
                        .contains("Username is required"),
                "Error message should mention username required");
    }

    @Test
    @Story("User Login")
    @Description("Verify locked out user cannot login")
    @Severity(SeverityLevel.NORMAL)
    public void testLockedOutUser() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("locked_out_user", "secret_sauce");

        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage()
                        .contains("Sorry, this user has been locked out"),
                "Error message should mention locked out");
    }
}