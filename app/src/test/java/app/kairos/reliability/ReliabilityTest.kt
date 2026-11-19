package app.kairos.reliability

import app.kairos.util.TimeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityTest {
    private fun status(n: Boolean = true, e: Boolean = true, b: Boolean = true, v: DkmaVendor? = null) =
        ReliabilityStatus(v, "", n, e, b)

    @Test fun vendorDetectionUsesPrefixes() {
        assertEquals(DkmaVendor.XIAOMI, Reliability.vendorOf("Xiaomi", "Redmi"))
        assertEquals(DkmaVendor.XIAOMI, Reliability.vendorOf("unknown", "POCO"))
        assertEquals(DkmaVendor.SAMSUNG, Reliability.vendorOf("samsung", "samsung"))
        assertNull("Micromax Xiaomi sanılmamalı", Reliability.vendorOf("Micromax", "Micromax"))
        assertEquals("https://dontkillmyapp.com/general", Reliability.guideUrl(null))
        assertEquals("Xiaomi", Reliability.vendorNameOf("xiaomi"))
        assertEquals("OnePlus", Reliability.vendorNameOf("OnePlus"))
    }

    @Test fun blockingOnlyForNotificationsOrExactAlarms() {
        assertFalse(status().blocking)
        assertTrue(status(n = false).blocking)
        assertTrue(status(e = false).blocking)
        assertFalse("pil sadece öneri", status(b = false).blocking)
        assertFalse("üretici sadece bilgi", status(v = DkmaVendor.SAMSUNG).blocking)
    }

    @Test fun signatureChangesWhenANewIssueAppears() {
        val a = status(n = false)
        val b = status(n = false, e = false)
        assertTrue(a.signature != b.signature)
        assertEquals(listOf(ReliabilityIssue.NOTIFICATIONS, ReliabilityIssue.EXACT_ALARMS), b.issues)
    }

    @Test fun turkishLocativeSuffix() {
        assertEquals("08:00'de", TimeText.timeWithLocative(8 * 60))
        assertEquals("09:00'da", TimeText.timeWithLocative(9 * 60))
        assertEquals("10:00'da", TimeText.timeWithLocative(10 * 60))
        assertEquals("13:00'te", TimeText.timeWithLocative(13 * 60))
        assertEquals("07:30'da", TimeText.timeWithLocative(7 * 60 + 30))
        assertEquals("07:15'te", TimeText.timeWithLocative(7 * 60 + 15))
        assertEquals("06:40'ta", TimeText.timeWithLocative(6 * 60 + 40))
        assertEquals("20:00'de", TimeText.timeWithLocative(20 * 60))
    }
}
