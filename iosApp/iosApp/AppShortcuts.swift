import AppIntents

// The actions of the Shortcuts app (and Siri, Spotlight), opening a part of the app. They're the quick actions' ones
// (Info.plist's UIApplicationShortcutItems), their ids being those of the Kotlin AppShortcut. ComposeApp isn't imported
// here: its AppShortcut would clash with App Intents' one

struct OpenDamageCalcIntent: AppIntent {
    static let title: LocalizedStringResource = "Open the Damage Calc"
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        _ = openShortcut("damage-calc")
        return .result()
    }
}

struct OpenShowdownReplaysIntent: AppIntent {
    static let title: LocalizedStringResource = "Open the Showdown Replays"
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        _ = openShortcut("showdown-replays")
        return .result()
    }
}

struct OpenPokedexIntent: AppIntent {
    static let title: LocalizedStringResource = "Open the Pokédex"
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        _ = openShortcut("pokedex")
        return .result()
    }
}

/// Shown in the Shortcuts app without the user having to set anything up. Only SF Symbols can be their icons
struct AlakastatsShortcuts: AppShortcutsProvider {
    static let shortcutTileColor: ShortcutTileColor = .grayBrown

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenDamageCalcIntent(),
            phrases: ["Open the damage calc in \(.applicationName)", "\(.applicationName) damage calc"],
            shortTitle: "Damage Calc",
            systemImageName: "plus.forwardslash.minus"
        )
        AppShortcut(
            intent: OpenShowdownReplaysIntent(),
            phrases: ["Open the replays in \(.applicationName)", "\(.applicationName) replays"],
            shortTitle: "Replays",
            systemImageName: "play.fill"
        )
        AppShortcut(
            intent: OpenPokedexIntent(),
            phrases: ["Open the Pokédex in \(.applicationName)", "\(.applicationName) Pokédex"],
            shortTitle: "Pokédex",
            systemImageName: "book.closed.fill"
        )
    }
}
