package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import com.tambapps.pokemon.alakastats.ui.composables.HTML_BRIDGE_JS

/**
 * The page the web views load to play a replay back, with the base URL they should give it
 */
const val REPLAY_HTML_BASE_URL = "https://replay.pokemonshowdown.com/"

private const val SHOWDOWN_CLIENT = "https://play.pokemonshowdown.com"

// what Showdown's replay-embed.js loads, in the order they depend on each other
private val SHOWDOWN_STYLES = listOf(
    "style/font-awesome.css",
    "style/battle.css?a7",
    "style/replay.css?a7",
    "style/utilichart.css?a7",
)
private val SHOWDOWN_SCRIPTS = listOf(
    "js/lib/ps-polyfill.js",
    "config/config.js?a7",
    "js/lib/jquery-1.11.0.min.js",
    "js/lib/html-sanitizer-minified.js",
    "js/battle-sound.js",
    "js/battledata.js?a7",
    "data/pokedex-mini.js?a7",
    "data/pokedex-mini-bw.js?a7",
    "data/graphics.js?a7",
    "data/pokedex.js?a7",
    "data/moves.js?a7",
    "data/abilities.js?a7",
    "data/items.js?a7",
    "data/teambuilder-tables.js?a7",
    "js/battle-tooltips.js?a7",
    "js/battle.js?a7",
)

/**
 * Builds a page playing the replay back with Showdown's battle engine, loading it, the sprites and the styles from
 * play.pokemonshowdown.com. The page has no controls: the app drives the battle through [ReplayPlayer], and the
 * page posts it the [ReplayPlaybackState] on each change. Music and sound effects are off.
 *
 * It doesn't use Showdown's replay-embed.js, which adds the scripts to the page so they run in whatever order they
 * load: when graphics.js runs before battledata.js, the move animations' images get no URL prefix and don't load
 * @param log the protocol log of the replay
 * @param asP2 whether to show the battle from the second player's side
 * @param dark whether to use Showdown's dark color scheme
 */
fun replayHtml(log: String, asP2: Boolean, dark: Boolean): String {
    // turned back into / by the page, it keeps a </script> in the log from closing the tag
    val escapedLog = log.replace("/", "\\/")
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
${SHOWDOWN_STYLES.joinToString("\n") { """<link rel="stylesheet" href="$SHOWDOWN_CLIENT/$it">""" }}
<style>
  body { margin: 0; }
  .wrapper { max-width: 1180px; margin: 0 auto; padding: 8px 4px; box-sizing: border-box; }
  /* the battle is at a fixed position, the log on its right */
  .replay-wrapper { padding-top: 372px !important; }
  /*
   * on narrow screens, the log goes below the battle as on the replay site, taking the rest of the height. Under
   * 640px, the battle scales itself down to the width of the page, which only works when it is in the flow
   */
  @media (max-width: 820px) {
    html, body { overflow-x: hidden; }
    .replay-wrapper { padding: 0 !important; }
    .battle { position: relative; top: 0; left: 0; margin: 0 auto; }
    .battle-log {
      position: static;
      margin: 4px auto 0;
      max-width: 640px;
      height: calc(100vh - min(100vw * 360 / 640, 360px) - 8px);
      min-height: 150px;
    }
  }
</style>
</head>
<body${if (dark) " class=\"dark\"" else ""}>
<div class="wrapper replay-wrapper">
  <div class="battle"></div>
  <div class="battle-log"></div>
</div>
<script type="text/plain" id="battle-log-data">$escapedLog</script>
${SHOWDOWN_SCRIPTS.joinToString("\n") { """<script src="$SHOWDOWN_CLIENT/$it"></script>""" }}
<script>
  $HTML_BRIDGE_JS
  var battle = new Battle({
    ${'$'}frame: $('.battle'),
    ${'$'}logFrame: $('.battle-log'),
    log: document.getElementById('battle-log-data').textContent.replace(/\\\//g, '/').split('\n'),
    isReplay: true,
    paused: true,
    autoresize: true
  });
  // mutes the music and the sound effects
  battle.setMute(true);
  if ($asP2) battle.switchViewpoint();

  // the speeds of replay-embed.js
  var messageFadeTimes = { hyperfast: 40, fast: 50, normal: 300, slow: 500, reallyslow: 1000 };
  var messageShownTimes = { hyperfast: 1, fast: 1, normal: 1, slow: 1000, reallyslow: 3000 };
  function setSpeed(speed) {
    battle.messageShownTime = messageShownTimes[speed];
    battle.messageFadeTime = messageFadeTimes[speed];
    battle.scene.updateAcceleration();
  }

  function postState() {
    postToApp(JSON.stringify({
      turn: battle.turn,
      paused: battle.paused,
      atEnd: battle.atQueueEnd,
      started: battle.started
    }));
  }
  battle.subscribe(postState);
  postState();
</script>
</body>
</html>
"""
}

/**
 * The number of turns of the replay, read from its log
 */
fun replayTurnCount(log: String): Int = log.lineSequence().count { it.startsWith("|turn|") }
