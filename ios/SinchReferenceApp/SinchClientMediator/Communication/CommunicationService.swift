import Foundation

protocol CommunicationService: AnyObject {

  func call(userId: String, uuid: UUID, isVideo: Bool, with completion: @escaping (Error?) -> Void)

  func reportOutgoingCall(uuid: UUID, time: Date?, for state: OutgoingCallState)
  func reportIncomingCall(localUserId: String,
                          remoteUserId: String,
                          uuid: UUID,
                          isVideoOffered: Bool,
                          with completion: @escaping (Error?) -> Void)

  func end(uuid: UUID, with completion: @escaping (Error?) -> Void)

  func reportCallEnd(uuid: UUID, time: Date?, endCause: EndCause)

  func declineIncomingCallIfBusy(uuid: UUID)
}
