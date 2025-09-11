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

  var username: String
  var cli: String
  var communicationKit: CommunicationKit
  var recepientName: String = ""
  var recepientPhone: String = ""
  var status: Status
}

final class MainViewModel {

  private var clientMediator: SinchClientMediator

  @Published private(set) var state: MainState

  init(clientMediator: SinchClientMediator) {
    let userInfo = UserInfo.load()
    let communicationKit = CommunicationKit.load()

    self.state = MainState(username: userInfo.userId,
                           cli: userInfo.cli,
                           communicationKit: communicationKit,
                           status: .none)

    self.clientMediator = clientMediator
  }

  func set(recepientName: String) {
    update { $0.recepientName = recepientName }
  }

  func set(recepientPhone: String) {
    update { $0.recepientPhone = recepientPhone }
  }

  func call(for type: CallType) {
    guard !state.recepientName.isEmpty || !state.recepientPhone.isEmpty else {
      update {
        let message = "Recipient name or Phone number should be provided."
        $0.status = .error(message: message)
      }
      return
    }

    clientMediator.call(destination: type == .phone ? state.recepientPhone : state.recepientName,
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
