package app.kairos.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private fun release(tag: String, apk: String? = "Kairos-$tag.apk", url: String = "https://github.com/x/y/releases/download/$tag/a.apk", draft: Boolean = false) = """
        {"tag_name":"$tag","body":"Bildirim sesi yükseltildi.\nDiğer düzeltmeler","draft":$draft,"prerelease":false,
         "assets":[${if (apk == null) "" else """{"name":"$apk","browser_download_url":"$url","size":2048000}"""}],
         "html_url":"https://github.com/x/y","author":{"login":"x"}}
    """.trimIndent()

    @Test fun versionComparison() {
        assertTrue(UpdateChecker.isNewer("v0.1.3", "0.1.2"))
        assertTrue(UpdateChecker.isNewer("0.2.0", "0.1.9"))
        assertTrue(UpdateChecker.isNewer("1.0", "0.9.9"))
        assertFalse(UpdateChecker.isNewer("v0.1.2", "0.1.2"))
        assertFalse(UpdateChecker.isNewer("0.1.1", "0.1.2"))
        assertEquals(listOf(0, 1, 2), UpdateChecker.parse("v0.1.2-beta"))
    }

    @Test fun newerReleaseWithApkIsOffered() {
        val u = UpdateChecker.fromRelease(release("v0.1.3"), "0.1.2")!!
        assertEquals("0.1.3", u.versionName)
        assertEquals("Bildirim sesi yükseltildi.\nDiğer düzeltmeler", u.notes)
        assertEquals(2048000L, u.sizeBytes)
    }

    @Test fun sameOrOlderDraftNoApkOrInsecureUrlIsIgnored() {
        assertNull(UpdateChecker.fromRelease(release("v0.1.2"), "0.1.2"))
        assertNull(UpdateChecker.fromRelease(release("v0.1.1"), "0.1.2"))
        assertNull(UpdateChecker.fromRelease(release("v0.1.3", draft = true), "0.1.2"))
        assertNull(UpdateChecker.fromRelease(release("v0.1.3", apk = null), "0.1.2"))
        assertNull(UpdateChecker.fromRelease(release("v0.1.3", url = "http://insecure/a.apk"), "0.1.2"))
    }
}
