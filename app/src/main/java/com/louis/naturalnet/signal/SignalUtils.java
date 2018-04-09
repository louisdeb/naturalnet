package com.louis.naturalnet.signal;

import android.content.Intent;
import android.location.Location;
import android.telephony.SignalStrength;
import com.google.android.gms.location.LocationResult;

import static com.google.android.gms.location.LocationResult.extractResult;
import static com.google.android.gms.location.LocationResult.hasResult;

/*
    GSM Signal Information: https://archive.org/stream/etsi_ts_127_007_v08.05.00/ts_127007v080500p#page/n81/mode/2up/search/99
 */

public class SignalUtils {

    public static SignalQuality getSignalQuality(SignalStrength signalStrength) {
        // signalStrength.getLevel() needs API level 23.
        // We utilise functions from SignalStrength, implemented locally, to overcome the version issues.

        SignalQuality quality;

        if (signalStrength.isGsm()) {
            int gsmLevel = signalStrength.getGsmSignalStrength();
            if (gsmLevel <= 2 || gsmLevel == 99) quality = SignalQuality.NONE_OR_NOT_KNOWN;
            else if (gsmLevel >= 12) quality = SignalQuality.GREAT;
            else if (gsmLevel >= 8)  quality = SignalQuality.GOOD;
            else if (gsmLevel >= 5)  quality = SignalQuality.MODERATE;
            else quality = SignalQuality.POOR;

        } else {
            SignalQuality cdmaQuality = getCdmaQuality(signalStrength);
            SignalQuality evdoQuality = getEvdoLevel(signalStrength);
            if (evdoQuality == SignalQuality.NONE_OR_NOT_KNOWN) {
                /* We don't know evdo, use cdma */
                quality = cdmaQuality;
            } else if (cdmaQuality == SignalQuality.NONE_OR_NOT_KNOWN) {
                /* We don't know cdma, use evdo */
                quality = evdoQuality;
            } else {
                /* We know both, use the lowest level */
                quality = cdmaQuality.getVal() < evdoQuality.getVal() ? cdmaQuality : evdoQuality;
            }
        }

        return quality;
    }

    private static SignalQuality getCdmaQuality(SignalStrength signalStrength) {
        final int cdmaDbm = signalStrength.getCdmaDbm();
        final int cdmaEcio = signalStrength.getCdmaEcio();
        SignalQuality qualityDbm;
        SignalQuality qualityEcio;

        if (cdmaDbm >= -75) qualityDbm = SignalQuality.GREAT;
        else if (cdmaDbm >= -85) qualityDbm = SignalQuality.GOOD;
        else if (cdmaDbm >= -95) qualityDbm = SignalQuality.MODERATE;
        else if (cdmaDbm >= -100) qualityDbm = SignalQuality.POOR;
        else qualityDbm = SignalQuality.NONE_OR_NOT_KNOWN;

        // Ec/Io are in dB*10
        if (cdmaEcio >= -90) qualityEcio = SignalQuality.GREAT;
        else if (cdmaEcio >= -110) qualityEcio = SignalQuality.GOOD;
        else if (cdmaEcio >= -130) qualityEcio = SignalQuality.MODERATE;
        else if (cdmaEcio >= -150) qualityEcio = SignalQuality.POOR;
        else qualityEcio = SignalQuality.NONE_OR_NOT_KNOWN;

        return (qualityDbm.getVal() < qualityEcio.getVal()) ? qualityDbm : qualityEcio;
    }

    private static SignalQuality getEvdoLevel(SignalStrength signalStrength) {
        final int evdoDbm = signalStrength.getEvdoDbm();
        final int evdoSnr = signalStrength.getEvdoSnr();
        SignalQuality qualityDbm;
        SignalQuality qualitySnr;

        if (evdoDbm >= -65) qualityDbm = SignalQuality.GREAT;
        else if (evdoDbm >= -75) qualityDbm = SignalQuality.GOOD;
        else if (evdoDbm >= -90) qualityDbm = SignalQuality.MODERATE;
        else if (evdoDbm >= -105) qualityDbm = SignalQuality.POOR;
        else qualityDbm = SignalQuality.NONE_OR_NOT_KNOWN;

        if (evdoSnr >= 7) qualitySnr = SignalQuality.GREAT;
        else if (evdoSnr >= 5) qualitySnr = SignalQuality.GOOD;
        else if (evdoSnr >= 3) qualitySnr = SignalQuality.MODERATE;
        else if (evdoSnr >= 1) qualitySnr = SignalQuality.POOR;
        else qualitySnr = SignalQuality.NONE_OR_NOT_KNOWN;

        return (qualityDbm.getVal() < qualitySnr.getVal()) ? qualityDbm : qualitySnr;
    }

    public static SignalQuality getGpsQuality(Intent intent) {
        // Will want to add some smoothing to this function.
        // Some intents will not contain the location but will be right after a request
        // that does contain the location, and followed by a request that does contain the location.

        if (!hasResult(intent))
            return SignalQuality.NONE_OR_NOT_KNOWN;

        LocationResult locationResult = extractResult(intent);
        Location location = locationResult.getLastLocation();
        float accuracy = location.getAccuracy();

        SignalQuality quality;

        if (accuracy <= 5) quality = SignalQuality.GREAT;
        else if (accuracy <= 20) quality = SignalQuality.GOOD;
        else if (accuracy <= 100) quality = SignalQuality.MODERATE;
        else if (accuracy <= 1000) quality = SignalQuality.POOR;
        else quality = SignalQuality.NONE_OR_NOT_KNOWN;

        return quality;
    }

}
