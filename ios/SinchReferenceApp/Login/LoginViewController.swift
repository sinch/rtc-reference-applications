import Combine
import UIKit

final class LoginViewController: UIViewController {

  @IBOutlet private var loginInfoLabel: UILabel! {
    didSet {
      loginInfoLabel.text = """
      You can still receive incoming calls as \"\(viewModel.state.username ?? "unknown")\"
      (push profile is still on Sinch servers)
      """
    }
  }

  @IBOutlet private var userNameTextField: UITextField! {
    didSet {
      userNameTextField.delegate = self

      userNameTextField.keyboardType = .default
      userNameTextField.textContentType = .username
      userNameTextField.returnKeyType = .done
    }
  }

  @IBOutlet private var cliTextField: UITextField! {
    didSet {
      cliTextField.delegate = self

      cliTextField.keyboardType = .phonePad
      cliTextField.textContentType = .telephoneNumber
      cliTextField.returnKeyType = .done
    }
  }

  @IBOutlet private var communicationSwitch: UISwitch! {
    didSet {
      if #available(iOS 17.4, *) {
        communicationSwitch.isEnabled = true
      } else {
        communicationSwitch.isEnabled = false
      }
    }
  }

  @IBOutlet private var usingCommunicationLabel: UILabel! {
    didSet {
      usingCommunicationLabel.adjustsFontSizeToFitWidth = true
      usingCommunicationLabel.lineBreakMode = .byClipping
    }
  }

  @IBOutlet private var noteLabel: UILabel! {
    didSet {
      noteLabel.numberOfLines = 10
      noteLabel.adjustsFontSizeToFitWidth = true
      noteLabel.lineBreakMode = .byClipping

      noteLabel.text = """
      \u{2022} For App2App(audio, video) calls just provide user name
      \u{2022} For App2PSTN calls, provide both user name and CLI number
      """
    }
  }

  @IBOutlet private var loginButton: UIButton! {
    didSet {
      loginButton.setup(with: .login)
    }
  }

  @IBOutlet private var loaderView: UIActivityIndicatorView!

  @IBOutlet private var backgroundView: UIView! {
    didSet {
      let backgroundTapGesture = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
      backgroundView.addGestureRecognizer(backgroundTapGesture)
    }
  }

  var viewModel: LoginViewModel!

  weak var navigator: NavigationDelegate?

  private var cancellableBag = Set<AnyCancellable>()

  override func viewDidLoad() {
    super.viewDidLoad()

    assignTextUpdate()
    assignCommunicationKit()
    assignPresentation()

    viewModel.$state.map { $0.status != .loading }
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .assign(to: \.isHidden, on: loaderView)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.status)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { [weak self] status in
        guard let self = self else { return }

        switch status {
          case .error(let message):
            self.prepareErrorAlert(with: message)
          case .login:
            self.loaderView.stopAnimating()
            self.viewModel.reset()
            self.navigator?.navigate(to: .main)
          default:
            break
        }
      }
      .store(in: &cancellableBag)

    viewModel.$state.map(\.status)
      .removeDuplicates()
      .sink { [weak self] status in
        guard let self = self else { return }

        switch status {
          case .error(let message):
            self.userNameTextField.placeholder = message
          default:
            self.userNameTextField.placeholder = "User name"
        }
      }
      .store(in: &cancellableBag)
  }

  private func assignTextUpdate() {
    NotificationCenter.default.publisher(for: UITextField.textDidChangeNotification,
                                         object: userNameTextField)
      .compactMap { ($0.object as? UITextField)?.text }
      .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { [weak self] in self?.viewModel.set(username: $0) }
      .store(in: &cancellableBag)

    NotificationCenter.default.publisher(for: UITextField.textDidChangeNotification,
                                         object: cliTextField)
      .compactMap { ($0.object as? UITextField)?.text }
      .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { [weak self] in self?.viewModel.set(cli: $0) }
      .store(in: &cancellableBag)
  }

  private func assignCommunicationKit() {
    viewModel.$state.map { $0.communicationKit == .liveCommunicationKit }
      .removeDuplicates()
      .assign(to: \.isOn, on: communicationSwitch)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.communicationKit)
      .removeDuplicates()
      .sink { [weak self] communicationKit in
        guard let self = self else { return }

        if #available(iOS 17.4, *) {
          self.usingCommunicationLabel.text = "Using \(communicationKit.toString())"
        } else {
          self.usingCommunicationLabel.text = "Using CallKit"
        }
      }
      .store(in: &cancellableBag)
  }

  private func assignPresentation() {
    viewModel.$state.map(\.username)
      .removeDuplicates()
      .assign(to: \.text, on: userNameTextField)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.cli)
      .removeDuplicates()
      .assign(to: \.text, on: cliTextField)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.loginInfoHidden)
      .removeDuplicates()
      .assign(to: \.isHidden, on: loginInfoLabel)
      .store(in: &cancellableBag)
  }

  @IBAction private func communicationSwitchChanged(_ sender: UISwitch) {
    viewModel.set(communicationKit: sender.isOn ? .liveCommunicationKit : .callKit)
  }

  @IBAction private func login(_ sender: Any) {
    viewModel.login()
  }

  @objc private func hideKeyboard() {
    view.endEditing(true)
  }
}

extension LoginViewController: UITextFieldDelegate {

  func textFieldShouldReturn(_ textField: UITextField) -> Bool {
    textField.resignFirstResponder()
    return true
  }
}
