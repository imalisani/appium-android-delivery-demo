Feature: Confirm a delivery with photographic evidence
  As a driver
  I want to attach a photo of the completed delivery
  So that I can confirm the delivery with evidence

  Background:
    Given synthetic delivery DEMO-101 is pending
    And it has no attached photo
    And camera permission has not been granted

  @POD-01 @critical @negative
  Scenario: A delivery without evidence cannot be confirmed
    When I open the delivery
    Then the app shows that no photo is attached
    And the confirmation button is disabled
    And the delivery remains pending

  @POD-02 @high @negative
  Scenario: Denying camera permission leaves the delivery pending
    When I choose to add a photo
    And I deny the Android camera permission
    Then the app explains that the camera permission was denied
    And the confirmation button is disabled
    And the delivery remains pending

  @POD-03 @critical @smoke
  Scenario: A captured photo enables confirmation
    When I choose to add a photo
    And I allow the Android camera permission
    And I take a photo
    Then the app displays the attached photo
    And the delivery remains pending until I confirm it
    When I confirm the delivery and reopen the app
    Then the delivery is confirmed
    And the attached photo is still visible
