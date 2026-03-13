package com.Bobr.mill

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.Bobr.mill.domain.engine.BotEngine
import com.Bobr.mill.domain.engine.MillGameEngine
import com.Bobr.mill.ui.viewmodels.MillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.collections.get

class MillViewModelTest {

    // Hilft dabei, dass LiveData/Flow sofort auf dem Test-Thread ausgeführt wird
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Setzt den Dispatcher.Main auf einen Test-Dispatcher um
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MillViewModel
    private lateinit var gameEngine: MillGameEngine
    private lateinit var botEngine: BotEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gameEngine = MillGameEngine()
        botEngine = BotEngine()
        viewModel = MillViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

}