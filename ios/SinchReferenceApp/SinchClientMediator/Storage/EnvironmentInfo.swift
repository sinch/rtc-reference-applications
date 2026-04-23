import Foundation

enum EnvironmentInfo {

  private enum Keys {

    static let environment = "com.sinch.environment"
  }

  static func save(_ environment: AppEnvironment) {
    UserDefaults.standard.set(environment.rawValue, forKey: Keys.environment)
  }

  static func load() -> AppEnvironment {
    let rawValue = UserDefaults.standard.string(forKey: Keys.environment)

    guard let value = rawValue, let environment = AppEnvironment(rawValue: value) else {
      return .empty
    }

    return environment
  }

  static func clear() {
    UserDefaults.standard.removeObject(forKey: Keys.environment)
  }
}
