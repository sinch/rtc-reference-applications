import Combine
import OSLog
import SinchRTC

struct LoginState: Equatable {

  enum Status {

    case login
    case loading
    case error(message: String)
    case none
  }

  var username: String?
  var cli: String?
  var communicationKit: CommunicationKit = .callKit
  var loginInfoHidden: Bool = true
  var status: Status = .none
}

final class LoginViewModel {

  private var clientMediator: SinchClientMediator

  @Published private(set) var state: LoginState

  private var hasValidSinchAppConfiguration: Bool {
    APPLICATION_KEY != "<APPLICATION KEY>" && APPLICATION_SECRET != "<APPLICATION SECRET>"
  }

  init(clientMediator: SinchClientMediator) {
    let userInfo = UserInfo.load()
    let communicationKit = CommunicationKit.load()

    self.state = LoginState(username: userInfo.userId,
                            cli: userInfo.cli,
                            communicationKit: communicationKit,
                            loginInfoHidden: userInfo.userId.isEmpty)

    self.clientMediator = clientMediator
    self.clientMediator.logoutDelegate = self
  }

  func set(username: String) {
    update { $0.username = username }
  }

  func set(cli: String) {
    update { $0.cli = cli }
  }

  func set(communicationKit: CommunicationKit) {
    update { $0.communicationKit = communicationKit }
  }

  func login() {
    update { $0.status = .loading }

    guard hasValidSinchAppConfiguration else {
      update {
        let message = "Please set valid Sinch application key and secret in the project settings."
        $0.status = .error(message: message)
      }
      return
    }

    guard let username = state.username, !username.isEmpty else {
      update {
        let message = "Username cannot be empty."
        $0.status = .error(message: message)
      }
      return
    }

    clientMediator.setupCommunication(with: state.communicationKit)

    clientMediator.createAndStartClient(with: username, cli: state.cli ?? "") { [weak self] error in
      guard let self = self else { return }

      guard let error = error else {
        os_log("SinchClient started successfully: (version:%{public}@)",
               log: .sinchOSLog(for: LoginViewController.identifier),
               SinchRTC.version())

        self.update { $0.status = .login }

        return
      }

      os_log("SinchClient started with error: %{public}@",
             log: .sinchOSLog(for: LoginViewController.identifier),
             type: .error,
             error.localizedDescription)

      update {
        let message = error.localizedDescription.isEmpty ? "Not able to login" : error.localizedDescription
        $0.status = .error(message: message)
      }
    }
  }

  func reset() {
    update { $0.status = .none }
  }

  private func update(state transform: (inout LoginState) -> Void) {
    var viewState = self.state
    transform(&viewState)
    self.state = viewState
  }
}

extension LoginViewModel: LogoutDelegate {

  func didLogout() {
    update { state in
      state.username = ""
      state.cli = ""
      state.communicationKit = .callKit
      state.loginInfoHidden = true
      state.status = .none
    }
  }
}

extension LoginState.Status: Equatable {

  static func == (lhs: LoginState.Status, rhs: LoginState.Status) -> Bool {
    switch (lhs, rhs) {
      case (.none, .none), (.loading, .loading), (.login, .login):
        return true
      case (.error(let lhsMessage), .error(let rhsMessage)):
        return lhsMessage == rhsMessage
      default:
        return false
    }
  }
}
