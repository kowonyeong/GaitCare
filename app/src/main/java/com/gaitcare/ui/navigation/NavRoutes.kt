package com.gaitcare.ui.navigation

object NavRoutes {
    const val Login = "login"
    const val SignUp = "signup"
    const val FacilityHome = "facility_home"
    const val ElderDetail = "elder_detail/{elderId}"
    const val Measurement = "measurement/{elderId}"

    fun elderDetail(elderId: String) = "elder_detail/$elderId"
    fun measurement(elderId: String) = "measurement/$elderId"
}
