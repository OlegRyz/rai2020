package org.niksi.rai.controller

import kotlin.random.Random

class StrategicContext(val state: ImaginaryState) {
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
            state.fieldState.canceldOrder(metaAction)
        }
        return this
    }


    val rulesMap = mutableMapOf<MetaAction, (FieldState) -> Unit>()
    fun MetaAction.rule(name: String, function: (FieldState) -> Unit) {
        rulesMap[this] = function
    }
}

class StrategicDsl(val definititon: StrategicContext.() -> Unit) {

    fun calculate(state: ImaginaryState): Double = StrategicContext(state).run {
        definititon()
        rulesMap[state.metaAction]?.invoke(state.fieldState)
        return reward
    }
}
