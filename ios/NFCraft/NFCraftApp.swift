import SwiftUI

@main
struct NFCraftApp: App {
    @StateObject private var nfc = NFCService()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(nfc)
                .tint(Color(red: 0.18, green: 0.36, blue: 1.0))
        }
    }
}
