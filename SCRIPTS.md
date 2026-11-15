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
