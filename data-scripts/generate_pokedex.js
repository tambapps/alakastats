#!/usr/bin/env node
// Generates the app's Pokédex of a Pokémon Champions format: every legal Pokémon (Megas included, as entries of
// their own), its types, base stats, abilities and movepool, and the moves, abilities, items and types it refers to,
// with their English and French names and descriptions.
//
// Usage: node generate_pokedex.js [path to pokemon-showdown] [path to PokéAPI's data/v2/csv] [format id]
//
// - Pokémon Showdown is the source of the game data, through its own Dex (which applies the format's mod, e.g.
//   data/mods/champions) and team validator (which decides what a Pokémon can learn, a Mega its base form's moves).
//   It must be built first: `node build` in its folder.
// - PokéAPI's CSVs are the source of the French names and descriptions, Showdown's French translations and
//   pokedex-fr-overrides.json completing them.
//
// The paths default to siblings of the alakastats checkout (../../pokemon-showdown, ../../pokeapi/data/v2/csv), the
// format to the current VGC regulation. See SCRIPTS.md.
'use strict';
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const SCHEMA_VERSION = 3;
const WORKSPACE = path.join(__dirname, '..', '..');
const SHOWDOWN_DIR = path.resolve(process.argv[2] || path.join(WORKSPACE, 'pokemon-showdown'));
const POKEAPI_CSV_DIR = path.resolve(process.argv[3] || path.join(WORKSPACE, 'pokeapi', 'data', 'v2', 'csv'));
const FORMAT_ID = process.argv[4] || 'gen9championsvgc2026regmc';
const OUTPUT_FILE = path.join(__dirname, '..', 'composeApp', 'src', 'commonMain', 'composeResources', 'files', 'pokedex', 'champions.json');
const OVERRIDES_FILE = path.join(__dirname, 'pokedex-fr-overrides.json');
// PokéAPI's id of French
const FRENCH = '5';
// the version group of Pokémon Champions in PokéAPI, whose descriptions are the accurate ones for the format
const CHAMPIONS_VERSION_GROUP = 'champions';

const problems = [];

// --- Showdown ---
const simPath = path.join(SHOWDOWN_DIR, 'dist', 'sim');
if (!fs.existsSync(simPath)) {
  console.error(`${simPath} doesn't exist: run \`node build\` in ${SHOWDOWN_DIR} first`);
  process.exit(1);
}
const { Dex, TeamValidator, toID } = require(simPath);
const format = Dex.formats.get(FORMAT_ID);
if (!format.exists) {
  console.error(`Unknown format ${FORMAT_ID}`);
  process.exit(1);
}
const dex = Dex.forFormat(format);
const ruleTable = dex.formats.getRuleTable(format);
const validator = TeamValidator.get(format);
const textEn = dex.loadTextData('en');
// Showdown's French translations as they are: loadTextData('fr') would complete the missing ones (null) with English
const textFr = Object.fromEntries([
  ['Pokedex', 'pokedex', 'PokedexText'],
  ['Moves', 'moves', 'MovesText'],
  ['Abilities', 'abilities', 'AbilitiesText'],
  ['Items', 'items', 'ItemsText'],
].map(([table, file, exported]) => [table, require(path.join(SHOWDOWN_DIR, 'dist', 'data', 'text', 'fr', file))[exported] ?? {}]));

// --- PokéAPI ---
function readCsv(name) {
  const text = fs.readFileSync(path.join(POKEAPI_CSV_DIR, `${name}.csv`), 'utf8');
  const rows = [];
  let row = [], field = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++; }
      else if (c === '"') quoted = false;
      else field += c;
    } else if (c === '"') quoted = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n') { row.push(field); rows.push(row); row = []; field = ''; }
    else if (c !== '\r') field += c;
  }
  if (field || row.length) { row.push(field); rows.push(row); }
  const [header, ...body] = rows;
  return body.filter(r => r.length === header.length).map(r => Object.fromEntries(header.map((h, i) => [h, r[i]])));
}

const versionGroupOrder = new Map();
let championsVersionGroupId = null;
for (const vg of readCsv('version_groups')) {
  versionGroupOrder.set(vg.id, Number(vg.order));
  if (vg.identifier === CHAMPIONS_VERSION_GROUP) championsVersionGroupId = vg.id;
}

function cleanFlavorText(s) {
  return s.replace(/­/g, '').replace(/[\n\f\r]+/g, ' ').replace(/\s+/g, ' ').trim();
}

/** A PokéAPI table ("moves", "abilities", "items", "types"): its French names and descriptions by Showdown id */
function pokeApiTable(entity, nameTable, idColumn, flavorTable) {
  const idById = new Map(readCsv(entity).map(r => [r.id, toID(r.identifier)]));
  const names = new Map();
  for (const r of readCsv(nameTable)) {
    if (r.local_language_id === FRENCH && r.name) names.set(idById.get(r[idColumn]), r.name);
  }
  // the latest French description, with the version group it comes from
  const descs = new Map();
  if (flavorTable) {
    for (const r of readCsv(flavorTable)) {
      const lang = r.language_id ?? r.local_language_id;
      if (lang !== FRENCH || !r.flavor_text) continue;
      const id = idById.get(r[idColumn]);
      const order = versionGroupOrder.get(r.version_group_id) ?? 0;
      const current = descs.get(id);
      if (!current || order > current.order) {
        descs.set(id, { text: cleanFlavorText(r.flavor_text), order, isChampions: r.version_group_id === championsVersionGroupId });
      }
    }
  }
  return { names, descs };
}

const pokeApiMoves = pokeApiTable('moves', 'move_names', 'move_id', 'move_flavor_text');
const pokeApiAbilities = pokeApiTable('abilities', 'ability_names', 'ability_id', 'ability_flavor_text');
const pokeApiItems = pokeApiTable('items', 'item_names', 'item_id', 'item_flavor_text');
const pokeApiTypes = pokeApiTable('types', 'type_names', 'type_id', null);

const overrides = fs.existsSync(OVERRIDES_FILE) ? JSON.parse(fs.readFileSync(OVERRIDES_FILE, 'utf8')) : {};

// the French names of the Pokémon: of a species for a base form (e.g. "Prismillon", not the name of its default
// pattern), of the form for the others (e.g. "Méga-Dracaufeu Y", "Arcanin de Hisui")
const pokeApiPokemonNames = (() => {
  const speciesIds = new Map(readCsv('pokemon_species').map(r => [r.id, toID(r.identifier)]));
  const names = new Map();
  const speciesNames = new Map();
  for (const r of readCsv('pokemon_species_names')) {
    if (r.local_language_id === FRENCH && r.name) speciesNames.set(speciesIds.get(r.pokemon_species_id), r.name);
  }
  const formNames = new Map();
  for (const r of readCsv('pokemon_form_names')) {
    if (r.local_language_id === FRENCH && r.pokemon_name) formNames.set(r.pokemon_form_id, r.pokemon_name);
  }
  const pokemonIdentifiers = new Map(readCsv('pokemon').map(r => [r.id, r.identifier]));
  const formIdentifierNames = new Map();
  for (const r of readCsv('pokemon_forms')) {
    if (!formNames.has(r.id)) continue;
    formIdentifierNames.set(toID(r.identifier), formNames.get(r.id));
    if (r.is_default === '1') formIdentifierNames.set(toID(pokemonIdentifiers.get(r.pokemon_id)), formNames.get(r.id));
  }
  // the PokéAPI identifiers of the forms Showdown names otherwise (e.g. tauros-paldea-combat-breed)
  const pokeApiIds = overrides.pokeApiIds ?? {};
  return {
    get(species) {
      if (!species.forme) return speciesNames.get(species.id);
      return formIdentifierNames.get(toID(pokeApiIds[species.id] ?? species.id));
    },
  };
})();

/**
 * The French name of a Showdown entry: the override, else PokéAPI's, else Showdown's translation. A missing name is a
 * problem making the script fail, unless [fallback] gives one (e.g. built from the base form's for a Mega).
 */
function frenchName(table, id, englishName, pokeApiName, showdownTable, fallback) {
  const name = overrides[table]?.[id] ?? pokeApiName ?? textFr[showdownTable]?.[id]?.name ?? fallback?.();
  if (!name) problems.push(`no French name for ${table} ${englishName} (${id}): add it to ${path.basename(OVERRIDES_FILE)}`);
  return name ?? englishName;
}

/** Showdown's English descriptions of an entry, Champions' own ones replacing the base ones */
function englishDescs(showdownTable, id) {
  const text = textEn[showdownTable]?.[id] ?? {};
  const champions = text.champions ?? {};
  return { desc: champions.desc ?? text.desc ?? null, shortDesc: champions.shortDesc ?? text.shortDesc ?? null };
}

/**
 * The French description of an entry, null if it would be the description of the base game's version of an entry
 * Champions modified
 */
function frenchDesc(table, id, pokeApi, isModified) {
  const override = overrides[`${table}Descriptions`]?.[id];
  if (override) return override;
  const desc = pokeApi.descs.get(id);
  if (!desc || (isModified && !desc.isChampions)) return null;
  return desc.text;
}

// whether the format's mod changed what an entry of the base dex does: its legality (isNonstandard) and PP (every move
// above 20 PP is capped to 20 in Champions) left aside
const IGNORED_CHANGES = new Set(['isNonstandard', 'pp']);
function stringifyData(data) {
  if (!data) return 'undefined';
  return JSON.stringify(data, (key, value) => (IGNORED_CHANGES.has(key) ? undefined : typeof value === 'function' ? value.toString() : value));
}
function isModified(table, id) {
  return stringifyData(dex.data[table][id]) !== stringifyData(Dex.data[table][id]);
}

// --- the legal Pokémon ---
const legalMoves = dex.moves.all().filter(m => m.exists && !m.isNonstandard);
const isLegalSpecies = s => s.exists && !s.isNonstandard && s.tier !== 'Illegal' && !ruleTable.isBannedSpecies(s);
const cosmeticFormsOf = new Map();
const legalSpecies = [];
for (const s of dex.species.all()) {
  if (!isLegalSpecies(s)) continue;
  // forms only taken in battle (e.g. Aegislash-Blade, Palafin-Hero), Megas aside
  if (s.battleOnly && !s.isMega) continue;
  // cosmetic forms are the same Pokémon as their base form: the ones Showdown says so (e.g. Vivillon-Polar), and the
  // ones only differing by their look (e.g. Vivillon-Fancy, Polteageist-Antique)
  const base = dex.species.get(s.baseSpecies);
  if (s.forme && !s.isMega && (base.cosmeticFormes?.includes(s.name) || (isLegalSpecies(base) && playsLike(s, base)))) {
    if (!cosmeticFormsOf.has(base.id)) cosmeticFormsOf.set(base.id, []);
    cosmeticFormsOf.get(base.id).push(s.name);
    continue;
  }
  legalSpecies.push(s);
}

// the moves a Pokémon can learn in the format, a Mega the ones of its base form
function movesOf(species) {
  return legalMoves.filter(m => !validator.checkCanLearn(m, species)).map(m => m.id).sort();
}

function playsLike(form, base) {
  return form.types.join() === base.types.join()
    && JSON.stringify(form.baseStats) === JSON.stringify(base.baseStats)
    && JSON.stringify(form.abilities) === JSON.stringify(base.abilities)
    && movesOf(form).join() === movesOf(base).join();
}
legalSpecies.sort((a, b) => a.num - b.num);
const legalSpeciesIds = new Set(legalSpecies.map(s => s.id));

const TYPE_ID = t => t.toUpperCase();
const MOVE_CATEGORY = { Physical: 'PHYSICAL', Special: 'SPECIAL', Status: 'STATUS' };

const speciesEntries = legalSpecies.map(s => {
  const megaOf = s.isMega ? dex.species.get(s.changesFrom ?? s.baseSpecies) : null;
  const moves = movesOf(s);
  if (!moves.length) problems.push(`${s.name} learns no move`);
  const abilities = [s.abilities['0'], s.abilities['1']].filter(Boolean).map(toID);
  const nameFr = frenchName('species', s.id, s.name, pokeApiPokemonNames.get(s), 'Pokedex', megaOf && (() => {
    // a Mega PokéAPI doesn't have yet: "Méga-" and its base form's name, then its X/Y/Z
    const baseName = overrides.species?.[megaOf.id] ?? pokeApiPokemonNames.get(megaOf);
    const suffix = s.forme.match(/^Mega-([XYZ])$/)?.[1];
    return baseName && `Méga-${baseName}${suffix ? ` ${suffix}` : ''}`;
  }));
  return {
    id: s.id,
    name: s.name,
    nameFr,
    num: s.num,
    types: s.types.map(TYPE_ID),
    baseStats: { ...s.baseStats },
    abilities,
    hiddenAbility: s.abilities.H ? toID(s.abilities.H) : null,
    // the species of Showdown this one is a form of (e.g. tauros for Tauros-Paldea-Aqua), null for a base form
    baseSpecies: s.forme && legalSpeciesIds.has(toID(s.baseSpecies)) ? toID(s.baseSpecies) : null,
    isMega: !!s.isMega,
    // the form a Mega mega evolves from, and the Mega Stone it needs
    megaOf: megaOf ? megaOf.id : null,
    megaStone: s.isMega && s.requiredItem ? toID(s.requiredItem) : null,
    weightKg: s.weightkg,
    cosmeticForms: cosmeticFormsOf.get(s.id) ?? [],
    moves,
  };
});

// --- what the Pokémon refer to ---
const usedAbilityIds = new Set(speciesEntries.flatMap(s => [...s.abilities, s.hiddenAbility].filter(Boolean)));

// --- the classifications of the moves: the flags of Showdown, and the moves an ability blocks for the classifications
// without flag ---
const FLAG_CLASSIFICATIONS = {
  punch: 'PUNCHING', sound: 'SOUND', dance: 'DANCE', slicing: 'SLICING', wind: 'WIND', powder: 'POWDER',
  bullet: 'BALL_AND_BOMB', pulse: 'PULSE', bite: 'BITING', heal: 'HEALING',
};
// the ids of moves an ability's handler lists, e.g. ["explosion", "mindblown", ...] in Damp's
function movesListedBy(abilityId, handler) {
  const source = String(dex.abilities.get(abilityId)[handler] ?? '');
  const list = source.match(/\[([^\]]*)\]\.includes/);
  const ids = list ? [...list[1].matchAll(/["']([a-z0-9]+)["']/g)].map(match => match[1]) : [];
  if (!ids.length) problems.push(`no move found in ${abilityId}.${handler}: its source changed`);
  return new Set(ids);
}
// the moves Damp prevents, and the ones Aroma Veil protects from
const EXPLOSIVE_MOVES = movesListedBy('damp', 'onAnyTryMove');
const MENTAL_MOVES = movesListedBy('aromaveil', 'onAllyTryAddVolatile');
function classificationsOf(move) {
  const classifications = Object.keys(move.flags).map(flag => FLAG_CLASSIFICATIONS[flag]).filter(Boolean);
  if (EXPLOSIVE_MOVES.has(move.id)) classifications.push('EXPLOSIVE');
  if (MENTAL_MOVES.has(move.id)) classifications.push('MENTAL');
  return classifications.sort();
}

const moveEntries = legalMoves.sort((a, b) => a.name.localeCompare(b.name)).map(m => {
  const modified = isModified('Moves', m.id);
  return {
    id: m.id,
    name: m.name,
    nameFr: frenchName('moves', m.id, m.name, pokeApiMoves.names.get(m.id), 'Moves'),
    type: TYPE_ID(m.type),
    category: MOVE_CATEGORY[m.category],
    basePower: m.basePower,
    // null for a move that can't miss
    accuracy: m.accuracy === true ? null : m.accuracy,
    // the PP in battle, the format's mod computing them its way (Champions: 5 gives 8, 10 gives 12, 15 gives 16)
    pp: dex.data.Scripts.calculatePP ? dex.data.Scripts.calculatePP(m, 3) : m.pp,
    priority: m.priority,
    target: m.target,
    makesContact: !!m.flags.contact,
    classifications: classificationsOf(m),
    championsModified: modified,
    ...englishDescs('Moves', m.id),
    descFr: frenchDesc('moves', m.id, pokeApiMoves, modified),
  };
});

const abilityEntries = dex.abilities.all()
  .filter(a => a.exists && !a.isNonstandard && usedAbilityIds.has(a.id))
  .sort((a, b) => a.name.localeCompare(b.name))
  .map(a => {
    const modified = isModified('Abilities', a.id);
    return {
      id: a.id,
      name: a.name,
      nameFr: frenchName('abilities', a.id, a.name, pokeApiAbilities.names.get(a.id), 'Abilities'),
      championsModified: modified,
      ...englishDescs('Abilities', a.id),
      descFr: frenchDesc('abilities', a.id, pokeApiAbilities, modified),
    };
  });
for (const id of usedAbilityIds) {
  if (!abilityEntries.some(a => a.id === id)) problems.push(`unknown ability ${id}`);
}

const itemEntries = dex.items.all()
  .filter(i => i.exists && !i.isNonstandard)
  .sort((a, b) => a.name.localeCompare(b.name))
  .map(i => {
    const modified = isModified('Items', i.id);
    const megaForms = i.megaStone ? Object.values(i.megaStone).map(toID).filter(id => legalSpeciesIds.has(id)) : [];
    return {
      id: i.id,
      name: i.name,
      nameFr: frenchName('items', i.id, i.name, pokeApiItems.names.get(i.id), 'Items'),
      // the Megas it makes mega evolve
      megaForms,
      championsModified: modified,
      ...englishDescs('Items', i.id),
      descFr: frenchDesc('items', i.id, pokeApiItems, modified),
    };
  });

const typeEntries = dex.types.all()
  .filter(t => t.exists && !t.isNonstandard && t.name !== 'Stellar')
  .map(t => ({ id: TYPE_ID(t.name), name: t.name, nameFr: frenchName('types', t.id, t.name, pokeApiTypes.names.get(t.id), null) }));

// --- checks ---
for (const s of speciesEntries) {
  if (s.megaOf) {
    const base = speciesEntries.find(b => b.id === s.megaOf);
    if (!base) problems.push(`${s.name} mega evolves from ${s.megaOf}, which isn't legal`);
    else if (base.moves.join() !== s.moves.join()) problems.push(`${s.name} doesn't learn the moves of ${base.name}`);
    if (s.megaStone && !itemEntries.some(i => i.id === s.megaStone)) problems.push(`${s.name}'s Mega Stone ${s.megaStone} isn't legal`);
  }
}

if (problems.length) {
  console.error(`${problems.length} problem(s), nothing written:\n- ${problems.join('\n- ')}`);
  process.exit(1);
}

// --- the ids of the app: Showdown's names normalized like PokemonNormalizer.normalize does (lowercase, a space giving
// a dash), e.g. charizard-mega-y, trick-room, king's-rock. Up to here, the entries and their references had
// Showdown's ids ---
const normalize = name => name.toLowerCase().replace(/ /g, '-');
function appIds(entries) {
  const ids = new Map(entries.map(e => [e.id, normalize(e.name)]));
  if (new Set(ids.values()).size !== ids.size) throw new Error('two entries have the same normalized name');
  return id => {
    if (!ids.has(id)) throw new Error(`no entry of id ${id}`);
    return ids.get(id);
  };
}
const speciesId = appIds(speciesEntries);
const moveId = appIds(moveEntries);
const abilityId = appIds(abilityEntries);
const itemId = appIds(itemEntries);
const nullable = (id, toAppId) => (id == null ? null : toAppId(id));
for (const s of speciesEntries) {
  s.abilities = s.abilities.map(abilityId);
  s.hiddenAbility = nullable(s.hiddenAbility, abilityId);
  s.baseSpecies = nullable(s.baseSpecies, speciesId);
  s.megaOf = nullable(s.megaOf, speciesId);
  s.megaStone = nullable(s.megaStone, itemId);
  s.moves = s.moves.map(moveId).sort();
}
for (const i of itemEntries) i.megaForms = i.megaForms.map(speciesId);
for (const [entries, toAppId] of [[speciesEntries, speciesId], [moveEntries, moveId], [abilityEntries, abilityId], [itemEntries, itemId]]) {
  for (const e of entries) e.id = toAppId(e.id);
}

// --- output: one line per entry, for readable diffs ---
function gitCommit(dir) {
  try {
    return execFileSync('git', ['-C', dir, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim();
  } catch {
    return null;
  }
}
const meta = {
  schemaVersion: SCHEMA_VERSION,
  formatId: format.id,
  formatName: format.name,
  showdownCommit: gitCommit(SHOWDOWN_DIR),
  pokeApiCommit: gitCommit(POKEAPI_CSV_DIR),
};
const table = entries => `[\n${entries.map(e => `    ${JSON.stringify(e)}`).join(',\n')}\n  ]`;
const output = `{
  "meta": ${JSON.stringify(meta)},
  "types": ${table(typeEntries)},
  "abilities": ${table(abilityEntries)},
  "moves": ${table(moveEntries)},
  "items": ${table(itemEntries)},
  "species": ${table(speciesEntries)}
}
`;
fs.mkdirSync(path.dirname(OUTPUT_FILE), { recursive: true });
fs.writeFileSync(OUTPUT_FILE, output);
console.log(`${speciesEntries.length} Pokémon (${speciesEntries.filter(s => s.isMega).length} Megas), ${moveEntries.length} moves, `
  + `${abilityEntries.length} abilities, ${itemEntries.length} items of ${format.name} written to ${path.relative(process.cwd(), OUTPUT_FILE)}`
  + ` (${Math.round(output.length / 1024)} KB)`);
