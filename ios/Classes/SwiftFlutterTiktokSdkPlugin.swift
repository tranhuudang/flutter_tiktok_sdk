import Flutter
import UIKit
import TikTokOpenSDK

public class SwiftFlutterTiktokSdkPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.k9i/flutter_tiktok_sdk", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterTiktokSdkPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    registrar.addApplicationDelegate(instance)
  }
  
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "setup":
      result(nil)
    case "login":
      login(call, result: result)
    default:
      result(FlutterMethodNotImplemented)
      return
    }
  }
  
  public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    TikTokOpenSDKApplicationDelegate.sharedInstance().application(application, didFinishLaunchingWithOptions: launchOptions)
    return true
  }
  
  public func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    
    guard let sourceApplication = options[UIApplication.OpenURLOptionsKey.sourceApplication] as? String,
          let annotation = options[UIApplication.OpenURLOptionsKey.annotation] else {
      return false
    }
    
    if TikTokOpenSDKApplicationDelegate.sharedInstance().application(app, open: url, sourceApplication: sourceApplication, annotation: annotation) {
      return true
    }
    return false
  }
  
  public func application(_ application: UIApplication, open url: URL, sourceApplication: String?, annotation: Any) -> Bool {
    if TikTokOpenSDKApplicationDelegate.sharedInstance().application(application, open: url, sourceApplication: sourceApplication, annotation: annotation) {
      return true
    }
    return false
  }
  
  public func application(_ application: UIApplication, handleOpen url: URL) -> Bool {
    if TikTokOpenSDKApplicationDelegate.sharedInstance().application(application, open: url, sourceApplication: nil, annotation: "") {
      return true
    }
    return false
  }
  
  public func login(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any] else {
      result(FlutterError.nilArgument)
      return
    }
    
    guard let scope = args["scope"] as? String else {
      result(FlutterError.failedArgumentField("scope", type: String.self))
      return
    }
    
    guard let rootViewController = UIApplication.shared.keyWindow?.rootViewController else {
      result(nil)
      return
    }
    
    let request = TikTokOpenSDKAuthRequest()
    let scopes = scope.split(separator: ",")
    let scopesSet = NSOrderedSet(array:scopes)
    request.permissions = scopesSet
    
    request.send(rootViewController, completion: { (resp : TikTokOpenSDKAuthResponse) -> Void in
      if resp.isSucceed {
        let authorizationCode = resp.code
        result(authorizationCode)
      } else {
        result(FlutterError(
          code: String(resp.errCode.rawValue),
          message: resp.errString,
          details: nil
        ))
      }
    })
  }
}

extension FlutterError {
  static let nilArgument = FlutterError(
    code: "argument.nil",
    message: "Expect an argument when invoking channel method, but it is nil.", details: nil
  )
  
  static func failedArgumentField<T>(_ fieldName: String, type: T.Type) -> FlutterError {
    return .init(
      code: "argument.failedField",
      message: "Expect a `\(fieldName)` field with type <\(type)> in the argument, " +
      "but it is missing or type not matched.",
      details: fieldName)
  }
}