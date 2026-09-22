package com.jxguo92.mykarooextension.core.karoo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface KarooFlows {
    fun streamStates(typeId: String): Flow<StreamState>
    fun userProfile(): Flow<UserProfile>
    fun rideState(): Flow<RideState>
}

class KarooFlowAdapter(private val system: KarooSystemService) : KarooFlows {
    override fun streamStates(typeId: String): Flow<StreamState> = callbackFlow {
        val id = system.addConsumer<OnStreamState>(
            params = OnStreamState.StartStreaming(typeId),
            onError = { trySend(StreamState.NotAvailable) },
            onComplete = { trySend(StreamState.NotAvailable) },
            onEvent = { trySend(it.state) },
        )
        awaitClose { system.removeConsumer(id) }
    }

    override fun userProfile(): Flow<UserProfile> = callbackFlow {
        val id = system.addConsumer<UserProfile>(
            params = UserProfile.Params,
            onError = { close() },
            onComplete = { close() },
            onEvent = { trySend(it) },
        )
        awaitClose { system.removeConsumer(id) }
    }

    override fun rideState(): Flow<RideState> = callbackFlow {
        val id = system.addConsumer<RideState>(
            params = RideState.Params,
            onError = { close() },
            onComplete = { close() },
            onEvent = { trySend(it) },
        )
        awaitClose { system.removeConsumer(id) }
    }
}

fun StreamState.fieldValue(field: String): Double? =
    (this as? StreamState.Streaming)?.dataPoint?.values?.get(field)?.takeIf { it.isFinite() }

fun StreamState.rearCogReading(): RearCogTeethReading? =
    (this as? StreamState.Streaming)?.dataPoint?.values?.let {
        RearCogTeethCalculator.resolve(
            it[DataType.Field.SHIFTING_REAR_GEAR_TEETH],
            it[DataType.Field.SHIFTING_REAR_GEAR],
            it[DataType.Field.SHIFTING_REAR_GEAR_MAX],
        )
    }
