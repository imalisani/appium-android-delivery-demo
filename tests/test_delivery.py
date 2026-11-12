import pytest

from delivery_page import PACKAGE, DeliveryPage


@pytest.mark.negative
def test_pod_01_confirmation_is_blocked_without_photo(driver):
    page = DeliveryPage(driver)

    assert page.read("photo_status").text == "No photo attached"
    assert not page.read("confirm_delivery").is_enabled()
    assert page.read("delivery_status").text == "Delivery pending"


@pytest.mark.negative
def test_pod_02_denied_camera_permission_keeps_delivery_pending(driver):
    page = DeliveryPage(driver)
    page.tap("add_photo")
    page.respond_to_camera_permission(allow=False)

    page.wait_for_photo_status("Camera permission denied")
    assert not page.read("confirm_delivery").is_enabled()
    assert page.read("delivery_status").text == "Delivery pending"


@pytest.mark.smoke
def test_pod_03_delivery_can_be_confirmed_with_captured_photo(driver):
    page = DeliveryPage(driver)
    page.tap("add_photo")
    page.respond_to_camera_permission(allow=True)
    page.tap("take_photo")

    page.wait_for_photo_status("Photo attached")
    assert page.read("attached_photo").is_displayed()
    assert page.read("delivery_status").text == "Delivery pending"

    page.tap("confirm_delivery")
    assert page.read("delivery_status").text == "Delivery confirmed"

    driver.terminate_app(PACKAGE)
    driver.activate_app(PACKAGE)

    assert page.read("delivery_status").text == "Delivery confirmed"
    assert page.read("attached_photo").is_displayed()
