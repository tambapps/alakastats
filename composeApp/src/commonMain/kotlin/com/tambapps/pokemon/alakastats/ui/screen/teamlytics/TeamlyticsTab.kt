package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

/**
 * The tabs of a team, in the order of its pager
 *
 * @param title what the tab displays
 * @param path how the web app's URL tells it
 */
enum class TeamlyticsTab(val title: String, val path: String) {
    OVERVIEW("Overview", "overview"),
    REPLAYS("Replays", "replays"),
    USAGES("Usages", "usages"),
    LEAD_STATS("Lead Stats", "lead-stats"),
    OPPONENT_TRENDS("Opponent Trends", "opponent-trends"),
    GAME_PLANS("Game Plans", "game-plans"),
    DAMAGE_CALC("Damage Calc", "damage-calc"),
    QUIZZES("Quizzes", "quizzes"),
}
