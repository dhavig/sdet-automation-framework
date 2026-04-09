Feature: SauceDemo Login

  Scenario: Successful login with valid credentials
    Given I am on the SauceDemo login page
    When I login with username "standard_user" and password "secret_sauce"
    Then I should be redirected to the Products page

  Scenario: Failed login with invalid credentials
    Given I am on the SauceDemo login page
    When I login with username "invalid_user" and password "wrong_pass"
    Then I should see a login error message

  Scenario: Failed login with empty credentials
    Given I am on the SauceDemo login page
    When I login with username "" and password ""
    Then I should see a login error message
