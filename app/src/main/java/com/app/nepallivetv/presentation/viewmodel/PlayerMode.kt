package com.app.nepallivetv.presentation.viewmodel

/**
 * The three on-screen player sizes the user can move between in portrait.
 *
 * - [MINI]     a 56dp persistent bar above the bottom nav. Default after a channel is selected.
 * - [EXPANDED] roughly half-screen, video sits at 16:9 above the channel list. Reached
 *              by tapping the expand control on the mini bar.
 * - [FULL]     edge-to-edge. Reached either by tapping fullscreen from EXPANDED or by
 *              rotating the device to landscape.
 *
 * Landscape orientation is always rendered as FULL regardless of stored mode — see
 * the `isLandscape` check in HomeScreen / MyListScreen.
 */
enum class PlayerMode { MINI, EXPANDED, FULL }
