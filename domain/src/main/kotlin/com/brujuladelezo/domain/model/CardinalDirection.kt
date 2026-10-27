package com.brujuladelezo.domain.model

/** Rosa de 16 rumbos, con las abreviaturas en español (O de oeste). */
enum class CardinalDirection(val abbreviation: String) {
    N("N"), NNE("NNE"), NE("NE"), ENE("ENE"),
    E("E"), ESE("ESE"), SE("SE"), SSE("SSE"),
    S("S"), SSO("SSO"), SO("SO"), OSO("OSO"),
    O("O"), ONO("ONO"), NO("NO"), NNO("NNO");

    companion object {
        /** Rumbo en grados (cualquier valor; se normaliza) → punto de la rosa más cercano. */
        fun fromBearing(degrees: Double): CardinalDirection {
            val normalized = ((degrees % 360.0) + 360.0) % 360.0
            val index = ((normalized + 11.25) / 22.5).toInt() % 16
            return entries[index]
        }
    }
}
