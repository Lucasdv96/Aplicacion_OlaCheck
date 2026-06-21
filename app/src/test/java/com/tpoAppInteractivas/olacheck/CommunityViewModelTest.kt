package com.tpoAppInteractivas.olacheck

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tpoAppInteractivas.olacheck.data.local.CommunityPost
import com.tpoAppInteractivas.olacheck.data.local.UserDataStore
import com.tpoAppInteractivas.olacheck.repository.CommunityRepository
import com.tpoAppInteractivas.olacheck.viewmodel.CommunityViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CommunityRepository
    private lateinit var userDataStore: UserDataStore
    private val savedStateHandle = SavedStateHandle(mapOf("beachId" to "1"))

    private val fakeUser = UserDataStore.UserData(
        uid = "user123",
        displayName = "Lucas",
        email = "lucas@test.com",
        photoUrl = ""
    )

    private val fakePosts = listOf(
        CommunityPost(id = "p1", beachId = "1", userId = "user123", text = "Buenas olas hoy!")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        userDataStore = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al iniciar, carga los posts de la playa`() = runTest {
        // simula que Firestore devuelve la lista de posts
        every { repository.getPostsForBeach("1") } returns flowOf(fakePosts)
        every { userDataStore.userData } returns flowOf(fakeUser)

        val viewModel = CommunityViewModel(repository, userDataStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.posts.test {
            val posts = awaitItem()
            assertEquals(1, posts.size)
            assertEquals("Buenas olas hoy!", posts.first().text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no envia post si el texto esta vacio`() = runTest {
        // verifica que sendPost no llama al repositorio con texto vacío
        every { repository.getPostsForBeach("1") } returns flowOf(emptyList())
        every { userDataStore.userData } returns flowOf(fakeUser)

        val viewModel = CommunityViewModel(repository, userDataStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendPost()

        coVerify(exactly = 0) { repository.sendPost(any()) }
    }

    @Test
    fun `envia post correctamente con texto valido`() = runTest {
        // simula envio de un post con contenido
        every { repository.getPostsForBeach("1") } returns flowOf(emptyList())
        every { userDataStore.userData } returns flowOf(fakeUser)
        coEvery { repository.sendPost(any()) } returns Unit

        val viewModel = CommunityViewModel(repository, userDataStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChange("Gran sesión de surf!")
        viewModel.sendPost()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.sendPost(any()) }
    }
}