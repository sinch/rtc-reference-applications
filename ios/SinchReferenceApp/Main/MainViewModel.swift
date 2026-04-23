import Combine
import OSLog
import SinchRTC

struct MainState: Equatable {

  enum Status {

    case call(SinchCall, type: CallType)
    case error(message: String)
    case logout
    case none
  }

  enum SetAction {

    case recepientName(String)
    case recepientPhone(String)
    case sipIdentity(String)
    case conferenceId(String)
  }

  var username: String
  var cli: String
  var communicationKit: CommunicationKit
  var appEnvironment: String
  var recepientName: String = ""
  var recepientPhone: String = ""
  var sipIdentity: String = ""
  var conferenceId: String = ""
  var status: Status
}

final class MainViewModel {

  private var clientMediator: SinchClientMediator

  @Published private(set) var state: MainState

  var sdkVersion: String {
    return clientMediator.sdkVersion
  }

  init(clientMediator: SinchClientMediator) {
    let userInfo = UserInfo.load()
    let communicationKit = CommunicationKit.load()
    let environment = EnvironmentInfo.load()

    self.state = MainState(username: userInfo.userId,
                           cli: userInfo.cli,
                           communicationKit: communicationKit,
                           appEnvironment: environment.rawValue,
                           status: .none)

    self.clientMediator = clientMediator
  }

  func set(_ action: MainState.SetAction) {
    update {
      switch action {
        case .recepientName(let name):
          $0.recepientName = name
        case .recepientPhone(let phone):
          $0.recepientPhone = phone
        case .sipIdentity(let identity):
          $0.sipIdentity = identity
        case .conferenceId(let id):
          $0.conferenceId = id
      }
    }
  }

  func call(for type: CallType) {
    guard !state.recepientName.isEmpty || !state.recepientPhone.isEmpty
      || !state.sipIdentity.isEmpty || !state.conferenceId.isEmpty else {
      update {
        let message = "Recipient name, SIP identity, Conference ID or Phone number should be provided."
        $0.status = .error(message: message)
      }
      return
    }

    let destination: String

    switch type {
      case .audio, .video:
        destination = state.recepientName
      case .phone:
        destination = state.recepientPhone
      case .sip:
        destination = state.sipIdentity
      case .conference:
        destination = state.conferenceId
    }

    clientMediator.call(destination: destination,
                        type: type) { [weak self] (result: Result<SinchCall, Error>) in
      guard let self = self else { return }

      switch result {
        case .success(let call):
          update { $0.status = .call(call, type: type) }
        case .failure(let error):
          os_log("Call failed: %{public}@, type: %{public}@",
                 log: .sinchOSLog(for: MainViewController.identifier),
                 type: .error,
                 error.localizedDescription,
                 type.rawValue)

          update {
            let message = "Call failed: \(error.localizedDescription)"
            $0.status = .error(message: message)
          }
      }
    }
  }

  func logout() {
    clientMediator.logout { [weak self] in
      guard let self = self else { return }

      self.update { $0.status = .logout }
    }
  }

  private func update(state transform: (inout MainState) -> Void) {
    var viewState = self.state
    transform(&viewState)
    self.state = viewState
  }
}

extension MainState.Status: Equatable {

  static func == (lhs: MainState.Status, rhs: MainState.Status) -> Bool {
    switch (lhs, rhs) {
      case (.none, .none), (.logout, .logout):
        return true
      case (.call(let lhsCall, let lhsType), .call(let rhsCall, let rhsType)):
        return lhsCall.callId == rhsCall.callId && lhsType == rhsType
      case (.error(let lhsMessage), .error(let rhsMessage)):
        return lhsMessage == rhsMessage
      default:
        return false
    }
  }
}
