import Foundation
import SinchRTC

final class CallRegistry {
    
    private var lock = NSLock()
    private var activeCalls: [String: SinchCall] = [:]
    private var mapCallIdToCallKitId: [String: UUID] = [:]

    func reset() {
        self.activeCalls.removeAll()
        self.mapCallIdToCallKitId.removeAll()
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
            self.mapCallIdToCallKitId.removeValue(forKey: callId)
        }
    }

    func sinchCall(forCallId callId: String) -> SinchCall? {
        return synchronized(self.lock) {
          return self.activeCalls[callId]
        }
    }

    func sinchCall(forCallKitUUID uuid: UUID) -> SinchCall? {
        return synchronized(self.lock) {
            let tuple = self.mapCallIdToCallKitId.first { tuple -> Bool in
                let (_, value) = tuple
                return uuid == value
            }
            
            guard let (key, _) = tuple else { return nil }
          
            return self.activeCalls[key]
        }
    }

    var activeSinchCalls: [SinchCall] {
        synchronized(self.lock) {
          return Array(self.activeCalls.values)
        }
    }

    // MARK: - CallKit <-> Sinch CallId mapping API
    // This is necessary to properly bind Callkit UUID and sinch callId when making calls.
    func map(callKitId uuid: UUID, toSinchCallId callId: String) {
        synchronized(self.lock) {
            self.mapCallIdToCallKitId[callId] = uuid
        }
    }

    func callKitUUID(forSinchId callId: String) -> UUID? {
        return synchronized(self.lock) {
            return self.mapCallIdToCallKitId[callId]
        }
    }
}
