# SERVICE.app

Android call handoff for restaurant service teams. Automatically acknowledge answering-service SMS messages and forward each complete message to the appropriate on-call technician.

## Routing

- Answering service: **(833) 355-0861** by default. In Settings, change it to a test sender or restore the default. Changing or restoring the source pauses routing. Reply codes go back to the configured sender.
- **BRINK:** a header line beginning with **SLIM CHICKENS**, ignoring case and extra spaces; store suffixes are allowed.
- **ALOHA:** all other restaurant calls.
- Each department has an independently editable on-call number.
- The number on the standalone **Reply with** line is sent back to the answering service. The complete original message is then submitted to the selected department.

The subtitle refers to restaurant and grocery support; automated grocery routing is not implemented because those calls already go directly to that department.

## Install

Download the APK from this repository's Releases page and open it on an Android 8.0+ phone. Allow installation from your browser or Files app if prompted. This is an internal-distribution app, not a Play Store publication.

**Switching from the early debug build:** the release APK uses a dedicated signing key. Android cannot install it over the debug-signed test app. Record your department numbers, uninstall the test app, then install the release APK. Uninstalling clears the app's local history and settings. Later releases using the release key can update this release normally.

## Setup

1. Open SERVICE.app on the phone that receives answering-service SMS messages.
2. In Settings, grant SMS and notification permissions.
3. Enter a BRINK and/or ALOHA on-call number. At least one department must be configured to enable routing. The answering-service number can be changed for testing; restore the default after testing. US ten-digit numbers and +1 formatting are supported.
4. Use **Open test preview** to inspect routing and acknowledgment without sending texts.
5. Enable routing from Overview. Update each department number when coverage changes.
6. Verify acknowledgment and forwarding using controlled messages from the configured sender before relying on automated handoffs.

Only ordinary SMS is supported. RCS/chat messages and MMS are not processed. On dual-SIM phones, configure a default SMS SIM. Android/installer permission restrictions and carrier behavior require testing on the actual phone.

## Build and test

Requires JDK 17 and Android SDK 35. The Gradle wrapper downloads Gradle 8.11.1.

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

macOS/Linux:

```sh
bash gradlew testDebugUnitTest lintDebug assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release, set `SERVICE_SIGNING_STORE` to an absolute keystore path and `SERVICE_SIGNING_PASSWORD` to its password. The key alias must be `service-release`. Then run `gradlew.bat testDebugUnitTest lintRelease assembleRelease` (or `bash gradlew ...`). Never commit signing keys or passwords. Existing release updates require the original release key.

## Operation and limitations

- Routing starts paused; upgrading from the single-destination prototype also pauses routing until at least one department is configured.
- A single unambiguous `Reply with` line with 1–12 digits is required. Leading zeroes are preserved. Missing/ambiguous codes or invalid selected destinations create an attention entry without sending texts.
- Acknowledgment is submitted first, forwarding second. These operations are independent, not atomic, and arrival order is not guaranteed. Forwarding is still attempted if acknowledgment submission fails.
- History records destination, department, and separate send results. **Sent** means Android reported sending; it does not prove recipient delivery or reading. Pending results require manual checking. There is no remote escalation or technician acknowledgment workflow.
- Long SMS messages are assembled and forwarded in multiple parts. Duplicate detection uses sender, SMS timestamp, and body within the latest 100 stored records; a retransmission with a different timestamp is a new call. Failed or pending texts are not automatically resent.
- The phone must stay powered on with cellular service. Open the app after installation; force-stopping it prevents reception until reopened. A process/device interruption during handoff can leave an incomplete record needing manual attention.

## Data

No cloud service, account, advertising, analytics, or Internet permission. Department numbers and the latest 100 call records are stored in app-private local storage. Android cloud backup is disabled. There is no export/delete-history UI. SMS sending uses the phone's cellular plan.

## Android references

- [SMS permissions](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS)
- [SmsManager](https://developer.android.com/reference/android/telephony/SmsManager)
- [Google Play SMS permission policy](https://support.google.com/googleplay/android-developer/answer/10208820)

Calls for an unconfigured department are recorded and flagged for attention. They are neither acknowledged nor forwarded, and never fall back to the other department.
