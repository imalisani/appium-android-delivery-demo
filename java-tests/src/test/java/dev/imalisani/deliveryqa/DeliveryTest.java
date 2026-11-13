package dev.imalisani.deliveryqa;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryTest {
    private static final Path EVIDENCE = Path.of("java-tests", "target", "evidence");
    private static final String DEFAULT_APK = "app/build/outputs/apk/debug/app-debug.apk";
    private static final String PENDING = "Delivery pending";
    private static final String CONFIRMED = "Delivery confirmed";

    private AndroidDriver driver;
    private DeliveryPage page;

    @RegisterExtension
    final AfterTestExecutionCallback evidence = this::captureEvidence;

    @BeforeEach
    void startSession() throws IOException {
        Path apk = Path.of(System.getenv().getOrDefault("APP_PATH", DEFAULT_APK)).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(apk), () -> "APK missing: " + apk + ". Build :app:assembleDebug first.");

        UiAutomator2Options options = new UiAutomator2Options()
                .setApp(apk.toString())
                .setDeviceName("Android Emulator")
                .setFullReset(true)
                .setAutoGrantPermissions(false)
                .setDisableWindowAnimation(true);
        String serial = System.getenv("ANDROID_SERIAL");
        if (serial != null && !serial.isBlank()) {
            options.setUdid(serial);
        }

        URL server = URI.create(System.getenv().getOrDefault("APPIUM_URL", "http://127.0.0.1:4723")).toURL();
        driver = new AndroidDriver(server, options);
        page = new DeliveryPage(driver);
    }

    @AfterEach
    void stopSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Tag("negative")
    @DisplayName("POD-01 Confirmation is blocked without a photo")
    void pod01ConfirmationIsBlockedWithoutPhoto() {
        assertEquals("No photo attached", page.photoStatus());
        assertFalse(page.canConfirmDelivery(), "A delivery without evidence must stay locked");
        assertEquals(PENDING, page.deliveryStatus());
    }

    @Test
    @Tag("negative")
    @DisplayName("POD-02 Denying camera permission leaves the delivery pending")
    void pod02DeniedPermissionKeepsDeliveryPending() {
        page.requestPhoto();
        page.denyCameraPermission();

        String status = page.photoStatus();
        assertTrue(status.startsWith("Camera permission denied"), () -> "Unexpected photo status: " + status);
        assertFalse(page.canConfirmDelivery(), "Denying camera access must keep confirmation locked");
        assertEquals(PENDING, page.deliveryStatus());
    }

    @Test
    @Tag("smoke")
    @DisplayName("POD-03 Confirmation and photo survive reopening the app")
    void pod03ConfirmedDeliverySurvivesReopening() {
        page.requestPhoto();
        page.allowCameraPermission();
        page.capturePhoto();

        assertTrue(page.hasVisiblePhoto(), "The attached photo should be visible");
        assertEquals(PENDING, page.deliveryStatus());
        assertTrue(page.canConfirmDelivery(), "A captured photo should unlock confirmation");

        page.confirmDelivery();
        assertEquals(CONFIRMED, page.deliveryStatus());

        assertTrue(driver.terminateApp(DeliveryPage.APP_PACKAGE), "The app must stop before reopening");
        driver.activateApp(DeliveryPage.APP_PACKAGE);

        assertEquals(CONFIRMED, page.deliveryStatus());
        assertEquals("Photo attached", page.photoStatus());
        assertTrue(page.hasVisiblePhoto(), "The photo should survive reopening");
    }

    private void captureEvidence(ExtensionContext context) {
        String name = context.getRequiredTestMethod().getName();
        try {
            Files.createDirectories(EVIDENCE);
            Files.write(EVIDENCE.resolve(name + ".png"), driver.getScreenshotAs(OutputType.BYTES));
        } catch (IOException | WebDriverException error) {
            System.err.println("Could not save screenshot for " + name + ": " + error.getMessage());
        }

        if (context.getExecutionException().isPresent()) {
            try {
                Files.writeString(EVIDENCE.resolve(name + ".xml"), driver.getPageSource());
            } catch (IOException | WebDriverException error) {
                System.err.println("Could not save UI hierarchy for " + name + ": " + error.getMessage());
            }
        }
    }
}
