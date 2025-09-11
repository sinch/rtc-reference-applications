import OSLog
import SinchRTC

extension OSLog {

  // Predefined log for Sinch, where category is name of the class from where logger was called.
  static func sinchOSLog(for category: String) -> OSLog {
    return OSLog(subsystem: "com.sinch.sdk.app", category: category)
  }
}

// Sinch Logging extending with Sinch Log Type.
extension LogSeverity {

  var osLogType: OSLogType {
    switch self {
      case .info, .warning: return .default
      case .critical: return .fault
      case .trace: return .debug
      default: return .default
    }
  }
}
