package com.example.model

enum class SosWorkflowState {
    IDLE,
    CHECKING_PERMISSIONS,
    ENABLING_GPS,
    OBTAINING_LOCATION,
    SENDING_SMS,
    CALLING_CONTACT,
    UPLOADING,
    COMPLETED
}
