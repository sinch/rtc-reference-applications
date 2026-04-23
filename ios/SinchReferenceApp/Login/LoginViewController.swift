import Combine
import UIKit

final class LoginViewController: UIViewController {

  private enum Constant {

    static let cornerRadius: CGFloat = 6
    static let borderWidth: CGFloat = 1

    static let pickerShadowOpacity: Float = 0.25
    static let pickerShadowRadius: CGFloat = 4
    static let pickerShadowOffsetWidth: CGFloat = 0
    static let pickerShadowOffsetHeight: CGFloat = 2
  }

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
      userNameTextField.text = viewModel.state.username

      userNameTextField.keyboardType = .default
      userNameTextField.textContentType = .username
      userNameTextField.returnKeyType = .done
    }
  }

  @IBOutlet private var cliTextField: UITextField! {
    didSet {
      cliTextField.delegate = self
      cliTextField.text = viewModel.state.cli

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
      \u{2022} For App-to-App(audio, video) calls, App-to-SIP and App-to-Conference calls just provide user name
      \u{2022} For App-to-PSTN calls, provide both user name and CLI number
      """
    }
  }

  @IBOutlet private var configurationButton: UIButton! {
    didSet {
      configurationButton.layer.cornerRadius = Constant.cornerRadius
    }
  }

  @IBOutlet private var configurationPicker: UIPickerView! {
    didSet {
      configurationPicker.dataSource = self
      configurationPicker.delegate = self
      configurationPicker.isHidden = true
      configurationPicker.layer.borderColor = UIColor.black.cgColor
      configurationPicker.layer.borderWidth = Constant.borderWidth
      configurationPicker.layer.cornerRadius = Constant.cornerRadius
      configurationPicker.layer.shadowColor = UIColor.black.cgColor
      configurationPicker.layer.shadowOpacity = Constant.pickerShadowOpacity
      configurationPicker.layer.shadowRadius = Constant.pickerShadowRadius
      configurationPicker.layer.shadowOffset = CGSize(width: Constant.pickerShadowOffsetWidth,
                                                      height: Constant.pickerShadowOffsetHeight)
      configurationPicker.clipsToBounds = false
    }
  }

  @IBOutlet private var loginButton: UIButton! {
    didSet {
      loginButton.setup(with: .login)
    }
  }

  @IBOutlet private var versionLabel: UILabel! {
    didSet {
      versionLabel.text = viewModel.sdkVersion
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

    if let index = AppEnvironment.allCases.firstIndex(of: viewModel.state.appEnvironment) {
      configurationPicker.selectRow(index, inComponent: 0, animated: false)
    }

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
      .receive(on: DispatchQueue.main)
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
      .receive(on: DispatchQueue.main)
      .assign(to: \.isOn, on: communicationSwitch)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.communicationKit)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
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
      .receive(on: DispatchQueue.main)
      .assign(to: \.text, on: userNameTextField)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.cli)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .assign(to: \.text, on: cliTextField)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.loginInfoHidden)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .assign(to: \.isHidden, on: loginInfoLabel)
      .store(in: &cancellableBag)
  }

  @IBAction private func communicationSwitchChanged(_ sender: UISwitch) {
    viewModel.set(communicationKit: sender.isOn ? .liveCommunicationKit : .callKit)
  }

  @IBAction private func login(_ sender: Any) {
    viewModel.login()
  }

  @IBAction private func chooseConfiguration(_ sender: Any) {
    configurationPicker.isHidden = !configurationPicker.isHidden
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

extension LoginViewController: UIPickerViewDataSource, UIPickerViewDelegate {

  func numberOfComponents(in pickerView: UIPickerView) -> Int {
    1
  }

  func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
    AppEnvironment.allCases.count
  }

  func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
    AppEnvironment.allCases[row].rawValue
  }

  func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
    let environment = AppEnvironment.allCases[row]

    viewModel.set(environment: environment)
    configurationPicker.isHidden = true
  }
}
