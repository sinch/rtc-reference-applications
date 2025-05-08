# In-app Voice and Video Reference Application for JavaScript

## Usage

Reference app of the Javascript SDK from Sinch.

Use latest version of Chrome to test the app.

Start from root url and enter the application key and secret and select the type of call to be made.

In order to obtain your credentials you need to either creating and account or loging in to [dashboard.sinch.com](https://dashboard.sinch.com/).
And after that navigate to [Voice & Video / Apps](https://dashboard.sinch.com/voice/apps) and create a new App.

Read specific instructions for each call sample by clicking the icon on the left hand side of the call type header. i.e. 'In app video calling demo'

### Running locally

Run `npm install` and then `npm start` to host the app locally.

## Available Demos

- voicecall - in-app to in-app voice (data to data voice)
- videocall - in-app to in-app voice and video (data to data voice)
- callnumber - in-app to telephone (data voice to PSTN)
- callsip - in-app to sip (data to SIP)

## Documentation

## OS and browser support

The sample application (and the Sinch SDK) works on all major browsers currently available. However, the SDK relies on the W3C Push API to send push notifications, so the browser must support this API. For a detailed list of minimum version requirements for each browser, see: [link](https://developer.mozilla.org/en-US/docs/Web/API/Push_API).

### iOS restrictions

To receive push messages on iOS, keep the following in mind:

- Your application must run on Safari 16.4+. Other browsers on iOS run within a WebView, which does not support the Push API.
- The application must run as a standalone app. This means you need to set the `"display": "standalone"` property in your app’s `manifest.json` file. Additionally, the user must add the app to the home screen and launch it from there. See how this is handled within our reference app. Search for `manifest.json` file and `isIOSBrowserNotInStandaloneMode()` check.
- Browsers usually allow HTTP traffic for local development. This facilitates testing JavaScript applications locally, on desktop browsers. However, when testing on a mobile device connected via USB, localhost refers to the device itself and not the machine serving the web application. In this case, HTTPS is required to ensure that the mobile device and the browser can access the application hosted on desktop.

For more information regarding Push API on iOS see [link](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers).

### Terminated app on iOS Safari

See [link](/javascript/samples/voicecall/sw.js#L63) for more information regarding the current limitation of the reference app in terms of receiving push notifications when terminated.

### Permission prompt from user gesture

In browsers other than Chrome, it is typically required that the first interaction with the Push API, which triggers a permission request, must be initiated by a user action (e.g., a user click). This interaction usually happens automatically when the SinchClient starts. As a result, in some cases, we cannot start the SinchClient automatically when the page loads. Instead, we must delay the process until the user performs some interaction. For a more detailed explanation and a working example of how to handle this issue, see how the `canAutoStart()` method is used in the reference app.

Read full documentation [here](https://developers.sinch.com/docs/in-app-calling/js-cloud/).
