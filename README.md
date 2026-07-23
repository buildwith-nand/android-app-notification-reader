# Android Notification Reader (SMS text reader)

An Android app that listens to incoming SMS and app notifications (e.g. UPI payment alerts) in real time, extracts the payment amount and reference number using configurable text patterns, and pushes the parsed data to a backend API for storage.

## Overview

Many UPI apps and banks send a notification or SMS like:

```
You have received a UPI Payment with Rs. 500.00
Ref No. 123456789012
```
```
Your a/c XXXX credited by Rs.1,200.00 on 21-07-26
```
```
Money Received - INR 750
```

This app listens for these notifications/SMS as they arrive, matches them against a set of **configurable trigger phrases**, extracts the **amount** and **reference/transaction number**, and forwards the data to a remote API — without requiring manual entry.

A floating widget is also provided so the user can see recent captured transactions at a glance, even while using other apps.

## Features

- 🔔 **Notification Listener Service** — captures notifications from UPI/banking apps (GPay, PhonePe, Paytm, bank apps, etc.) as they arrive
- 💬 **SMS reading support** — parses incoming SMS containing payment credit messages
- 🧩 **Configurable match patterns** — add/edit trigger phrases (e.g. `"received a UPI Payment"`, `"credited by Rs"`, `"Money Received - INR"`) without code changes
- 🔢 **Amount & reference number extraction** — regex-based parsing pulls the transaction amount and reference/UTR number from the message body
- 🌐 **API integration** — posts extracted data to a configurable backend endpoint
- 🪟 **Floating widget** — an always-on-top overlay showing recent captured payments
- 🔁 **Boot persistence** — services restart automatically after device reboot
- ⚙️ **Foreground service** — keeps listening reliably in the background

## How It Works

1. `NotificationListenerService` intercepts incoming notifications; a `BroadcastReceiver` (or content observer) watches incoming SMS.
2. Each message body is checked against the list of configured trigger strings.
3. On a match, regex patterns extract:
   - **Amount** (e.g. `Rs. 500.00`, `INR 750`, `Rs.1,200.00`)
   - **Reference / UTR / transaction number**
4. The parsed data (amount, reference number, source app/sender, timestamp) is sent to the configured API endpoint via an HTTP POST request.
5. The floating widget displays the latest captured transactions for quick confirmation.

## Permissions

| Permission | Purpose |
|---|---|
| `FOREGROUND_SERVICE` | Keep the listener/widget services alive in the background |
| `SYSTEM_ALERT_WINDOW` | Display the floating widget over other apps |
| `POST_NOTIFICATIONS` | Show status notifications (Android 13+) |
| `INTERNET` | Send extracted data to the backend API |
| `ACCESS_NETWORK_STATE` | Check connectivity before making API calls |
| `WAKE_LOCK` | Ensure processing completes even if the device sleeps |
| `RECEIVE_BOOT_COMPLETED` | Restart services after device reboot |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Read/write local config or logs |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required system permission to implement a Notification Listener Service |

> **Note:** `SYSTEM_ALERT_WINDOW` and Notification Listener access must also be granted manually by the user via Android's special app-access settings — these cannot be granted through the normal runtime permission dialog.

## Setup

### Prerequisites
- Android Studio (latest stable)
- Minimum SDK: _add your `minSdkVersion`_
- Target SDK: _add your `targetSdkVersion`_

### Installation
1. Clone the repository
   ```bash
   git clone <repo url>.git
   ```
2. Open the project in Android Studio and let Gradle sync.
3. Update the API endpoint and any keys in the config file (see below).
4. Build and run on a device (a physical device is recommended — emulators handle notification listener access inconsistently).

### Granting required access on-device
After installing the app:
1. **Notification access** — Settings → Apps → Special app access → Notification access → enable for this app.
2. **Display over other apps** — Settings → Apps → Special app access → Display over other apps → enable for this app.
3. **Notifications permission** — accept the runtime prompt (Android 13+).
4. Disable battery optimization for the app so the background service isn't killed.

## Configuration

Trigger phrases and API settings are configurable so you can adapt the app to new payment apps/banks without rebuilding:

```json
{
  "apiEndpoint": "https://your-api.example.com/transactions",
  "triggerPhrases": [
    "received a UPI Payment",
    "credited by Rs",
    "Money Received - INR"
  ],
  "amountRegex": "(?:Rs\\.?|INR)\\s?([\\d,]+(?:\\.\\d{1,2})?)",
  "refNoRegex": "(?:Ref\\.?\\s?No\\.?|UTR|Txn\\s?ID)[:\\s]*([A-Za-z0-9]+)"
}
```

_Adjust the location/format above to match how your app actually loads config (e.g. shared preferences screen, remote config, or a local JSON/XML file)._

## API Payload

Example of the data posted to the backend on a successful match:

```json
{
  "amount": "500.00",
  "referenceNumber": "123456789012",
  "source": "GPay",
  "rawMessage": "You have received a UPI Payment with Rs. 500.00",
  "timestamp": "2026-07-23T10:15:30Z"
}
```

## Tech Stack

- Kotlin / Java (Android)
- `NotificationListenerService`
- `BroadcastReceiver` for SMS
- Foreground Service + Overlay (`WindowManager`) for the floating widget
- HTTP client (e.g. Retrofit/OkHttp) for API calls

## Privacy & Security Notes

- This app reads sensitive financial notifications/SMS — **use it only with data you own and control**.
- Do not commit real API keys, tokens, or endpoints to the repository; use a local `local.properties` / `.env` file excluded via `.gitignore`.
- Consider adding transport-layer security (HTTPS-only) and authentication on the API endpoint.
- Be transparent with end users about what data is captured and where it is sent, and comply with applicable data protection regulations (e.g. Play Store policies on SMS/notification access, which are strictly limited to default SMS/dialer/assistant apps for most use cases).

## Roadmap / Ideas

- [ ] Add support for more banks/UPI apps out of the box
- [ ] UI screen to manage trigger phrases without editing config files
- [ ] Local transaction history with search/filter
- [ ] Retry queue for failed API calls
- [ ] Export logs to CSV

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change.
