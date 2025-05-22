package domain.model

import java.time.Instant
import java.time.LocalDate

data class Reward(
    val id: String,
    val name: String?,
    val description: String?,
    val amount: Money,
    val backersCount: Int,
    val estimatedDeliveryDate: LocalDate?,
    val isLimited: Boolean,
    val remainingQuantity: Int?,
    val limit: Int?,
    val hasShipping: Boolean,
    val shippingCountriesCount: Int,
    val isEarlyBird: Boolean,
    val startsAt: Instant?,
    val endsAt: Instant?
)