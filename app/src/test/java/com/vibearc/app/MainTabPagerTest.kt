package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class MainTabPagerTest {
    @Test fun leftSwipesFollowFeedStatsPlaylistsOrder() {
        assertEquals(listOf(Tab.Home, Tab.Stats, Tab.Library), MainTabs)
    }
    @Test fun indicatorIsContinuousWhenCurrentPageChangesDuringSwipe() {
        assertEquals(mainTabPosition(0, .5f), mainTabPosition(1, -.5f), .0001f)
        assertEquals(mainTabPosition(1, .5f), mainTabPosition(2, -.5f), .0001f)
    }
    @Test fun reversedAndCancelledDragsReturnToTheirOriginalPosition() {
        assertEquals(.75f, mainTabPosition(1, -.25f), .0001f)
        assertEquals(1f, mainTabPosition(1, 0f), .0001f)
    }
    @Test fun endPagesCannotMoveIndicatorOutsideTheBar() {
        assertEquals(0f, mainTabPosition(0, -.5f), .0001f)
        assertEquals(2f, mainTabPosition(2, .5f), .0001f)
    }
}
