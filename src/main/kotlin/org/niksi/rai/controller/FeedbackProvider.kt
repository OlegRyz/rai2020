package org.niksi.rai.controller

fun <T> T.feedbackTo(feedbackProvider: FeedbackProvider<T>): T {
    feedbackProvider.updateFeedback(this)
    return this
}

interface FeedbackProvider<T> {
    fun updateFeedback(feedback: T)
}