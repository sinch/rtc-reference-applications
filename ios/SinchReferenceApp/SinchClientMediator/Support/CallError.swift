import Foundation

enum CallError: Error {

  case noClient
  case noCallType(String)
  case noRecepientIdentifier
}

extension CallError: LocalizedError {

  var errorDescription: String? {
    switch self {
      case .noClient: return "SinchClient was not assigned."
      case .noCallType(let callId): return "Call type for call id: \(callId) was not set."
      case .noRecepientIdentifier: return "Recipient identifier was not set for the call."
    }
  }
}
