import Foundation

extension Int {
    
    // Adds a leading zero to a single digit integer, if double digit integer, zero won't be added.
    // Presentation helper to format integer to look as a time(minutes and seconds).
    var timePresentation: String {
        return String(format: "%02d", self)
    }
}
