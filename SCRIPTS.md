# Data scripts

The data files bundled with the app, in `composeApp/src/commonMain/composeResources/files/`, are generated or completed
by scripts living in `data-scripts/`: [Marcel](https://github.com/tambapps/marcel) scripts run with `marcl`, and the
Node.js generator of the Pokédex. They are meant to be run from that folder:

```bash
cd data-scripts
```

The scripts and their sources are kept out of `composeResources`, as everything in it is bundled in the apps: only the
JSON files the app reads go there.

The YAML files the scripts read (`pokemon-sprites.yaml`, `items-mapping.yaml`, `moves.yaml`) are ignored by git: they
only exist on the machine maintaining the data, in `data-scripts/`. The app only reads the JSON files generated from
them, which are versioned.

## `yaml_to_json.mcl`: a YAML mapping to its JSON

```bash
marcl yaml_to_json.mcl <file.yaml>
```

Writes the JSON of a YAML file to the app's resources, with the same name (`pokemon-sprites.yaml` gives
`composeResources/files/pokemon-sprites.json`).
Used after editing one of the YAML mappings:

| YAML (source, not versioned) | JSON (read by the app)  | Content                                                |
|------------------------------|-------------------------|--------------------------------------------------------|
| `pokemon-sprites.yaml`       | `pokemon-sprites.json`  | the name, sprite and artwork URLs of each Pokémon/form |
| `items-mapping.yaml`         | `items-mapping.json`    | the name and sprite URL of each item                   |
| `moves.yaml`                 | `moves.json`            | the name, category and type of each move               |

The key of an entry is the image name of the Pokémon (e.g. `lucario-mega-z`), the one the app builds from its name.

## `download_pokemon_images.mcl`: the Pokémon images of the web app

```bash
marcl download_pokemon_images.mcl pokemon-sprites.yaml
```

Downloads the sprite and the artwork of every Pokémon of the mapping, to
`composeApp/src/wasmJsMain/resources/images/pokemons/sprite/<key>.png` and `.../artwork/<key>.png`: the web app serves
its images itself, rather than fetching them from the URLs of the mapping like the mobile apps do.

- Every image is downloaded again, overwriting the existing ones.
- A URL answering an error is printed and skipped, any other failure stops the script.
- The output folder is an absolute path of the script, to update if the repository is cloned elsewhere.
- The images are versioned, for the GitHub Action deploying the web app: commit the new and changed ones.

## `download_pokemon_items.mcl`: the item images of the web app

```bash
marcl download_pokemon_items.mcl items-mapping.yaml
```

The same for items: downloads the `spriteUrl` of every item of the mapping to
`composeApp/src/wasmJsMain/resources/images/items/<key>.png`, with the same behaviors as the Pokémon images (its
progress says "pokemons", but counts items).

## `ncp_sets_to_json.mcl`: the damage calc's preset sets

```bash
marcl ncp_sets_to_json.mcl <path to NCP-VGC-Damage-Calculator>/script_res/setdex_ncp-g10.js [output file]
```

Converts the Pokémon Champions sets of the [Nerd of Now damage calculator](https://github.com/nerd-of-now/NCP-VGC-Damage-Calculator)
into `composeResources/files/sets/champions.json` (or the given output file), the sets the damage calc proposes for a Pokémon.

- The source is a JS file (`var SETDEX_GEN10 = {...}`), with comments and trailing commas: the script cleans it up into
  JSON before reading it.
- The output has, for each species, its sets in the source's order: the first one is applied when the species is picked.
- A set without ability in the source gets an empty one, the app then using the species' default ability (like the
  source).
- `PresetSetsFileTest` checks the result: every species can be picked in the damage calc, and every ability, item and
  move is known to Champions. A failure lists every problem.
- The sets are MIT licensed: `sets/NCP-LICENSE` must stay next to them.

### Updating the sets

When Nerd of Now publishes new sets (e.g. for a new regulation):

```bash
git -C <path to NCP-VGC-Damage-Calculator> pull
cd data-scripts
marcl ncp_sets_to_json.mcl <path to NCP-VGC-Damage-Calculator>/script_res/setdex_ncp-g10.js
cd -
./gradlew :composeApp:wasmJsBrowserTest --tests "*PresetSetsFileTest*"
```

Then commit and push `sets/champions.json` to `main`: the apps fetch it from the repository, so the new sets are used
without releasing a new version (the bundled file being the fallback when offline).

## `generate_pokedex.js`: the Pokédex of the current Champions format

```bash
node generate_pokedex.js [path to pokemon-showdown] [path to pokeapi]/data/v2/csv [format id]
```

Generates `composeResources/files/pokedex/champions.json`, read by `LocalPokedexRepository`: every Pokémon legal in the
format (`gen9championsvgc2026regmc` by default), its types, base stats, abilities (the hidden one apart) and movepool,
and the moves, abilities, items and types, with their English and French names and descriptions. The app keeps it in
memory, with indexes for its queries (`Pokedex`).

It needs [Node.js](https://nodejs.org) and two checkouts, siblings of the alakastats one by default:

- [Pokémon Showdown](https://github.com/smogon/pokemon-showdown), the source of the game data, built (`node build` in
  its folder). The script uses Showdown's own `Dex` on the format (which applies its mod, e.g. `data/mods/champions`)
  and team validator (which decides what a Pokémon learns): Showdown's rules aren't reimplemented.
- [PokéAPI](https://github.com/PokeAPI/pokeapi), for its CSVs (`data/v2/csv`, a sparse checkout is enough): the French
  names and descriptions. Showdown's French translations come next, then `pokedex-fr-overrides.json`.

What it does with the data:

- A Mega is an entry of its own (its stats, types and ability), learning the moves of the form it mega evolves from,
  with its Mega Stone.
- Forms only taken in battle (e.g. Aegislash-Blade, Palafin-Hero) are left out; cosmetic forms (e.g. Vivillon-Polar,
  or any form playing exactly like its base form) are listed in the `cosmeticForms` of their base form.
- A move's PP are its PP in battle, computed like the format's mod does (Champions: 5 gives 8, 10 gives 12, 15 gives
  16), and its `classifications` the families some abilities and items act on: Showdown's flags (punching, sound,
  dance, slicing, wind, powder, ball & bomb, pulse, biting, healing), the moves Damp prevents (explosive) and the ones
  Aroma Veil protects from (mental), read from these abilities.
- `championsModified` tells a move, ability or item Champions changed (its legality and the PP cap left aside). The
  English descriptions are Showdown's Champions ones; the French description of a modified entry is left out (null)
  unless PokéAPI has the one of Champions, the others describing the main games' version.
- The ids (and the references between entries) are the names of the app: Showdown's English names normalized like
  `PokemonNormalizer.normalize` does, e.g. `charizard-mega-y`, `trick-room`, `king's-rock`. `Pokedex` finds an entry
  by its normalized name, else by its name without case, accents and punctuation (`King’s Shield`, `Kings Shield`).
- The JSON has one line per entry, for a regulation's changes to be read in its diff, and its `meta` gives the format
  and the commits of Showdown and PokéAPI it was generated from.

The script fails without writing anything when something is missing, listing every problem: a Pokémon, move, ability,
item or type without French name, which `pokedex-fr-overrides.json` completes (`species`, `moves`, `abilities`,
`items`, `types`, by Showdown id, e.g. `charizardmegay`), along with the PokéAPI identifiers of the forms Showdown names otherwise
(`pokeApiIds`, e.g. `tauros-paldea-combat-breed`). `PokedexFileTest` checks the result: every entry has its names, a
Mega learns its base form's moves, the stats and types agree with the damage calc's data, and the preset sets only use
moves their Pokémon learn.

### Updating the Pokédex for a new regulation

```bash
git -C <path to pokemon-showdown> pull
(cd <path to pokemon-showdown> && node build)
git -C <path to pokeapi> pull
cd data-scripts
node generate_pokedex.js "" "" <id of the new format>
cd -
./gradlew :composeApp:wasmJsBrowserTest --tests "*Pokedex*"
```

Review the diff of `pokedex/champions.json`, update the format expected by `PokedexFileTest` and the default one of
the script, then commit: unlike the preset sets, the Pokédex only changes with a new version of the app.
