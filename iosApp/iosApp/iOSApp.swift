import SwiftUI

@main
struct iOSApp: App {
    // for the quick actions
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}