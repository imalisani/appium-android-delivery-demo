package dev.imalisani.deliveryqa;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

final class DeliveryPage {
    static final String APP_PACKAGE = "dev.imalisani.deliveryqa";

    private static final By ADD_PHOTO = resource("add_photo");
    private static final By TAKE_PHOTO = resource("take_photo");
    private static final By CONFIRM_DELIVERY = resource("confirm_delivery");
    private static final By PHOTO_STATUS = resource("photo_status");
    private static final By DELIVERY_STATUS = resource("delivery_status");
    private static final By ATTACHED_PHOTO = resource("attached_photo");
    private static final By ALLOW_CAMERA = permissionButton("permission_allow_foreground_only_button");
    private static final By DENY_CAMERA = permissionButton("permission_deny_button");

    private final WebDriverWait wait;

    DeliveryPage(AndroidDriver driver) {
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    void requestPhoto() {
        tap(ADD_PHOTO);
    }

    void denyCameraPermission() {
        tap(DENY_CAMERA);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(PHOTO_STATUS, "Camera permission denied"));
    }

    void allowCameraPermission() {
        tap(ALLOW_CAMERA);
    }

    void capturePhoto() {
        tap(TAKE_PHOTO);
        wait.until(ExpectedConditions.textToBe(PHOTO_STATUS, "Photo attached"));
    }

    void confirmDelivery() {
        tap(CONFIRM_DELIVERY);
        wait.until(ExpectedConditions.textToBe(DELIVERY_STATUS, "Delivery confirmed"));
    }

    String photoStatus() {
        return visible(PHOTO_STATUS).getText();
    }

    String deliveryStatus() {
        return visible(DELIVERY_STATUS).getText();
    }

    boolean canConfirmDelivery() {
        return visible(CONFIRM_DELIVERY).isEnabled();
    }

    boolean hasVisiblePhoto() {
        return visible(ATTACHED_PHOTO).isDisplayed();
    }

    private void tap(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    private WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private static By resource(String name) {
        return AppiumBy.id(APP_PACKAGE + ":id/" + name);
    }

    private static By permissionButton(String name) {
        return AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\".*:id/" + name + "\")");
    }
}
