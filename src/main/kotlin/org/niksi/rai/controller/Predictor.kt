package org.niksi.rai.controller

class Predictor(val dsl: StrategicDsl) : FeedbackProvider<MetaAction> {
    var previous = DO_NOTHING
    fun predict(metaAction: MetaAction, fieldState: FieldState): Double {
        val state = fieldState.simulate(metaAction)
        return dsl.calculate(state)
    }

    override fun updateFeedback(choosenAction: MetaAction) {
        previous = choosenAction
    }

}
