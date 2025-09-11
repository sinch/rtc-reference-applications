import Foundation

struct UserInfo {

  private enum Keys {

    static let userId = "com.sinch.userId"
    static let cli = "com.sinch.cli"
  }

  let userId: String
  let cli: String
}

extension UserInfo {

  static func save(userId: String, cli: String) {
    UserDefaults.standard.setValue(userId, forKey: Keys.userId)
    UserDefaults.standard.setValue(cli, forKey: Keys.cli)
  }

  static func load(with userId: String = "", and cli: String = "") -> UserInfo {
    let userName = UserDefaults.standard.string(forKey: Keys.userId) ?? userId
    let cli = UserDefaults.standard.string(forKey: Keys.cli) ?? cli

    return UserInfo(userId: userName, cli: cli)
  }

  static func clear() {
    UserDefaults.standard.removeObject(forKey: Keys.userId)
    UserDefaults.standard.removeObject(forKey: Keys.cli)
  }
}
