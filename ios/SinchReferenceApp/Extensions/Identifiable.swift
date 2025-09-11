import Foundation

protocol Identifiable {

  static var identifier: String { get }
}

extension NSObject: Identifiable {

  static var identifier: String {
    return String(describing: Self.self)
  }
}
