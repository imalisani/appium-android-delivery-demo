# Proof of delivery · Android Appium demo

Mobile QA portfolio project by Irina Malisani. A driver can confirm synthetic
delivery `DEMO-101` after attaching a photo. The native Android app requests the
system camera permission, captures a JPEG with CameraX and stores it locally
along with the delivery status. The demo uses one delivery and has no backend.

**ES** · Demo de automatización mobile para una entrega ficticia. La suite
verifica el bloqueo sin foto, el permiso de cámara denegado y la confirmación
con una foto capturada desde el emulador.

## What the suite verifies

| Scenario | Risk | Observable result |
| --- | --- | --- |
| POD-01 · Confirm without a photo | Critical · incomplete delivery evidence | Confirmation is disabled and the delivery stays pending. |
| POD-02 · Deny camera access | High · permission handling blocks the workflow | The app explains the denial and leaves the delivery pending. |
| POD-03 · Capture and confirm | Critical · missing evidence or lost delivery status | The photo is visible, confirmation becomes possible, and both the photo and status survive reopening the app. |

[Gherkin scenarios](docs/proof-of-delivery.feature) ·
[Java tests](java-tests/src/test/java/dev/imalisani/deliveryqa/DeliveryTest.java) ·
[Python tests](tests/test_delivery.py) ·
[GitHub Actions runs](https://github.com/imalisani/appium-android-delivery-demo/actions/workflows/android-appium.yml)

Each scenario starts with a clean installation. Appium interacts with the
Android permission prompt and the app UI. The happy path takes a photo using
the emulator's camera. It checks both the attached image and the delivery status.
The Java suite uses JUnit 5 and Maven. Its [Page Object](java-tests/src/test/java/dev/imalisani/deliveryqa/DeliveryPage.java)
uses resource IDs and explicit waits for the camera and delivery state.

## Run locally

**Prerequisites** · Android Studio and SDK Platform 35, Build-Tools 34.0.0,
platform-tools, an Android 14 (API 34) emulator using the Google APIs image,
JDK 17 or 21, Node.js 22.12+ in the 22.x series, and npm 10+.
Install Maven 3.9+ for the Java suite or Python 3.12 for the Python suite.
Configure `ANDROID_HOME` and `JAVA_HOME`, and add the SDK's
`platform-tools` directory to `PATH`. The Gradle Wrapper pins Gradle 8.9.

In Android Studio's Device Manager, start an API 34 AVD such as a Pixel 5 with
the back camera enabled. You can also launch it with
`emulator -avd YOUR_AVD -camera-back emulated`. Check that `adb devices` lists
it as `device`. With more than one Android device connected, set
`ANDROID_SERIAL` to the serial for your emulator.

For a quick manual run, download the debug APK from a completed GitHub Actions run.
Install it with `adb install -r app-debug.apk` and open **Delivery QA** on the
emulator. To test that APK, set `APP_PATH` to its path in the terminal that runs
the tests. Both suites accept `APPIUM_URL` when the server uses a different URL.

### Java · Appium and JUnit 5

On Windows PowerShell, build the APK and start Appium from the project root:

```powershell
.\gradlew.bat :app:assembleDebug
npm.cmd install --global appium@3.2.0
appium.cmd driver install uiautomator2@6.7.0
appium.cmd --address 127.0.0.1 --port 4723
```

In another PowerShell terminal at the project root, run the full Java suite or
select the confirmation scenario by method name:

```powershell
mvn -f java-tests/pom.xml test
mvn -f java-tests/pom.xml '-Dtest=DeliveryTest#pod03ConfirmedDeliverySurvivesReopening' test
```

On macOS or Linux, build the APK, install Appium and its driver, then let the
script start and stop the Appium server:

```bash
./gradlew :app:assembleDebug
npm install --global appium@3.2.0
appium driver install uiautomator2@6.7.0
bash scripts/run-android-tests.sh java
```

### Python · Appium and Pytest

On Windows, reuse the Appium setup above and run these commands from the project root:

```powershell
py -3.12 -m venv .venv
& .\.venv\Scripts\python.exe -m pip install -r requirements.txt
New-Item -ItemType Directory -Force artifacts | Out-Null
& .\.venv\Scripts\python.exe -m pytest -q --junitxml=artifacts/junit.xml
```

On macOS or Linux, you can build and run the suite with:

```bash
./gradlew :app:assembleDebug
npm install --global appium@3.2.0
appium driver install uiautomator2@6.7.0
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
bash scripts/run-android-tests.sh
```

### Execution evidence

The GitHub Actions workflow runs both suites on the same Android 14 emulator.
Each scenario saves a screenshot, and a failed test also saves the Android UI hierarchy.

| Artifact | Contents |
| --- | --- |
| `android-appium-java-evidence` | Java screenshots and failure UI hierarchies from `java-tests/target/evidence/`, plus JUnit XML from `java-tests/target/surefire-reports/` |
| `android-appium-evidence` | Python screenshots, failure UI hierarchies, Pytest JUnit XML and the shared Appium server log from `artifacts/` |

The suite covers a single Android delivery with a local photo. Backend upload,
offline synchronization and iOS are outside the scope of this demo.
