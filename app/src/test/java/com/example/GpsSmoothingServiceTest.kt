package com.example

import com.example.domain.GpsSmoothingService
import com.example.domain.PaceTriggerService
import com.example.domain.PaceZone
import com.example.domain.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsSmoothingServiceTest {

    @Test
    fun rejectsWeakAccuracyAndTeleportationJumps() {
        val smoother = GpsSmoothingService()
        val t0 = 1_700_000_000_000L

        // Weak multipath fix (> 30m) should be dropped
        val weak = smoother.processLivePoint(
            TrackPoint(40.0150, -105.2705, 1655.0, t0, accuracyMeters = 45.0, speedMps = 3.0)
        )
        assertNull(weak)
        assertEquals(1, smoother.rejectedOutlierCount)

        // Valid fix establishes baseline
        val p1 = smoother.processLivePoint(
            TrackPoint(40.0150, -105.2705, 1655.0, t0 + 1000L, accuracyMeters = 6.0, speedMps = 3.2)
        )
        assertNotNull(p1)

        // Impossible 500m jump in 1 second (~500 m/s) should be rejected
        val teleport = smoother.processLivePoint(
            TrackPoint(40.0195, -105.2705, 1660.0, t0 + 2000L, accuracyMeters = 5.0, speedMps = 3.2)
        )
        assertNull(teleport)
        assertEquals(2, smoother.rejectedOutlierCount)
    }

    @Test
    fun dampensElevationNoiseWithEmaAndSimplifiesCollinearPointsWithRdp() {
        val smoother = GpsSmoothingService()
        val t0 = 1_700_000_000_000L

        val first = smoother.processLivePoint(
            TrackPoint(40.01500, -105.27050, 1600.0, t0, accuracyMeters = 5.0, speedMps = 3.0)
        )!!
        // Sudden +20m barometric spike should be dampened by EMA alpha = 0.25 -> 1605.0m
        val spike = smoother.processLivePoint(
            TrackPoint(40.01503, -105.27050, 1620.0, t0 + 2000L, accuracyMeters = 5.0, speedMps = 3.0)
        )!!
        assertEquals(1605.0, spike.elevationMeters, 0.01)

        // Build straight line with a sharp switchback vertex in the middle
        val route = listOf(
            TrackPoint(40.0000, -105.0000, 1600.0, t0, 4.0, 3.0),
            TrackPoint(40.0005, -105.0000, 1602.0, t0 + 10_000L, 4.0, 3.0), // collinear
            TrackPoint(40.0010, -105.0000, 1604.0, t0 + 20_000L, 4.0, 3.0), // switchback apex
            TrackPoint(40.0010, -105.0010, 1606.0, t0 + 30_000L, 4.0, 3.0)  // sharp 90-deg turn
        )
        val simplified = GpsSmoothingService.simplifyTrack(route, epsilonMeters = 2.5)
        assertEquals(3, simplified.size) // drops redundant collinear point, keeps switchback corner
        assertTrue(first.latitude == 40.01500)
    }

    @Test
    fun calculatesSteepClimbGradePercentageAndIgnoresMicroStops() {
        val t0 = 1_700_000_000_000L
        val base = TrackPoint(40.0000, -105.0000, 1600.0, t0, 4.0, 2.5)
        // Micro-movement under 5m must return 0.0%
        val microMove = TrackPoint(40.00001, -105.0000, 1602.0, t0 + 2_000L, 4.0, 0.5)
        assertEquals(0.0, GpsSmoothingService.calculateGradePercentage(base, microMove), 0.001)

        // ~111.2m north with +10m elevation gain -> ~9.0% steep uphill grade
        val steepClimb = TrackPoint(40.0010, -105.0000, 1610.0, t0 + 40_000L, 4.0, 2.5)
        val grade = GpsSmoothingService.calculateGradePercentage(base, steepClimb)
        assertTrue("Expected grade near 9%, got $grade", grade in 8.5..9.5)
    }

    @Test
    fun mapsSlopeToQuadraticGrowlVolumeAndDetectsRollingPaceZones() {
        assertEquals(0.0, PaceTriggerService.calculateGrowlVolume(3.5), 0.001)
        assertEquals(0.0, PaceTriggerService.calculateGrowlVolume(4.0), 0.001)
        assertEquals(0.25, PaceTriggerService.calculateGrowlVolume(9.5), 0.01) // midpoint squared = 0.25
        assertEquals(1.0, PaceTriggerService.calculateGrowlVolume(15.0), 0.001)
        assertEquals(1.0, PaceTriggerService.calculateGrowlVolume(20.0), 0.001)

        // Target pace 8:00/mi (480 sec/mi); runner sprints at ~6:00/mi (~4.47 m/s)
        val trigger = PaceTriggerService(targetPaceSecPerMile = 480.0)
        val t0 = 1_700_000_000_000L
        assertNull(trigger.updatePace(TrackPoint(40.00000, -105.0, 1600.0, t0, 4.0, 4.5)))
        assertNull(trigger.updatePace(TrackPoint(40.00020, -105.0, 1600.0, t0 + 5_000L, 4.0, 4.5)))
        val surgeEval = trigger.updatePace(TrackPoint(40.00040, -105.0, 1600.0, t0 + 10_000L, 4.0, 4.5))
        assertNotNull(surgeEval)
        assertEquals(PaceZone.HUNTING_SURGE, surgeEval?.currentZone)
        assertEquals(PaceZone.HUNTING_SURGE, surgeEval?.triggeredZoneTransition)

        // Immediate next point within 60s cooldown should NOT re-trigger
        val suppressed = trigger.updatePace(TrackPoint(40.00045, -105.0, 1600.0, t0 + 11_000L, 4.0, 4.5))
        assertNull(suppressed?.triggeredZoneTransition)
    }
}
