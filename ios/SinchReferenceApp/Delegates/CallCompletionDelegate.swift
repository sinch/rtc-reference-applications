import Foundation
import SinchRTC

protocol CallCompletionDelegate: AnyObject {

  func provideCallDetails(for call: SinchCall?, with duration: String)
}

extension UIViewController: CallCompletionDelegate {

  func provideCallDetails(for call: SinchCall?, with duration: String) {
    guard let call = call else { return }

    var date = ""
    var details = "Call Duration: \(duration)\n"

    if let startTime = call.details.startedTime {
      date = "(" + startTime.toDateString + ")"
      details += "Start time: " + startTime.toTimeString + "\n"
    }

    if let progressTime = call.details.progressedTime {
      details += "Progress time: " + progressTime.toTimeString + "\n"
    }

    if let ringTime = call.details.rungTime {
      details += "Ring time: " + ringTime.toTimeString + "\n"
    }

    if let answerTime = call.details.answeredTime {
      details += "Answer time: " + answerTime.toTimeString + "\n"
    }

    if let establishTime = call.details.establishedTime {
      details += "Establish time: " + establishTime.toTimeString + "\n"
    }

    if let endTime = call.details.endedTime {
      details += "End time (\(call.details.endCause.rawValue)): " + endTime.toTimeString
    }

    self.prepareAlert(title: "Call Details \(date)", message: details)
  }
}
