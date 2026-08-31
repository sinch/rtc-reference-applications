import Foundation
import UIKit

enum AppEnvironment: String, CaseIterable, Codable {

  case empty

  var host: String { return "" }

  var appKey: String { return "" }

  var appSecret: String { return "" }

  var defaultUserName: String {
    return UIDevice.current.name.replacingOccurrences(of: " ", with: "")
  }

  var defaultCli: String { return "" }
}
