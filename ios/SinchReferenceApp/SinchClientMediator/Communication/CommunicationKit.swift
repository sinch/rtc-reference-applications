import Foundation

enum CommunicationKit: String {

  private enum Keys {

    static let communicationKit = "com.sinch.communicationKit"
  }

  case callKit
  case liveCommunicationKit
}

extension CommunicationKit {

  func toString() -> String {
    return self == .callKit ? "CallKit" : "LiveCommunicationKit"
  }
}

extension CommunicationKit {

  static func save(communicationKit: CommunicationKit) {
    UserDefaults.standard.set(communicationKit.rawValue, forKey: Keys.communicationKit)
  }

  static func load() -> CommunicationKit {
    let rawValue = UserDefaults.standard.string(forKey: Keys.communicationKit)
    return CommunicationKit(rawValue: rawValue ?? CommunicationKit.callKit.rawValue) ?? .callKit
  }
}
