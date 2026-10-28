package com.dafu.auction.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.presentation.screens.MainScreen
import com.dafu.auction.presentation.theme.DafuAuctionTheme
import com.dafu.auction.presentation.viewmodel.AuctionViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: AuctionViewModel = koinViewModel()
            val state: AuctionState by viewModel.state.collectAsStateWithLifecycle()

            DafuAuctionTheme(theme = state.theme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(state = state, viewModel = viewModel)
                }
            }
        }
    }
}
