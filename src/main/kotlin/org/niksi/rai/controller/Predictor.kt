package org.niksi.rai.controller

class Predictor : FeedbackProvider<MetaAction> {
    var previous = DO_NOTHING
    fun predict(metaAction: MetaAction): Double {

        return (if (metaAction == previous) 0.0 else 1.0)
    }

    override fun updateFeedback(choosenAction: MetaAction) {
        previous = choosenAction
    }

}
