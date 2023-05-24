import UIKit

extension UIButton {
    
    fileprivate enum Constant {
        
        static let cornerRadius: CGFloat = 6
        static let textSize: CGFloat = 16
        static let textImagePadding: CGFloat = 8
        static let loginBackgroundColor = UIColor.systemYellow
        static let callBackgroundColor = UIColor(red: 100 / 255, green: 196 / 255, blue: 148 / 255, alpha: 1)
        static let hangupBackgroundColor = UIColor(red: 235 / 255, green: 127 / 255, blue: 110 / 255, alpha: 1)
        static let disabledBackgroundColor = UIColor(red: 239 / 255, green: 239 / 255, blue: 240 / 255, alpha: 0.8)
        static let defaultAssetColor = UIColor.white
        static let disabledAssetColor = UIColor(red: 189 / 255, green: 189 / 255, blue: 191 / 255, alpha: 1)
    }
    
    struct SinchButtonConfiguration {
        
        let backgroundColor: UIColor
        let textColor: UIColor
        let image: UIImage?
        
        init(backgroundColor: UIColor, textColor: UIColor = Constant.defaultAssetColor, image: UIImage? = nil) {
            self.backgroundColor = backgroundColor
            self.textColor = textColor
            self.image = image
        }
    }
    
    func setup(with configuration: SinchButtonConfiguration) {
        if #available(iOS 15.0, *) {
            setupConfigurableButton(with: configuration)
        } else {
            setupInsetsButton(with: configuration)
        }
    }
    
    @available(iOS 15.0, *)
    func setupConfigurableButton(with sinchConfiguration: SinchButtonConfiguration) {
        var configuration = UIButton.Configuration.filled()
        configuration.image = sinchConfiguration.image
        configuration.imagePlacement = .trailing
        configuration.imagePadding = Constant.textImagePadding
        
        configuration.baseBackgroundColor = sinchConfiguration.backgroundColor
        
        self.configuration = configuration
    }
    
    func setupInsetsButton(with configuration: SinchButtonConfiguration) {
        self.layer.cornerRadius = Constant.cornerRadius
        self.layer.backgroundColor = self.isEnabled
            ? configuration.backgroundColor.cgColor
            : Constant.disabledBackgroundColor.cgColor
        
        self.titleLabel?.font = .systemFont(ofSize: Constant.textSize)
        self.setTitleColor(configuration.textColor, for: .normal)
        self.setTitleColor(Constant.disabledAssetColor, for: .disabled)
        
        guard let image = configuration.image else { return }
        
        self.setImage(image, for: .normal)
        self.imageView?.tintColor = self.isEnabled
            ? Constant.defaultAssetColor
            : Constant.disabledAssetColor
        
        self.semanticContentAttribute = .forceRightToLeft
        
        self.titleEdgeInsets.left -= Constant.textImagePadding
        self.imageEdgeInsets.left += (Constant.textImagePadding * 2)
    }
}

extension UIButton.SinchButtonConfiguration {
    
    static let login = UIButton.SinchButtonConfiguration(backgroundColor: UIButton.Constant.loginBackgroundColor)
    
    static let call = UIButton.SinchButtonConfiguration(backgroundColor: UIButton.Constant.callBackgroundColor, image: UIImage(systemName: "phone"))
    
    static let hangup = UIButton.SinchButtonConfiguration(backgroundColor: UIButton.Constant.hangupBackgroundColor, image: UIImage(systemName: "phone.down"))
}
