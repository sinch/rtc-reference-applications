import Combine
import SinchRTC
import UIKit

final class MainViewController: UIViewController {

  @IBOutlet private var userLabel: UILabel! {
    didSet {
      userLabel.text = """
      Logged in as \"\(viewModel.state.username)\"
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

  @IBOutlet private var separatorView: UIView! {
    didSet {
      separatorView.isHidden = viewModel.state.cli.isEmpty
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

  @IBOutlet private var noteLabel: UILabel! {
    didSet {
      noteLabel.numberOfLines = 10
      noteLabel.adjustsFontSizeToFitWidth = true
      noteLabel.lineBreakMode = .byClipping
      noteLabel.text = viewModel.state.cli.isEmpty ? "" : """
      \u{2022} To make App2App(audio, video) calls just provide recipient name
      \u{2022} To make App2PSTN calls, just provide recipient number
      """
    }
  }

  @IBOutlet private var callButton: UIButton! {
    didSet {
      callButton.setup(with: .call)
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
    NotificationCenter.default.publisher(for: UITextField.textDidChangeNotification,
                                         object: recipientNameTextField)
      .compactMap { ($0.object as? UITextField)?.text }
      .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { [weak self] in self?.viewModel.set(recepientName: $0) }
      .store(in: &cancellableBag)

    NotificationCenter.default.publisher(for: UITextField.textDidChangeNotification,
                                         object: phoneTextField)
      .compactMap { ($0.object as? UITextField)?.text }
      .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
      .removeDuplicates()
      .receive(on: DispatchQueue.main)
      .sink { [weak self] in self?.viewModel.set(recepientPhone: $0) }
      .store(in: &cancellableBag)
  }

  @IBAction private func call(_ sender: Any) {
    let audioCallAction = UIAlertAction(title: "Audio App-to-App", style: .default, handler: { [weak self] _ in
      guard let self = self else { return }
      self.viewModel.call(for: .audio)
    })

    let videoCallAction = UIAlertAction(title: "Video App-to-App", style: .default, handler: { [weak self] _ in
      guard let self = self else { return }
      self.viewModel.call(for: .video)
    })

    let phoneCallAction = !viewModel.state.recepientPhone.isEmpty
      ? UIAlertAction(title: "Phone Call", style: .default, handler: { [weak self] _ in
        guard let self = self else { return }
        self.viewModel.call(for: .phone)
      })
      : nil

    let cancel = UIAlertAction(title: "Cancel", style: .cancel)

    prepareActionSheet(title: "Choose a call type",
                       message: "Connection with \(viewModel.state.recepientName) via:",
                       actions: [audioCallAction,
                                 videoCallAction,
                                 phoneCallAction,
                                 cancel].compactMap { $0 })
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
