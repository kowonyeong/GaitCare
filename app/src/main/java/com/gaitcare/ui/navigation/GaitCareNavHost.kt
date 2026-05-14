package com.gaitcare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gaitcare.ui.auth.LoginScreen
import com.gaitcare.ui.auth.SignUpScreen
import com.gaitcare.ui.elder.ElderDetailScreen
import com.gaitcare.ui.facility.FacilityHomeScreen
import com.gaitcare.ui.measurement.MeasurementScreen

@Composable
fun GaitCareNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.Login) {
        composable(NavRoutes.Login) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(NavRoutes.FacilityHome) },
                onSignUpClick = { navController.navigate(NavRoutes.SignUp) }
            )
        }
        composable(NavRoutes.SignUp) {
            SignUpScreen(onSignUpComplete = { navController.popBackStack() })
        }
        composable(NavRoutes.FacilityHome) {
            FacilityHomeScreen(
                onElderClick = { navController.navigate(NavRoutes.elderDetail(it)) }
            )
        }
        composable(
            route = NavRoutes.ElderDetail,
            arguments = listOf(navArgument("elderId") { type = NavType.StringType })
        ) {
            val elderId = it.arguments?.getString("elderId").orEmpty()
            ElderDetailScreen(
                elderId = elderId,
                onStartMeasurement = { navController.navigate(NavRoutes.measurement(elderId)) }
            )
        }
        composable(
            route = NavRoutes.Measurement,
            arguments = listOf(navArgument("elderId") { type = NavType.StringType })
        ) {
            val elderId = it.arguments?.getString("elderId").orEmpty()
            MeasurementScreen(elderId = elderId, onFinish = { navController.popBackStack() })
        }
    }
}
