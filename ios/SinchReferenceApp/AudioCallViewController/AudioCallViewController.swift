import OSLog
import SinchRTC
import UIKit

final class AudioCallViewController: UIViewController {
    
    @IBOutlet private var callInfoLabel: UILabel!
    
    @IBOutlet private var hangupButton: UIButton!
    
    // To end call, should be passed to AudioCallViewController.
    var call: SinchCall?
    
    private var timer: Timer?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        hangupButton.isEnabled = call?.direction == .outgoing
        
        // Add observer to track call actions.
        SinchClientMediator.instance.addObserver(self)
    }
    
    // End call implementation.
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
            os_log("Call was set and ongoing: %{public}@",
                   log: .sinchOSLog(for: AudioCallViewController.identifier),
                   type: .error,
                   (call != nil).description)
            return
        }
        
        SinchClientMediator.instance.end(call: call)
    }
}

// SinchCallDelegate methods implementation and handling of call progression.
extension AudioCallViewController: SinchClientMediatorObserver {
    
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
        
        guard timer == nil else { return }
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { _ in
            let interval = Int(Date().timeIntervalSince(call.details.establishedTime ?? Date()))
            
            let minutes = Int(interval / 60).timePresentation
            let seconds = Int(interval % 60).timePresentation
            
            // Displays some call information when call was answered.
            self.callInfoLabel.text = "\(minutes):\(seconds) with \(call.remoteUserId)"
        })
        
        // Stop playing sound when call was answered.
        SinchClientMediator.instance.sinchClient?
            .audioController
            .stopPlayingSoundFile()
    }
    
    func callDidEnd(_ call: SinchCall) {
        timer?.invalidate()
        timer = nil
        
        // Finish call, by stop playing sound, dimissing and removing observers.
        dismiss(animated: true)
        SinchClientMediator.instance.sinchClient?
            .audioController
            .stopPlayingSoundFile()
        SinchClientMediator.instance.removeObserver(self)
        
        dismiss(animated: true)
    }
}
