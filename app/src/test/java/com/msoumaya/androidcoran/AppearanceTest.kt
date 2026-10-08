package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import kotlinx.serialization.json.*
import org.junit.Test
class AppearanceTest {
    @Test fun themeDefaultsDoNotOverrideNonWhitePalettes() { assertEquals("#153F36",appPalette("classic",null).primary);assertEquals("#132B47",appPalette("night",null).primary);assertEquals("#7B285C",appPalette("unknown",null).primary) }
    @Test fun explicitAccentKeepsThemePaperAndText() { val p=appPalette("classic","rose");assertEquals("#A95069",p.primary);assertEquals("#FCF0F3",p.selected);assertEquals("#FFFDF7",p.surface);assertEquals("#20342E",p.text);assertEquals(appPalette("classic",null),appPalette("classic","invalid")) }
    @Test fun paperFallbackAndArtworkMatchReference() { assertEquals("#faf7f2",paperColor("unknown"));assertEquals("#d7c5ad",paperColor("sepia"));assertEquals("emerald",themeArtwork("classic"));assertEquals("rose",themeArtwork("feminine")) }
    @Test fun homeResumesLastReadBeforeTodaySessionAndGoal() {
        val state=defaultState();assertEquals(5673,homeReadingVerse(state))
        val session=state.with("sessions" to element(listOf(json("status" to "todo","date" to "2026-10-08","start" to 6000,"end" to 6001))))
        assertEquals(6000,homeReadingVerse(session,java.time.LocalDate.of(2026,10,8)))
        assertEquals(9,homeReadingVerse(session.with("lastRead" to json("verseId" to 9))))
    }
}
