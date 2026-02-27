package com.sdet.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import java.util.List;

public class ProductsPage extends BasePage {

    @FindBy(className = "title")
    private WebElement pageTitle;

    @FindBy(className = "inventory_item")
    private List<WebElement> productItems;

    @FindBy(className = "shopping_cart_link")
    private WebElement cartIcon;

    @FindBy(className = "shopping_cart_badge")
    private WebElement cartBadge;

    @FindBy(css = ".product_sort_container")
    private WebElement sortDropdown;

    @FindBy(css = ".btn_inventory")
    private List<WebElement> addToCartButtons;

    // Constructor
    public ProductsPage() {
        super();
    }

    // Actions
    public String getPageTitle() {
        return getText(pageTitle);
    }

    public boolean isProductsPageDisplayed() {
        return isDisplayed(pageTitle);
    }

    public int getProductCount() {
        return productItems.size();
    }

    public void addFirstProductToCart() {
        click(addToCartButtons.get(0));
    }

    public void addProductToCart(int index) {
        click(addToCartButtons.get(index));
    }

    public String getCartCount() {
        return getText(cartBadge);
    }

    public void clickCartIcon() {
        click(cartIcon);
    }

    public boolean isCartBadgeDisplayed() {
        return isDisplayed(cartBadge);
    }

    public List<WebElement> getAllProducts() {
        return productItems;
    }
}