package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StudyPDF", appName)
  }

  @Test
  fun `normalizeBaseUrl ensures scheme and trailing slash`() {
    assertEquals(
      "https://pdf-chatbot-api.sacadcompany.workers.dev/",
      SessionPreferences.normalizeBaseUrl("pdf-chatbot-api.sacadcompany.workers.dev"),
    )
    assertEquals(
      "https://example.workers.dev/",
      SessionPreferences.normalizeBaseUrl("https://example.workers.dev"),
    )
  }
}
