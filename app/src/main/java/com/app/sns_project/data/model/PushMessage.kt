package com.app.sns_project.data.model

data class PushMessage(var to: String? = null,
                       var notification: Notification? = Notification()
) {
    data class Notification(
        var body: String? = null,
        var title: String? = null
    )
}