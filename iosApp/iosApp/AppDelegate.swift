import UIKit
import ComposeApp

/// Goes to the screen of a shortcut (quick action or App Intent). @param id the id of a Kotlin AppShortcut
/// - Returns: whether it's one of a shortcut
func openShortcut(_ id: String) -> Bool {
    IosShortcutsKt.openAppShortcut(id: id)
}

/// Only there to give the app's scene a delegate, the quick actions (Info.plist's UIApplicationShortcutItems) going to
/// the scene's: SwiftUI has nothing to receive them
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }
}

class SceneDelegate: NSObject, UIWindowSceneDelegate {
    /// The app launched by a quick action
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let shortcutItem = connectionOptions.shortcutItem {
            _ = openShortcut(shortcutItem.type)
        }
    }

    /// A quick action while the app is running
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        completionHandler(openShortcut(shortcutItem.type))
    }
}
