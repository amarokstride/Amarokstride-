import Toybox.Activity;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;
import Toybox.Attention;
import Toybox.Math;

class StridersHowlView extends WatchUi.DataField {

    // Striders Howl Color Palette
    private const COLOR_DEEP_NAVY  = 0x0A0F1D;
    private const COLOR_CYAN_GLOW  = 0x00E5FF;
    private const COLOR_FROST_WHT  = 0xFFFFFF;
    private const COLOR_MUTED_GREY = 0x8892B0;
    private const COLOR_ALPHA_GOLD = 0xFFD700;
    private const COLOR_HR_ALERT   = 0xFF3366;

    // Running Metrics
    private var mCurrentPaceStr as String = "--:--";
    private var mTotalDistanceStr as String = "0.00";
    private var mHeartRate as Number = 0;
    private var mElapsedSec as Number = 0;
    private var mLastMilestoneMile as Number = 0;

    function initialize() {
        DataField.initialize();
    }

    // Called whenever Activity info updates (1 Hz during run)
    function compute(info as Activity.Info) as Void {
        // 1. Current Pace Calculation (min/mile)
        if (info.currentSpeed != null && info.currentSpeed > 0.2) {
            var secPerMile = (1609.344 / info.currentSpeed).toNumber();
            var minutes = secPerMile / 60;
            var seconds = secPerMile % 60;
            mCurrentPaceStr = Lang.format("$1$:$2$", [minutes.format("%d"), seconds.format("%02d")]);
        } else {
            mCurrentPaceStr = "--:--";
        }

        // 2. Total Distance in Miles
        if (info.elapsedDistance != null) {
            var miles = info.elapsedDistance / 1609.344;
            mTotalDistanceStr = miles.format("%.2f");

            // Milestone check (trigger haptic howl every 1.0 mile)
            var currentMileInt = Math.floor(miles).toNumber();
            if (currentMileInt > mLastMilestoneMile && currentMileInt > 0) {
                mLastMilestoneMile = currentMileInt;
                triggerWolfHaptic();
            }
        }

        // 3. Heart Rate
        if (info.currentHeartRate != null) {
            mHeartRate = info.currentHeartRate;
        } else {
            mHeartRate = 0;
        }

        // 4. Elapsed Time
        if (info.elapsedTime != null) {
            mElapsedSec = (info.elapsedTime / 1000).toNumber();
        }
    }

    // Custom multi-burst haptic vibration for milestone unlock
    private function triggerWolfHaptic() as Void {
        if (Attention has :vibrate) {
            var vibeData = [
                new Attention.VibeProfile(50, 200),
                new Attention.VibeProfile(0, 100),
                new Attention.VibeProfile(100, 400),
                new Attention.VibeProfile(0, 100),
                new Attention.VibeProfile(100, 800)
            ];
            Attention.vibrate(vibeData);
        }
    }

    // Screen Render Loop
    function onUpdate(dc as Graphics.Dc) as Void {
        var width = dc.getWidth();
        var height = dc.getHeight();
        var centerX = width / 2;
        var centerY = height / 2;

        dc.setColor(COLOR_DEEP_NAVY, COLOR_DEEP_NAVY);
        dc.clear();

        drawHeartRateZoneArc(dc, centerX, centerY, (width / 2) - 4);
        drawWolfPawIcon(dc, centerX, 24);

        dc.setColor(COLOR_MUTED_GREY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, centerY - 45, Graphics.FONT_XTINY, "PACE (/MI)", Graphics.TEXT_JUSTIFY_CENTER);

        dc.setColor(COLOR_CYAN_GLOW, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, centerY - 30, Graphics.FONT_NUMBER_HOT, mCurrentPaceStr, Graphics.TEXT_JUSTIFY_CENTER);

        var colLeft = centerX - (width * 0.22);
        var colRight = centerX + (width * 0.22);
        var rowBottomY = centerY + 38;

        dc.setColor(COLOR_MUTED_GREY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(colLeft, rowBottomY, Graphics.FONT_XTINY, "DIST (MI)", Graphics.TEXT_JUSTIFY_CENTER);
        dc.setColor(COLOR_FROST_WHT, Graphics.COLOR_TRANSPARENT);
        dc.drawText(colLeft, rowBottomY + 16, Graphics.FONT_MEDIUM, mTotalDistanceStr, Graphics.TEXT_JUSTIFY_CENTER);

        dc.setColor(COLOR_MUTED_GREY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(colRight, rowBottomY, Graphics.FONT_XTINY, "BPM", Graphics.TEXT_JUSTIFY_CENTER);

        var hrColor = (mHeartRate > 165) ? COLOR_HR_ALERT : COLOR_FROST_WHT;
        dc.setColor(hrColor, Graphics.COLOR_TRANSPARENT);
        var hrStr = (mHeartRate > 0) ? mHeartRate.format("%d") : "--";
        dc.drawText(colRight, rowBottomY + 16, Graphics.FONT_MEDIUM, hrStr, Graphics.TEXT_JUSTIFY_CENTER);

        var timeMinutes = mElapsedSec / 60;
        var timeSeconds = mElapsedSec % 60;
        var timeHours = timeMinutes / 60;
        timeMinutes = timeMinutes % 60;
        var timerStr = Lang.format("$1$:$2$:$3$", [timeHours.format("%d"), timeMinutes.format("%02d"), timeSeconds.format("%02d")]);

        dc.setColor(COLOR_MUTED_GREY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height - 32, Graphics.FONT_XTINY, timerStr, Graphics.TEXT_JUSTIFY_CENTER);
    }

    private function drawWolfPawIcon(dc as Graphics.Dc, x as Number, y as Number) as Void {
        dc.setColor(COLOR_CYAN_GLOW, Graphics.COLOR_TRANSPARENT);
        dc.fillCircle(x, y + 4, 5);
        dc.fillCircle(x - 6, y - 1, 2);
        dc.fillCircle(x - 2, y - 5, 2);
        dc.fillCircle(x + 2, y - 5, 2);
        dc.fillCircle(x + 6, y - 1, 2);
    }

    private function drawHeartRateZoneArc(dc as Graphics.Dc, cx as Number, cy as Number, radius as Number) as Void {
        var arcColor = COLOR_MUTED_GREY;
        var sweepDeg = 180;

        if (mHeartRate > 0) {
            if (mHeartRate < 120) {
                arcColor = 0x00E5FF;
                sweepDeg = 100;
            } else if (mHeartRate < 155) {
                arcColor = 0x00FF88;
                sweepDeg = 140;
            } else if (mHeartRate < 172) {
                arcColor = COLOR_ALPHA_GOLD;
                sweepDeg = 170;
            } else {
                arcColor = COLOR_HR_ALERT;
                sweepDeg = 210;
            }
        }

        dc.setPenWidth(5);
        dc.setColor(arcColor, Graphics.COLOR_TRANSPARENT);
        dc.drawArc(cx, cy, radius, Graphics.ARC_CLOCKWISE, 180 - (sweepDeg / 2), 180 + (sweepDeg / 2));
    }
}
