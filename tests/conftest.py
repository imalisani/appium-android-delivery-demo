import os
import warnings
from pathlib import Path

import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from selenium.common.exceptions import WebDriverException

ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
ARTIFACTS = ROOT / "artifacts"


@pytest.fixture
def driver():
    app_path = Path(os.environ.get("APP_PATH", str(APK))).expanduser().resolve()
    if not app_path.is_file():
        pytest.fail(f"APK missing: {app_path}. Build :app:assembleDebug first.")

    capabilities = {
        "platformName": "Android",
        "appium:automationName": "UiAutomator2",
        "appium:app": str(app_path),
        "appium:deviceName": "Android Emulator",
        "appium:fullReset": True,
        "appium:autoGrantPermissions": False,
        "appium:disableWindowAnimation": True,
    }
    if udid := os.environ.get("ANDROID_SERIAL"):
        capabilities["appium:udid"] = udid

    server = os.environ.get("APPIUM_URL", "http://127.0.0.1:4723")
    options = UiAutomator2Options().load_capabilities(capabilities)
    session = webdriver.Remote(server, options=options)
    try:
        yield session
    finally:
        session.quit()


@pytest.hookimpl(wrapper=True)
def pytest_runtest_makereport(item, call):
    report = yield
    session = item.funcargs.get("driver")
    if report.when != "call" or session is None:
        return report

    try:
        ARTIFACTS.mkdir(exist_ok=True)
        screenshot_path = ARTIFACTS / f"{item.name}.png"
        if not session.save_screenshot(str(screenshot_path)):
            warnings.warn(f"Could not write screenshot: {screenshot_path}", stacklevel=1)
        if report.failed:
            (ARTIFACTS / f"{item.name}.xml").write_text(session.page_source, encoding="utf-8")
    except (OSError, WebDriverException) as error:
        warnings.warn(f"Could not collect Android evidence for {item.name}: {error}", stacklevel=1)
    return report
