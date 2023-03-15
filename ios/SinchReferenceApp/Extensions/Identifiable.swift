import Foundation

protocol Identifiable {
    
    static var identifier: String { get }
}

// Helper for OSLog to get names of class from what logger was called.
extension NSObject: Identifiable {
    
    static var identifier: String {
        return String(describing: Self.self)
    }
}
