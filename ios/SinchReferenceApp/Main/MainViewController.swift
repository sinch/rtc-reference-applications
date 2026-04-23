import Combine
import SinchRTC
import UIKit

final class MainViewController: UIViewController {

  @IBOutlet private var userLabel: UILabel! {
    didSet {
      userLabel.text = """
      Logged in as \"\(viewModel.state.username)\"
      Environment: \(viewModel.state.appEnvironment)
      Using \(viewModel.state.communicationKit.toString())
      """
    }
  }

  @IBOutlet private var recipientNameTextField: UITextField! {
    didSet {
      recipientNameTextField.delegate = self

      recipientNameTextField.keyboardType = .default
      recipientNameTextField.returnKeyType = .done
      recipientNameTextField.textContentType = .username
    }
  }

  @IBOutlet private var sipIdentityTextField: UITextField! {
    didSet {
      sipIdentityTextField.delegate = self

      sipIdentityTextField.keyboardType = .default
      sipIdentityTextField.returnKeyType = .done
      sipIdentityTextField.textContentType = .username
    }
  }

  @IBOutlet private var conferenceTextField: UITextField! {
    didSet {
      conferenceTextField.delegate = self

      conferenceTextField.keyboardType = .default
      conferenceTextField.returnKeyType = .done
      conferenceTextField.textContentType = .username
    }
  }

  @IBOutlet private var phoneLabel: UILabel! {
    didSet {
      phoneLabel.isHidden = viewModel.state.cli.isEmpty
    }
  }

  @IBOutlet private var phoneTextField: UITextField! {
    didSet {
      phoneTextField.delegate = self

      phoneTextField.keyboardType = .phonePad
      phoneTextField.textContentType = .telephoneNumber

      phoneTextField.isHidden = viewModel.state.cli.isEmpty
    }
  }

  @IBOutlet private var callButton: UIButton! {
    didSet {
      callButton.setup(with: .call)
    }
  }

  @IBOutlet private var versionLabel: UILabel! {
    didSet {
      versionLabel.text = viewModel.sdkVersion
    }
  }

  @IBOutlet private var backgroundView: UIView! {
    didSet {
      let backgroundTapGesture = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
      backgroundView.addGestureRecognizer(backgroundTapGesture)
    }
  }

  var viewModel: MainViewModel!

  weak var navigator: NavigationDelegate?

  private var cancellableBag = Set<AnyCancellable>()

  override func viewDidLoad() {
    super.viewDidLoad()

    assignTextUpdate()

    viewModel.$state.map(\.status)
      .removeDuplicates()
      .filter { $0 != .none }
      .receive(on: DispatchQueue.main)
      .sink { [weak self] status in
        guard let self = self else { return }

        switch status {
          case .error(let message):
            self.prepareErrorAlert(with: message)
          case .call(let call, let type):
            let destination: Navigation = type == .video ? .videoCall(call) : .audioCall(call, type)
            self.navigator?.navigate(to: destination)
          case .logout:
            self.view.endEditing(true)
            self.dismiss(animated: true)
          case .none:
            break
        }
      }
      .store(in: &cancellableBag)
  }

  private func assignTextUpdate() {
    configureTextDidChange(for: recipientNameTextField) { [weak self] text in
      self?.viewModel.set(.recepientName(text))
    }

    configureTextDidChange(for: sipIdentityTextField) { [weak self] text in
      self?.viewModel.set(.sipIdentity(text))
    }

    configureTextDidChange(for: conferenceTextField) { [weak self] text in
      self?.viewModel.set(.conferenceId(text))
    }

    configureTextDidChange(for: phoneTextField) { [weak self] text in
      self?.viewModel.set(.recepientPhone(text))
    }
  }

  private func configureTextDidChange(for textField: UITextField, handler: @escaping ((String) -> Void)) {
    NotificationCenter.default.publisher(for: UITextField.textDidChangeNotification,
                                         object: textField)
      .compactMap { ($0.object as? UITextField)?.text }
      .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { handler($0) }
      .store(in: &cancellableBag)
  }

  @IBAction private func call(_ sender: Any) {
    let audioCall: CallType? = !viewModel.state.recepientName.isEmpty ? .audio : nil
    let videoCall: CallType? = !viewModel.state.recepientName.isEmpty ? .video : nil
    let phoneCall: CallType? = !viewModel.state.recepientPhone.isEmpty ? .phone : nil
    let sipCall: CallType? = !viewModel.state.sipIdentity.isEmpty ? .sip : nil
    let conferenceCall: CallType? = !viewModel.state.conferenceId.isEmpty ? .conference : nil

    var actions: [UIAlertAction] = [audioCall,
                                    videoCall,
                                    sipCall,
                                    conferenceCall,
                                    phoneCall]
      .compactMap { $0 }
      .map { type in
        let title: String

        switch type {
          case .audio: title = "Audio App-to-App"
          case .video: title = "Video App-to-App"
          case .sip: title = "SIP Call"
          case .conference: title = "Conference Call"
          case .phone: title = "Phone Call"
        }

        return UIAlertAction(title: title, style: .default) { [weak self] _ in
          guard let self = self else { return }

          self.viewModel.call(for: type)
        }
      }

    let cancel = UIAlertAction(title: "Cancel", style: .cancel)
    actions.append(cancel)

    prepareActionSheet(title: "Choose a call type",
                       message: "Create a new call with:",
                       actions: actions.compactMap { $0 })
  }

  @IBAction private func logout(_ sender: Any) {
    viewModel.logout()
  }

  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    viewModel.logout()
  }

  @objc private func hideKeyboard() {
    view.endEditing(true)
  }
}

extension MainViewController: UITextFieldDelegate {

  func textFieldShouldReturn(_ textField: UITextField) -> Bool {
    textField.resignFirstResponder()

    return true
  }
}
