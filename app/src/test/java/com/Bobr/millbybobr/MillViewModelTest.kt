package com.Bobr.millbybobr

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.Bobr.millbybobr.domain.engine.BotEngine
import com.Bobr.millbybobr.domain.engine.MillGameEngine
import com.Bobr.millbybobr.ui.viewmodels.MillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule

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