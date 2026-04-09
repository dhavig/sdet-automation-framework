package com.sdet.cucumber.steps;

import com.sdet.config.ConfigReader;
import com.sdet.pages.LoginPage;
import com.sdet.pages.ProductsPage;
import com.sdet.utils.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class LoginSteps {

    private LoginPage loginPage;
    private ProductsPage productsPage;

    @Before
    public void setUp() {
        DriverManager.initDriver();
        DriverManager.getDriver().get(ConfigReader.getBaseUrl());
        loginPage   = new LoginPage();
        productsPage = new ProductsPage();
    }

    @After
    public void tearDown() {
        DriverManager.quitDriver();
    }

    @Given("I am on the SauceDemo login page")
    public void iAmOnTheSauceDemoLoginPage() {
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
                "Login page should be displayed");
    }

    @When("I login with username {string} and password {string}")
    public void iLoginWithUsernameAndPassword(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("I should be redirected to the Products page")
    public void iShouldBeRedirectedToTheProductsPage() {
        Assert.assertTrue(productsPage.isProductsPageDisplayed(),
                "Products page should be displayed after successful login");
    }

    @Then("I should see a login error message")
    public void iShouldSeeALoginErrorMessage() {
        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be displayed for invalid login");
    }
}
