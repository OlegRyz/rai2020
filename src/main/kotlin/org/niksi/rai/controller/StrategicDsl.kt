package org.niksi.rai.controller

import kotlin.random.Random

class RulesContext(val state: FieldState) {
    var reward = Random.nextDouble()
    fun Boolean.isRandom() {
        if (this) {
            reward = Random.nextDouble()
        }
    }

    fun Boolean.isBad() {
        if (this) {
            reward = 0.1 * Random.nextDouble()
        }
    }

    fun Boolean.isGood() {
        if (this) {
            reward = 0.2 * Random.nextDouble() + 0.7
        }
    }

    fun Boolean.isAlwaysNeeded() {
        if (this) {
            reward = 0.15 * Random.nextDouble() + 0.85
        }
    }


    fun Boolean.isNotAcceptable() {
        if (this) {
            reward = -1.0
        }
    }


    fun Boolean.alsoCancelOrder(metaAction: MetaAction): Boolean {
        if (this) {
            state.canceldOrder(metaAction)
        }
        return this
    }
}

class StrategicContext(val state: ImaginaryState) {
    private val pairedRulesList =
        mutableListOf<Pair<Pair<MetaAction, MetaAction>, RulesContext.(FieldState) -> Boolean>>()
    private val rulesList = mutableListOf<Pair<MetaAction, RulesContext.(FieldState) -> Unit>>()
    fun MetaAction.rule(name: String, function: RulesContext.(FieldState) -> Unit) {
        rulesList.add(this to function)
    }

    fun Pair<MetaAction, MetaAction>.pairedRule(name: String, function: RulesContext.(FieldState) -> Boolean) {
        pairedRulesList.add(this to function)
    }

    fun calculatePairedRulesReward(metaAction: MetaAction, fieldState: FieldState) = rulesList
        .filter { it.first == metaAction }
        .map { (_, decoder) ->
            RulesContext(fieldState).run {
                decoder(this, fieldState)
                reward
            }
        }
        .maxByOrNull { it } ?: 0.0

    fun calculateRulesReward(metaAction: MetaAction, fieldState: FieldState) = pairedRulesList
        .filter { (pair, _) -> pair.first == metaAction || pair.second == metaAction }
        .map { (pair, decoder) ->
            RulesContext(fieldState).run {
                val condition = decoder(this, fieldState)
                if (metaAction == pair.first && condition ||
                    metaAction == pair.second && !condition) {
                    reward
                } else {
                    0.0
                }
            }
        }
        .maxByOrNull { it } ?: 0.0
}

class StrategicDsl(val definititon: StrategicContext.() -> Unit) {

    fun calculate(state: ImaginaryState): Double = StrategicContext(state).run {
        definititon()
        val rullesReward = this.calculateRulesReward(state.metaAction, state.fieldState)
        val pairedRulesReward = this.calculatePairedRulesReward(state.metaAction, state.fieldState)
        return maxOf(rullesReward, pairedRulesReward)
    }
}