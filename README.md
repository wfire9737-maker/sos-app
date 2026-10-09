# Physical Emergency SOS Button with Live Location Tracking

An Android-based emergency response system that combines a mobile safety application with a physical ESP32-powered SOS button. The project is designed to help a user trigger an emergency alert and share relevant location and emergency information with trusted contacts.

> **Project status:** Features and hardware behavior must be validated on the target device before real-world or emergency use. See [Limitations and Safety](#limitations-and-safety).

## Overview

The system brings together:

- An Android application built with **Kotlin** and **Jetpack Compose**
- An **ESP32** device that communicates with the Android app over Bluetooth Low Energy (BLE)
- Emergency workflows for sending alerts and sharing location
- Additional safety features such as voice SOS, fall-detection support, safety timers, and nearby-device discovery, where configured and supported

## Key Features

The project includes or is designed to support the following capabilities. Availability depends on the current implementation, permissions, device compatibility, and configuration.

- **Manual SOS:** Trigger an emergency workflow from the Android app.
- **Physical SOS button:** Receive an SOS event from a paired ESP32 device over BLE.
- **Live location:** Obtain and share the phone's location when permission and location services are available.
- **Emergency contacts:** Manage contacts used by emergency workflows.
- **Calls and SMS:** Initiate emergency communication where the app, Android version, permissions, SIM, and network allow it.
- **Voice SOS:** Voice-triggered emergency activation, subject to the app's current listening mode and Android background-execution restrictions.
- **Fall detection:** Support for fall-event detection when compatible sensor hardware and the relevant implementation are configured.
- **Safety timers and trusted places:** Additional safety-oriented workflows.
- **Nearby BLE discovery and messaging:** Discover nearby app-compatible devices and exchange emergency-related messages where supported.
- **Settings and accessibility:** Theme, high-contrast, and other safety preferences.
- **Firebase integration:** Authentication and cloud data storage, subject to project configuration and published Firestore Security Rules.

## Technology Stack

### Android

- Kotlin
- Jetpack Compose
- Android Bluetooth Low Energy APIs
- Android location and notification APIs
- SharedPreferences and/or the project's configured persistence layer
- Firebase Authentication and Cloud Firestore, where enabled

### Hardware

- ESP32 development board
- Physical push button
- Optional NEO-6M GPS module
- Optional MPU6050 accelerometer/gyroscope for fall-detection experiments
- Battery and supporting wiring/components appropriate to the board and sensors

### Development tools

- Android Studio
- Google AI Studio (used in parts of the development workflow)
- Arduino IDE or a compatible ESP32 firmware toolchain
- Firebase Console / Firebase CLI for Firebase configuration and rules testing

## System Architecture

```text
[Physical SOS Button]
          |
          v
       [ESP32]
          |
          | Bluetooth Low Energy (BLE)
          v
 [Android SOS Application]
          |
          +--> Emergency workflow
          +--> Location retrieval and sharing
          +--> Emergency contacts / calls / SMS
          +--> Notifications and safety features
          |
          v
 [Firebase services, if configured]
```

The Android phone is responsible for app-level emergency workflows and phone location. The ESP32 communicates hardware events and device status over BLE. Actual behavior depends on the firmware, app implementation, Android permissions, connectivity, and device configuration.

## BLE Configuration

The firmware and Android BLE implementation use the following identifiers. Keep them consistent between the ESP32 firmware and Android app.

| Item | Value |
|---|---|
| BLE device name | `Physical-SOS-ESP32` |
| Service UUID | `4fafc201-1fb5-459e-8fcc-c5e9c331914b` |
| Status characteristic UUID | `beb54803-36e1-4688-b7f5-ea07361b26a8` |
| Battery characteristic UUID | `beb54804-36e1-4688-b7f5-ea07361b26a8` |
| GPS characteristic UUID | `beb54805-36e1-4688-b7f5-ea07361b26a8` |
| MPU6050 characteristic UUID | `beb54806-36e1-4688-b7f5-ea07361b26a8` |

The GPS and MPU6050 characteristics are relevant only if those sensors and their firmware support are implemented and configured.

## Repository Structure

The exact structure may evolve. Important areas of the Android codebase include:

```text
app/src/main/java/com/example/
├── ble/
│   ├── BleManager.kt
│   └── nearby/
├── service/
│   ├── EmergencyService.kt
│   ├── EmergencyProvider.kt
│   ├── LocationService.kt
│   ├── SecurityService.kt
│   ├── AuthService.kt
│   ├── NotificationService.kt
│   ├── SafetyTimerService.kt
│   ├── AiAnalysisService.kt
│   └── AIService.kt
└── ...
firestore.rules
firebase.json
app/google-services.json
```

This is a guide to notable paths, not a complete file listing. Some files or features may differ in a particular checkout.

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/wfire9737-maker/sos-app.git
cd sos-app
```

If the repository URL or default branch changes, use the URL shown on the repository's GitHub page.

### 2. Open the Android project

1. Open the project in Android Studio.
2. Allow Gradle to sync and resolve dependencies.
3. Review the app's SDK and build configuration.
4. Connect an Android phone or start a compatible emulator.

BLE hardware features require a compatible physical Android device and ESP32; an emulator alone cannot validate the complete hardware workflow.

### 3. Configure Firebase (if using cloud features)

1. Open the Firebase project associated with your own deployment.
2. Register the Android app using the correct application ID.
3. Download the matching `google-services.json` and place it at the expected app module path.
4. Enable the Firebase products required by the implementation, such as Authentication and Cloud Firestore.
5. Review and publish the intended Firestore Security Rules.
6. Run the repository's rules tests if the test setup is present.

**Security:** Never publish service-account private keys, admin SDK credentials, access tokens, passwords, or other secrets. A client `google-services.json` is app configuration, not an admin credential, but it should still correspond to the intended Firebase project. Do not leave permissive Firestore rules such as `allow read, write: if true;` in production.

### 4. Build and run

Use Android Studio's **Run** action, or build using the Gradle wrapper supplied with the project:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

Build success depends on the current source code, Gradle configuration, SDK installation, and Firebase setup.

### 5. Prepare the ESP32

1. Open the ESP32 firmware in the compatible firmware toolchain.
2. Confirm that the BLE name and UUIDs match the table above.
3. Wire the push button and any optional sensors according to the firmware's pin definitions.
4. Select the correct ESP32 board and port, then upload the firmware.
5. Use the app's BLE connection/test functionality to check the actual device connection and status.

**Important:** Do not assume a pin mapping, voltage arrangement, or battery-charging circuit from this README. Follow the actual firmware and board documentation, and verify the wiring before powering the circuit.

## Permissions and Device Requirements

Depending on enabled features and Android version, the app may require:

- Bluetooth and nearby-device permissions
- Location permission and enabled location services
- Notification permission
- Microphone permission for voice-triggered features
- Phone and SMS permissions or user-mediated alternatives for emergency communication
- Background-service permissions or exemptions where supported and justified

Grant only the permissions required by the features you use. Android may restrict background microphone use, Bluetooth scanning, location access, calls, and SMS. These restrictions can vary by Android release and device manufacturer.

## Firebase Security

The repository includes a Firestore rules file intended to restrict access to user-owned data. Before deploying:

- Confirm the rules currently published in Firebase Console match the reviewed `firestore.rules` file.
- Ensure the permissive catch-all rule `allow read, write: if true;` is absent.
- Run the rules tests, if included, against the local Firestore emulator.
- Verify that every client query and write follows the paths and ownership model enforced by the rules.
- Re-test cloud features after any rules or data-model change.

Local rules and tests do not automatically publish rules to Firebase Console. A successful local test is not proof that the live Firebase project has the same rules.

## Testing Checklist

Use this checklist before treating a build as ready for demonstration:

- [ ] App installs and launches on the target Android device.
- [ ] Required permissions are requested and handled correctly.
- [ ] ESP32 appears only when a real compatible device is detected.
- [ ] BLE connection, disconnection, and reconnection work reliably.
- [ ] A physical button press produces the intended app event.
- [ ] Location is real device location and unavailable states are handled clearly.
- [ ] Emergency contact details and communication actions are correct.
- [ ] Notifications and safety workflows behave as expected.
- [ ] Voice SOS and fall detection are tested only in their supported operating modes.
- [ ] Firestore access is restricted to the authenticated owner as intended.
- [ ] Firestore rules tests pass and live rules are checked separately.
- [ ] Failure cases are tested, including no network, Bluetooth off, location disabled, and permission denial.

## Limitations and Safety

This is a software/hardware project and should **not** be treated as a certified emergency-response or life-safety product.

- Do not rely on it as the sole means of contacting emergency services.
- Calls, SMS, location sharing, BLE delivery, cloud sync, and background operation can fail due to device settings, permissions, connectivity, battery state, or operating-system restrictions.
- GPS modules may not obtain a fix indoors or without adequate satellite visibility.
- Voice activation and fall detection can produce missed events or false alarms and require careful validation.
- Test with consenting participants and non-emergency contacts before any demonstration involving real communication.
- Never use a real emergency number for testing.
- Confirm local emergency procedures and use established emergency services when needed.

## Contributing

Contributions and issue reports are welcome. For changes involving SOS activation, BLE protocols, location, authentication, notifications, or Firestore rules, keep changes narrowly scoped and include testing details.

When opening an issue, include:

- Android version and device model
- Relevant app/firmware version
- Steps to reproduce
- Expected and actual behavior
- Relevant logs with personal information, tokens, and sensitive location data removed

## License

No license is specified here. Unless a license file is added to the repository, reuse and redistribution are not automatically granted. Add a `LICENSE` file if you intend to publish the project under an open-source license.

## Acknowledgements

Built using the Android platform, Kotlin, Jetpack Compose, ESP32 BLE capabilities, and Firebase services where configured.
