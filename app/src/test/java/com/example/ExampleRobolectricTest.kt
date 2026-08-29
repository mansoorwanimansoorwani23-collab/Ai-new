package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preference.AiraPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura", appName)
  }

  @Test
  fun `aira preferences default and custom wake phrase`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AiraPreferences(context)
    assertEquals("Hi Aira", prefs.wakePhrase.value)
    assertTrue(prefs.isWakeWordEnabled.value)

    prefs.setWakePhrase("Namaste Aira")
    assertEquals("Namaste Aira", prefs.wakePhrase.value)

    prefs.setVoice("shimmer")
    assertEquals("shimmer", prefs.voice.value)
  }
}
