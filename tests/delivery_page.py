from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support import expected_conditions as ec
from selenium.webdriver.support.ui import WebDriverWait

PACKAGE = "dev.imalisani.deliveryqa"


class DeliveryPage:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 25)

    @staticmethod
    def locator(name):
        return AppiumBy.ID, f"{PACKAGE}:id/{name}"

    def read(self, name):
        return self.wait.until(ec.visibility_of_element_located(self.locator(name)))

    def tap(self, name):
        self.wait.until(ec.element_to_be_clickable(self.locator(name))).click()

    def respond_to_camera_permission(self, *, allow):
        # Google APIs and AOSP images use different PermissionController package names.
        button = "permission_allow_foreground_only_button" if allow else "permission_deny_button"
        locator = (
            AppiumBy.ANDROID_UIAUTOMATOR,
            f'new UiSelector().resourceIdMatches(".*:id/{button}")',
        )
        self.wait.until(ec.element_to_be_clickable(locator)).click()

    def wait_for_photo_status(self, text):
        self.wait.until(ec.text_to_be_present_in_element(self.locator("photo_status"), text))
