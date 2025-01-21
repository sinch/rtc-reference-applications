import OSLog
import SinchRTC
import UIKit

final class LoginViewController: UIViewController {

  @IBOutlet private var loginInfoLabel: UILabel! {
    didSet {
      guard let userName = UserDefaults.standard.string(forKey: SinchClientMediator.userIdKey) else {
        loginInfoLabel.isHidden = true
        return
      }

      #if !targetEnvironment(simulator)
      loginInfoLabel.isHidden = false
      loginInfoLabel.text = """
      You can still receive incoming calls as \"\(userName)\"
      (push profile is still on Sinch servers)
      """
      #endif
    }
  }

  @IBOutlet private var userNameTextField: UITextField! {
    didSet {
      userNameTextField.delegate = self

      userNameTextField.keyboardType = .default
      userNameTextField.textContentType = .username
      userNameTextField.returnKeyType = .done

      userNameTextField.text = UserDefaults.standard.string(forKey: SinchClientMediator.userIdKey) ?? ""
    }
  }

  @IBOutlet private var cliTextField: UITextField! {
    didSet {
      cliTextField.delegate = self

      cliTextField.keyboardType = .phonePad
      cliTextField.textContentType = .telephoneNumber
      cliTextField.returnKeyType = .done

      cliTextField.text = UserDefaults.standard.string(forKey: SinchClientMediator.cliKey) ?? ""
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

  private var hasValidSinchAppConfiguration: Bool {
    APPLICATION_KEY != "<APPLICATION KEY>" && APPLICATION_SECRET != "<APPLICATION SECRET>"
  }

  override func viewDidLoad() {
    super.viewDidLoad()

    SinchClientMediator.instance.logoutDelegate = self

    // Background tap gesture is for numpad keyboard, since it doesn't have 'done' or 'return' key.
    let backgroundTapGesture = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
    view.addGestureRecognizer(backgroundTapGesture)
  }

  @IBAction private func login(_ sender: Any) {
    guard hasValidSinchAppConfiguration else {
      prepareErrorAlert(with: "APPLICATION_KEY and APPLICATION_SECRET must be set to valid values in Config.swift")
      return
    }

    guard let name = userNameTextField.text, !name.isEmpty else {
      userNameTextField.placeholder = "Username couldn't be empty"

      return
    }

    // CLI (Connected Line Identification) is needed for PSTN calls, to provide an ID for the caller.
    // If empty, the call will most likely fail.
    let cli = cliTextField.text ?? ""

    // Sinch client registration for specific user.
    SinchClientMediator.instance.createAndStartClient(with: name, cli: cli) { [weak self] error in
      guard let self = self else { return }

      if error != nil {
        os_log("SinchClient started with error: %{public}@",
               log: .sinchOSLog(for: LoginViewController.identifier),
               type: .error,
               error?.localizedDescription ?? "error")
        // If is fails, presents pop-up with error.
        self.prepareErrorAlert(with: error?.localizedDescription ?? "Not able to login")
      } else {
        os_log("SinchClient started successfully: (version:%{public}@)",
               log: .sinchOSLog(for: LoginViewController.identifier),
               SinchRTC.version())

        self.userNameTextField.resignFirstResponder()

        // If success, transfers to the next screen, to make a call.
        let mainViewController: MainViewController = self.prepareViewController(identifier: "main")

        mainViewController.userName = name
        mainViewController.cli = cli

        self.present(mainViewController, animated: true)
      }
    }
  }

  @objc private func hideKeyboard() {
    userNameTextField.resignFirstResponder()
    cliTextField.resignFirstResponder()
  }
}

extension LoginViewController: UITextFieldDelegate {

  func textFieldShouldReturn(_ textField: UITextField) -> Bool {
    textField.resignFirstResponder()

    return true
  }
}

extension LoginViewController: LogoutDelegate {

  func didLogout() {
    loginInfoLabel.isHidden = true
    loginInfoLabel.text = ""

    userNameTextField.text = ""
    cliTextField.text = ""
  }
}
