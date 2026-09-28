package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

/**
 * Port of the source calculator's setField (ap_calc.js) for one field type (weather or terrain): abilities
 * setting it on the field (e.g. Drought -> Sun), remembering the value set manually to restore it once no
 * ability sets it anymore. Sides are indexed 0 (attacker) and 1 (defender).
 *
 * @param none the value meaning no weather/terrain
 */
internal class AbilityFieldSync<T : Any>(private val none: T) {
    // the value last set manually by the user
    private var lastManual: T = none
    // the value set by each side's ability, null if none
    private val lastAuto = mutableListOf<T?>(null, null)

    /**
     * The field's new value when [side]'s ability changes (or its toggle), the ability setting [abilityValue]
     * (null if it sets none) while the field currently is [current]
     */
    fun onAbilityChange(current: T, side: Int, abilityValue: T?): T {
        if (current !in lastAuto || current == none) {
            // the current value wasn't set by an ability, so it was set manually
            lastManual = current
            lastAuto[1 - side] = null
        }
        return if (abilityValue != null) {
            lastAuto[side] = abilityValue
            abilityValue
        } else {
            lastAuto[side] = null
            lastAuto[1 - side] ?: lastManual
        }
    }

    /**
     * Swap the attacker and defender sides, so that what each side's ability set follows its pokemon
     */
    fun swapSides() {
        lastAuto.reverse()
    }
}
