import OSLog
import SinchRTC
import UIKit

final class VideoCallViewController: UIViewController {
    
    private enum Constant {
        
        static let cornerRadius: CGFloat = 6
    }
    
    @IBOutlet private var remoteUserName: UILabel!
    
    // Container for Sinch Video Controller to present remote user.
    @IBOutlet private var remoteVideoView: UIView! {
        didSet {
            remoteVideoView.contentMode = .scaleAspectFill
        }
    }
    
    // Container for Sinch Video Controller to present local user.
    @IBOutlet private var localVideoView: UIView! {
        didSet {
            localVideoView.contentMode = .scaleAspectFit
        }
    }
    
    @IBOutlet private var callTime: UILabel!
    
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
    
    @IBOutlet private var pauseButton: UIButton! {
        didSet {
            pauseButton.layer.cornerRadius = Constant.cornerRadius
        }
    }
    
    @IBOutlet private var cameraButton: UIButton! {
        didSet {
            cameraButton.layer.cornerRadius = Constant.cornerRadius
        }
    }
    
    @IBOutlet private var buttonsStack: UIStackView!
    
    @IBOutlet private var hangupButton: UIButton! {
        didSet {
            hangupButton.setup(with: .hangup)
        }
    }
    
    // To end call, should be passed to VideoCallViewController.
    var call: SinchCall?
    
    private var timer: Timer?
    
    private var paused: Bool = false
    private var muted: Bool = false
    private var speakerEnabled: Bool = true

    override func viewDidLoad() {
        super.viewDidLoad()
        
        hangupButton.isEnabled = call?.direction == .outgoing
        buttonsStack.isHidden = true
        
        // Add observer to track call actions.
        SinchClientMediator.instance.addObserver(self)
        
        guard let videoLocalView = SinchClientMediator.instance.sinchClient?.videoController.localView else { return }
        
        // Attach local video view to application hierarchy.
        localVideoView.addSubview(videoLocalView)
    }
    
    @IBAction private func togglePause(_ sender: Any) {
        guard let call = call else { return }
        
        paused
            ? call.resumeVideo()
            : call.pauseVideo()
        
        paused = !paused
        
        let pauseImage = paused ? UIImage(systemName: "play.rectangle") : UIImage(systemName: "pause.rectangle")
        pauseButton.setImage(pauseImage, for: .normal)
    }
    
    @IBAction private func toggleMute(_ sender: Any) {
        muted
            ? SinchClientMediator.instance.sinchClient?.audioController.unmute()
            : SinchClientMediator.instance.sinchClient?.audioController.mute()

        muted = !muted
        
        let mutedImage = muted ? UIImage(systemName: "mic.slash") : UIImage(systemName: "mic")
        muteButton.setImage(mutedImage, for: .normal)
    }
    
    @IBAction private func toggleSpeaker(_ sender: Any) {
        speakerEnabled
            ? SinchClientMediator.instance.sinchClient?.audioController.disableSpeaker()
            : SinchClientMediator.instance.sinchClient?.audioController.enableSpeaker()
        
        speakerEnabled = !speakerEnabled
        
        let speakerImage = speakerEnabled ? UIImage(systemName: "speaker") : UIImage(systemName: "speaker.slash")
        speakerButton.setImage(speakerImage, for: .normal)
    }
    
    // Switches camera from front to back and vice versa.
    @IBAction private func switchCamera(_ sender: Any) {
        SinchClientMediator.instance.sinchClient?
            .videoController
            .captureDevicePosition
            .toggle()
    }
    
    @IBAction private func hangup(_ sender: Any) {
        terminateCall()
        
        dismiss(animated: true)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        terminateCall()
    }
    
    private func terminateCall() {
        guard let call = call else {
            os_log("Video Call was set and ongoing: %{public}@",
                   log: .sinchOSLog(for: AudioCallViewController.identifier),
                   type: .error,
                   (call != nil).description)
            return
        }
        
        SinchClientMediator.instance.end(call: call)
    }
}

extension VideoCallViewController: SinchClientMediatorObserver {
    
    func callDidProgress(_ call: SinchCall) {
        let audio = Ringtone.ringback
        let path = Bundle.main.path(forResource: audio, ofType: nil)
        
        do {
            try SinchClientMediator.instance.sinchClient?
                .audioController
                .startPlayingSoundFile(withPath: path, looping: true)
        } catch {
            os_log("WARNING: path for resource %{public}@ not found in the main bundle",
                   log: .sinchOSLog(for: AudioCallViewController.identifier),
                   type: .error,
                   audio)
        }
    }

    func callDidEstablish(_ call: SinchCall) {
        hangupButton.isEnabled = true
        buttonsStack.isHidden = false
        
        guard timer == nil else { return }
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { _ in
            let interval = Int(Date().timeIntervalSince(call.details.establishedTime ?? Date()))
            
            let minutes = Int(interval / 60).timePresentation
            let seconds = Int(interval % 60).timePresentation
            
            // Displays some call information when call was answered.
            self.remoteUserName.text = call.remoteUserId
            self.callTime.text = "\(minutes):\(seconds)"
        })
        
        // Stop playing sound when call was answered.
        SinchClientMediator.instance.sinchClient?
            .audioController
            .stopPlayingSoundFile()
        
        // For video controller enabling speaker after establishing connection.
        SinchClientMediator.instance.sinchClient?.audioController.enableSpeaker()
    }
    
    // When video stream is available, this method will be called.
    func callDidAddVideoTrack(_ call: SinchCall) {
        guard let videoRemoteView = SinchClientMediator.instance.sinchClient?.videoController.remoteView else { return }

        remoteVideoView.addSubview(videoRemoteView)
    }

    func callDidPauseVideoTrack(_ call: SinchCall) {
        os_log("VideoController delegate: video was paused",
               log: .sinchOSLog(for: VideoCallViewController.identifier),
               type: .info)
    }

    func callDidResumeVideoTrack(_ call: SinchCall) {
        os_log("VideoController delegate: video was resumed",
               log: .sinchOSLog(for: VideoCallViewController.identifier),
               type: .info)
    }
    
    func callDidEnd(_ call: SinchCall) {
        timer?.invalidate()
        timer = nil
        
        // For video controller disabling speaker when call ends.
        SinchClientMediator.instance.sinchClient?.audioController.disableSpeaker()
                
        // Finish call, by stop playing sound, dimissing and removing observers.
        dismiss(animated: true)
        SinchClientMediator.instance.sinchClient?
            .audioController
            .stopPlayingSoundFile()
        SinchClientMediator.instance.removeObserver(self)
        
        dismiss(animated: true)
    }
}
