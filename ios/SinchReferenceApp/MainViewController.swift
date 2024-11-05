import OSLog
import SinchRTC
import UIKit

final class MainViewController: UIViewController {

  @IBOutlet private var userLabel: UILabel! {
    didSet {
      guard let userName = userName else {
        userLabel.text = "Login failed for user"

        return
      }

      userLabel.text = "Logged in as \(userName)"
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
      separatorView.isHidden = cli.isEmpty
    }
  }

  @IBOutlet private var phoneLabel: UILabel! {
    didSet {
      phoneLabel.isHidden = cli.isEmpty
    }
  }

  @IBOutlet private var phoneTextField: UITextField! {
    didSet {
      phoneTextField.delegate = self

      phoneTextField.keyboardType = .phonePad
      phoneTextField.textContentType = .telephoneNumber

      phoneTextField.isHidden = cli.isEmpty
    }
  }

  @IBOutlet private var noteLabel: UILabel! {
    didSet {
      noteLabel.numberOfLines = 10
      noteLabel.adjustsFontSizeToFitWidth = true
      noteLabel.lineBreakMode = .byClipping
      noteLabel.text = cli.isEmpty ? "" : """
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

  var userName: String?

  var cli: String = ""

  override func viewDidLoad() {
    super.viewDidLoad()

    // Background tap gesture is for numpad keyboard, since it doesn't have 'done' or 'return' key.
    let backgroundTapGesture = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
    view.addGestureRecognizer(backgroundTapGesture)
  }

  @IBAction private func call(_ sender: Any) {
    let recipient = recipientNameTextField.text ?? ""
    let phoneNumber = phoneTextField.text ?? ""

    guard !recipient.isEmpty || !phoneNumber.isEmpty else {
      prepareErrorAlert(with: "Recipient name or Phone number should be provided.")

      return
    }

    let audioCallAction = !recipient.isEmpty
    ? UIAlertAction(title: "Audio App-to-App", style: .default, handler: { [weak self] _ in
      guard let self = self else { return }

      self.prepareAudioCallViewController(to: recipient, callType: .audio)
    })
    : nil

    let videoCallAction = !recipient.isEmpty
    ? UIAlertAction(title: "Video App-to-App", style: .default, handler: { [weak self] _ in
      guard let self = self else { return }

      self.prepareVideoCallViewController(to: recipient)
    })
    : nil

    let pstnCallAction = !phoneNumber.isEmpty
    ? UIAlertAction(title: "Phone Call", style: .default, handler: { [weak self] _ in
      guard let self = self else { return }

      self.prepareAudioCallViewController(to: phoneNumber, callType: .phone)
    })
    : nil

    let cancel = UIAlertAction(title: "Cancel", style: .cancel)

    prepareActionSheet(title: "Choose a call type",
                       message: "Connection with \(recipient) via:",
                       actions: [audioCallAction,
                                 videoCallAction,
                                 pstnCallAction,
                                 cancel].compactMap { $0 })
  }

  // Connect logout action to button.
  @IBAction private func logout(_ sender: Any) {
    logout()
  }

  // Logout user on MainViewController dismiss.
  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    logout()
  }

  private func logout() {
    SinchClientMediator.instance.logout { [weak self] in
      guard let self = self else { return }

      self.recipientNameTextField.resignFirstResponder()
      self.dismiss(animated: true)
    }
  }

  // Start a call, with provided recipient name, both users shoud be logged in and registered.
  private func prepareAudioCallViewController(to recipient: String, callType: SinchClientMediator.CallType) {
    SinchClientMediator.instance.call(destination: recipient,
                                      type: callType) { [weak self] (result: Result<SinchCall, Error>) in
      guard let self = self else { return }

      switch result {
        // On success transfers to call view controller.
      case .success(let call):
        let audioCallViewController: AudioCallViewController = self.prepareViewController(identifier: "call")
        audioCallViewController.callCompletionDelegate = self

        // Pass call to be able to finish it.
        audioCallViewController.call = call

        self.recipientNameTextField.resignFirstResponder()
        self.present(audioCallViewController, animated: true)
      case .failure(let error):
        os_log("Audio Call failed failed: %{public}@",
               log: .sinchOSLog(for: MainViewController.identifier),
               type: .error,
               error.localizedDescription)
        self.prepareErrorAlert(with: error.localizedDescription)
      }
    }
  }

  // Start a video call, with provided recipient name, both users shoud be logged in and registered.
  private func prepareVideoCallViewController(to recipient: String) {
    SinchClientMediator.instance
      .call(destination: recipient,
            type: .video,
            with: { [weak self] (result: Result<SinchCall, Error>) in
        guard let self = self else { return }

        switch result {
        case .success(let call):
          // On success transfers to video call view controller.
          let videoCallViewController: VideoCallViewController = self.prepareViewController(identifier: "videoCall")

          videoCallViewController.call = call
          videoCallViewController.callCompletionDelegate = self

          self.recipientNameTextField.resignFirstResponder()
          self.present(videoCallViewController, animated: true)
        case .failure(let error):
          os_log("Video Call failed failed: %{public}@",
                 log: .sinchOSLog(for: MainViewController.identifier),
                 type: .error,
                 error.localizedDescription)
          self.prepareErrorAlert(with: error.localizedDescription)
        }
      })
  }

  @objc private func hideKeyboard() {
    recipientNameTextField.resignFirstResponder()
    phoneTextField.resignFirstResponder()
  }
}

extension MainViewController: UITextFieldDelegate {

  func textFieldShouldReturn(_ textField: UITextField) -> Bool {
    textField.resignFirstResponder()

    return true
  }
}
