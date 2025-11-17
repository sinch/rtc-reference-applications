# In-app Voice and Video Reference Application for iOS

Reference Applications for the iOS Client.

## Set up

### Update configuration file

To communicate with Sinch API you must include your application specific authorization settings (key and secret). In order to do that, check `ios/SinchReferenceApp/Config.swift` file, and fill it with the corresponding values copied from the [Sinch Dashboard](https://dashboard.sinch.com/voice/apps) page of your Sinch application.

The file looks as follows:

```swift
let APPLICATION_KEY = "<APPLICATION KEY>"
let APPLICATION_SECRET = "<APPLICATION SECRET>"
let ENVIRONMENT_HOST = "ocra.api.sinch.com"
```

Parameters description:

- `APPLICATION_KEY`: application key copied from your Sinch dashboard page of the application.
- `APPLICATION_SECRET`: application secret copied from your Sinch dashboard page of the application.
- `ENVIRONMENT_HOST`: base URL used to make REST calls against Sinch API - if you don't know what this value should be, keep the default value (`ocra.api.sinch.com`)

## Update the SDK to a new version

The SDK is downloaded on demand when the reference app is built.

To pin a new version of the iOS SDK, replace the value of `SDK_VERSION` variable in `./fetch_xcframework_if_needed.sh` script.
