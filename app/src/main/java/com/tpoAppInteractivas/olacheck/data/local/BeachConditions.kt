package com.tpoAppInteractivas.olacheck.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "beach_conditions",
    foreignKeys = [ForeignKey(
        entity = Beach::class,
        parentColumns = ["id"],
        childColumns = ["beachId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BeachConditions(
    @PrimaryKey val beachId: String,
    val waterTemp: Float = 0f,
    val airTemp: Float = 0f,
    val windSpeed: Float = 0f,
    val windDirection: Float = 0f,
    val waveHeight: Float = 0f,
    val wavePeriod: Float = 0f,
    val tideStatus: String = "",
    val humidity: Float = 0f,
    val fetchedAt: Long = 0L
)


