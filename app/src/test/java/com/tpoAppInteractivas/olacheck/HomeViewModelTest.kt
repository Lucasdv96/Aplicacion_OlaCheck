package com.tpoAppInteractivas.olacheck

import app.cash.turbine.test
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.repository.BeachListRepository
import com.tpoAppInteractivas.olacheck.ui.screens.UiState
import com.tpoAppInteractivas.olacheck.viewmodel.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Dispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest{

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BeachListRepository
    private lateinit var viewModel: HomeViewModel

    // playa de prueba reutilizable
    private val fakeBeach = Beach(
        id =  "1",
        name =  "Playa test",
        latitude =  -38.0,
        longitude = -54.5,
        imageUrl = null,
        lastUpdated = System.currentTimeMillis()
    )

    // condiciones de prueba reutilizables
    private val fakeConditions = BeachConditions(
        beachId = "1",
        waterTemp = 18.0f,
        airTemp = 20.0f,
        windSpeed = 10.0f,
        windDirection = 90.0f,
        waveHeight = 1.2f,
        wavePeriod = 8f,
        humidity = 70.0f,
        fetchedAt = System.currentTimeMillis()
    )

    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando hay conexion y datos, muestra Success`() = runTest {
        // configura el mock para simular conexion y datos disponibles
        every { repository.isOnline() } returns true
        coEvery { repository.refreshBeachData() } returns Unit
        every { repository.getBeaches() } returns flowOf(listOf(fakeBeach))
        every { repository.getAllConditions() } returns flowOf(listOf(fakeConditions))

        viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando no hay conexion pero hay cache, muestra Success`() = runTest {
        // simula sin internet pero con datos guardados en Room
        every { repository.isOnline() } returns false
        every { repository.getBeaches() } returns flowOf(listOf(fakeBeach))
        every { repository.getAllConditions() } returns flowOf(listOf(fakeConditions))

        viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando no hay conexion ni cache, muestra Offline`() = runTest {
        // simula sin internet y sin datos en Room
        every { repository.isOnline() } returns false
        every { repository.getBeaches() } returns flowOf(emptyList())
        every { repository.getAllConditions() } returns flowOf(emptyList())

        viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Offline)
            cancelAndIgnoreRemainingEvents()
        }
    }

}