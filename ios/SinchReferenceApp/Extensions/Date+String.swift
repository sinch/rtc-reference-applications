import Foundation

extension Date {

  private enum FormatConstant {

    static let date: String = "yyyy-MM-dd"
    static let time: String = "HH:mm:ss"
  }

  private func formatDate(with format: String) -> String {
    let dateFormatter = DateFormatter()
    dateFormatter.dateFormat = format

    return dateFormatter.string(from: self)
  }

  var toDateString: String {
    return formatDate(with: FormatConstant.date)
  }

  var toTimeString: String {
    return formatDate(with: FormatConstant.time)
  }
}
