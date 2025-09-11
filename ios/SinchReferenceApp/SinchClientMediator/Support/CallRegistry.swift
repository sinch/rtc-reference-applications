import Foundation
import SinchRTC

final class CallRegistry {

  private var lock = NSLock()
  private var activeCalls: [String: SinchCall] = [:]
  private var uuidsByCallIds: [String: UUID] = [:]

  func reset() {
    self.activeCalls.removeAll()
    self.uuidsByCallIds.removeAll()
  }

  // MARK: - SinchCall API

  func addSinchCall(_ call: SinchCall) {
    synchronized(self.lock) {
      self.activeCalls[call.callId] = call
    }
  }

  func removeSinchCall(withId callId: String) {
    synchronized(self.lock) {
      self.activeCalls.removeValue(forKey: callId)
      self.uuidsByCallIds.removeValue(forKey: callId)
    }
  }

  func sinchCall(for callId: String) -> SinchCall? {
    return synchronized(self.lock) {
      return self.activeCalls[callId]
    }
  }

  func sinchCall(from uuid: UUID) -> SinchCall? {
    return synchronized(self.lock) {
      guard let callId = self.uuidsByCallIds.first(where: { $0.value == uuid })?.key else {
        return nil
      }

      return self.activeCalls[callId]
    }
  }

  var activeSinchCalls: [SinchCall] {
    synchronized(self.lock) {
      return Array(self.activeCalls.values)
    }
  }

  // MARK: - UUID <-> Sinch call id mapping API

  // This is necessary to properly bind CallKit/LCK UUID and sinch call id when making calls.
  func map(uuid: UUID, to callId: String) {
    synchronized(self.lock) {
      self.uuidsByCallIds[callId] = uuid
    }
  }

  func uuid(from callId: String) -> UUID? {
    return synchronized(self.lock) {
      return self.uuidsByCallIds[callId]
    }
  }
}
