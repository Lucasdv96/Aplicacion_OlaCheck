package com.tpoAppInteractivas.olacheck

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.repository.BeachDetailRepository
import com.tpoAppInteractivas.olacheck.ui.screens.UiState
import com.tpoAppInteractivas.olacheck.viewmodel.BeachDetailViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BeachDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BeachDetailRepository
    private val savedStateHandle = SavedStateHandle(mapOf("beachId" to "1"))

    private val fakeBeach = Beach(
        id = "1",
        name = "Playa Test",
        latitude = -38.0,
        longitude = -57.5,
        imageUrl = null,
        lastUpdated = System.currentTimeMillis()
    )

    private val fakeConditions = BeachConditions(
        beachId = "1",
        waterTemp = 18f,
        airTemp = 20f,
        windSpeed = 10f,
        windDirection = 90f,
        waveHeight = 1.2f,
        wavePeriod = 8f,
        humidity = 70f,
        fetchedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando el refresh es exitoso, muestra Success con datos frescos`() = runTest {
        // simula refresh exitoso y datos disponibles en Room
        coEvery { repository.getBeachById("1") } returns fakeBeach
        coEvery { repository.getConditionsForBeach("1") } returns fakeConditions
        coEvery { repository.refreshConditions("1") } returns Unit

        val viewModel = BeachDetailViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando el refresh falla pero hay cache, muestra Success con datos guardados`() = runTest {
        // simula sin internet pero con datos cacheados en Room
        coEvery { repository.getBeachById("1") } returns fakeBeach
        coEvery { repository.getConditionsForBeach("1") } returns fakeConditions
        coEvery { repository.refreshConditions("1") } throws Exception("Sin conexión")

        val viewModel = BeachDetailViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando el refresh falla y no hay cache, muestra Offline`() = runTest {
        // simula sin internet y sin datos guardados
        coEvery { repository.getBeachById("1") } returns null
        coEvery { repository.getConditionsForBeach("1") } returns null
        coEvery { repository.refreshConditions("1") } throws Exception("Sin conexión")

        val viewModel = BeachDetailViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Offline)
            cancelAndIgnoreRemainingEvents()
        }
    }
}