package com.brujuladelezo.core

sealed interface AppError {
    data object SinSensorBrujula : AppError
    data object SinPermisoUbicacion : AppError
    data object UbicacionDesactivada : AppError
}
