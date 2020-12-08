package org.niksi.rai.controller

class Predictor(val dsl: StrategicDsl) {
    fun predict(metaAction: MetaAction, fieldState: FieldState): Double {
        val state = fieldState.simulate(metaAction)
        return dsl.calculate(state)
    }
}
