import pytest

from delivery_page import PACKAGE, DeliveryPage


@pytest.mark.negative
def test_confirmation_is_blocked_without_photo(driver):
    """POD-01: the initial delivery cannot be completed without evidence."""
    page = DeliveryPage(driver)

    assert page.read("photo_status").text == "No photo attached"
    assert not page.read("confirm_delivery").is_enabled()
    assert page.read("delivery_status").text == "Delivery pending"


@pytest.mark.negative
def test_denied_camera_permission_keeps_delivery_pending(driver):
    """POD-02: denying the Android system permission does not attach evidence."""
    page = DeliveryPage(driver)
    page.tap("add_photo")
    page.respond_to_camera_permission(allow=False)

    page.wait_for_photo_status("Camera permission denied")
    assert not page.read("confirm_delivery").is_enabled()
    assert page.read("delivery_status").text == "Delivery pending"


@pytest.mark.smoke
def test_delivery_can_be_confirmed_with_captured_photo(driver):
    """POD-03: a captured photo enables confirmation and survives reopening."""
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
