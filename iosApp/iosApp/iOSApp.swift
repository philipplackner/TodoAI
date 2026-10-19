import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
