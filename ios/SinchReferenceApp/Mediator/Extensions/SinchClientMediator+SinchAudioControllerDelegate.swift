import OSLog
import SinchRTC

// Delegate to perform actions when microphone or speaker was enabled / disabled.
extension SinchClientMediator: SinchAudioControllerDelegate {
    
    func audioControllerMuted(_ audioController: SinchRTC.SinchAudioController) {
        os_log("AudioController delegate: microphone was muted",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .info)
    }
    
    func audioControllerUnmuted(_ audioController: SinchRTC.SinchAudioController) {
        os_log("AudioController delegate: microphone was unmuted",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .info)
    }
    
    func audioControllerSpeakerEnabled(_ audioController: SinchRTC.SinchAudioController) {
        os_log("AudioController delegate: speaker was enabled",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .info)
    }
    
    func audioControllerSpeakerDisabled(_ audioController: SinchRTC.SinchAudioController) {
        os_log("AudioController delegate: speaker was disabled",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .info)
    }
}
