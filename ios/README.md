# In-app Voice and Video Reference Application for iOS

Reference Applications for the iOS Client.

## Set up

### Update configuration file

To communicate with Sinch API you must include your application specific authorization settings (key and secret). In order to do that, check `ios/SinchReferenceApp/AppEnvironment.swift` file, and add a new case to the `AppEnvironment` enum with the corresponding values copied from the [Sinch Dashboard](https://dashboard.sinch.com/voice/apps) page of your Sinch application.

The file looks as follows:

```swift
enum AppEnvironment: String, CaseIterable, Codable {

  case iosApp = "My app"

  var host: String {
    switch self {
    case .iosApp: return "ocra.api.sinch.com"
    }
  }

  var appKey: String {
    switch self {
    case .iosApp: return "<APPLICATION KEY>"
    }
  }

  var appSecret: String {
    switch self {
    case .iosApp: return "<APPLICATION SECRET>"
    }
  }

  // ...
}
```

Each environment is shown by its `rawValue` (e.g. `"My app"`) in the configuration picker on the login screen.

Parameters description:

- `appKey`: application key copied from your Sinch dashboard page of the application.
- `appSecret`: application secret copied from your Sinch dashboard page of the application.
- `host`: base URL used to make REST calls against Sinch API - if you don't know what this value should be, keep the default value (`ocra.api.sinch.com`)

## Update the SDK to a new version

The SDK is downloaded on demand when the reference app is built.

To pin a new version of the iOS SDK, replace the value of `SDK_VERSION` variable in `./fetch_xcframework_if_needed.sh` script.
