import Foundation
import UIKit

final class CallQualityEventBanner: UIView {

  private enum Constant {

    static let greenBackground: UIColor = .init(red: 193 / 255, green: 255 / 255, blue: 200 / 255, alpha: 1.0)
    static let redBackground: UIColor = .init(red: 255 / 255, green: 182 / 255, blue: 193 / 255, alpha: 1.0)

    static let conrnerRadius: CGFloat = 6
    static let shadowOpacity: Float = 0.2
    static let shadowOffset: CGSize = .init(width: 0, height: 2)
    static let shadowRadius: CGFloat = 4.0

    static let fontSize: CGFloat = 14
    static let messageNumberOfLines: Int = 2

    static let bannerHeight: CGFloat = 96

    static let animationDuration: TimeInterval = 0.3
    static let dismissDuration: TimeInterval = 3.0

    static let backgroundHorizontalPadding: CGFloat = 24
    static let backgroundVerticalPadding: CGFloat = 12

    static let messageHorizontalPadding: CGFloat = 12
    static let messageVerticalPadding: CGFloat = 12
  }

  private let messageLabel = UILabel()
  private let background = UIView()

  private var parentView = UIView()

  init(message: String, triggered: Bool, for parentView: UIView) {
    let frame = CGRect(x: 0, y: 0, width: parentView.frame.width, height: Constant.bannerHeight)

    super.init(frame: frame)

    self.parentView = parentView
    setupView(message: message, triggered: triggered)
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)

    setupView(message: "", triggered: false)
  }

  private func setupView(message: String, triggered: Bool) {
    self.backgroundColor = .clear

    background.layer.cornerRadius = Constant.conrnerRadius
    background.layer.backgroundColor = triggered ? Constant.redBackground.cgColor : Constant.greenBackground.cgColor

    background.layer.shadowColor = UIColor.black.cgColor
    background.layer.shadowOpacity = Constant.shadowOpacity
    background.layer.shadowOffset = Constant.shadowOffset
    background.layer.shadowRadius = Constant.shadowRadius
    background.layer.masksToBounds = false

    messageLabel.text = message
    messageLabel.font = UIFont.systemFont(ofSize: Constant.fontSize)
    messageLabel.textColor = .darkGray
    messageLabel.textAlignment = .left
    messageLabel.lineBreakMode = .byClipping
    messageLabel.numberOfLines = Constant.messageNumberOfLines
    messageLabel.adjustsFontSizeToFitWidth = true

    self.addSubview(background)
    background.addSubview(messageLabel)
  }

  func show() {
    frame = CGRect(x: 0, y: -frame.height, width: parentView.frame.width, height: frame.height)

    parentView.addSubview(self)

    UIView.animate(withDuration: Constant.animationDuration, animations: { [weak self] in
      guard let self = self else { return }

      self.frame.origin.y = 0
    }, completion: { [weak self] _ in
      guard let self = self else { return }

      DispatchQueue.main.asyncAfter(deadline: .now() + Constant.dismissDuration) {
        self.hide()
      }
    })
  }

  private func hide() {
    UIView.animate(withDuration: Constant.animationDuration, animations: { [weak self] in
      guard let self = self else { return }

      self.frame.origin.y = -self.frame.height
    }, completion: { [weak self] _ in
      guard let self = self else { return }

      self.removeFromSuperview()
    })
  }

  override func layoutSubviews() {
    super.layoutSubviews()

    background.frame = CGRect(x: Constant.backgroundHorizontalPadding,
                              y: Constant.backgroundVerticalPadding,
                              width: frame.width - (Constant.backgroundHorizontalPadding * 2),
                              height: frame.height - (Constant.backgroundVerticalPadding * 2))
    messageLabel.frame = CGRect(x: Constant.messageHorizontalPadding,
                                y: Constant.messageVerticalPadding,
                                width: background.frame.width - (Constant.messageHorizontalPadding * 2),
                                height: background.frame.height - (Constant.messageVerticalPadding * 2))
  }
}
