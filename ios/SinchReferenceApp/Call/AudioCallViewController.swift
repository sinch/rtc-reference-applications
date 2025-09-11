import Combine
import SinchRTC
import UIKit

final class AudioCallViewController: UIViewController {

  private enum Constant {

    static let cornerRadius: CGFloat = 6
  }

  @IBOutlet private var infoLabel: UILabel!

  @IBOutlet private var muteButton: UIButton! {
    didSet {
      muteButton.layer.cornerRadius = Constant.cornerRadius
    }
  }

  @IBOutlet private var speakerButton: UIButton! {
    didSet {
      speakerButton.layer.cornerRadius = Constant.cornerRadius
    }
  }

  @IBOutlet private var hangupButton: UIButton! {
    didSet {
      hangupButton.setup(with: .hangup)
    }
  }

  var viewModel: CallViewModel!

  private var cancellableBag = Set<AnyCancellable>()

  weak var callCompletionDelegate: CallCompletionDelegate?

  override func viewDidLoad() {
    super.viewDidLoad()

    assignEnablability()
    assignPresentation()

    viewModel.$state.map(\.info)
      .removeDuplicates()
      .assign(to: \.text, on: infoLabel)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.status)
      .removeDuplicates()
      .sink { [weak self] status in
        guard let self = self else { return }

        switch status {
          case .event(let event):
            let banner = CallQualityEventBanner(message: event.message,
                                                triggered: event.triggered,
                                                for: self.view)
            banner.show()
          case .end(let call, let duration):
            self.dismiss(animated: true, completion: { [weak self] in
              guard let self = self else { return }

              self.callCompletionDelegate?.provideCallDetails(for: call, with: duration)
            })
          default:
            break
        }
      }
      .store(in: &cancellableBag)
  }

  private func assignEnablability() {
    viewModel.$state.map(\.couldHangUp)
      .removeDuplicates()
      .assign(to: \.isEnabled, on: hangupButton)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.couldMute)
      .removeDuplicates()
      .assign(to: \.isEnabled, on: muteButton)
      .store(in: &cancellableBag)

    viewModel.$state.map(\.couldEnableSpeaker)
      .removeDuplicates()
      .assign(to: \.isEnabled, on: speakerButton)
      .store(in: &cancellableBag)
  }

  private func assignPresentation() {
    viewModel.$state.map(\.muted)
      .removeDuplicates()
      .sink { [weak self] muted in
        guard let self = self else { return }

        let mutedImage = muted
          ? UIImage(systemName: "mic.slash")
          : UIImage(systemName: "mic")

        self.muteButton.setImage(mutedImage, for: .normal)
      }
      .store(in: &cancellableBag)

    viewModel.$state.map(\.speakerEnabled)
      .removeDuplicates()
      .sink { [weak self] speakerEnabled in
        guard let self = self else { return }

        let speakerImage = speakerEnabled
          ? UIImage(systemName: "speaker.wave.2")
          : UIImage(systemName: "speaker.slash")

        self.speakerButton.setImage(speakerImage, for: .normal)
      }
      .store(in: &cancellableBag)
  }

  @IBAction private func toggleMute(_ sender: Any) {
    viewModel.toggleMute()
  }

  @IBAction private func toggleSpeaker(_ sender: Any) {
    viewModel.toggleSpeaker()
  }

  @IBAction private func hangup(_ sender: Any) {
    viewModel.terminate()

    dismiss(animated: true)
  }

  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    viewModel.terminate()
  }
}
