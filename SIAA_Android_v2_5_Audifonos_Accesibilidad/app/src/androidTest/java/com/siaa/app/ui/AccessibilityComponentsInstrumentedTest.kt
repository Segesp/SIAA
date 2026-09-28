package com.siaa.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Must be executed on an emulator/device. Adding this file is not a passing device test. */
class AccessibilityComponentsInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullWidthActionHasLabelClickAndMinimumTarget() {
        var clicked = false
        compose.setContent { MaterialTheme { ActionButton("Comenzar mi sesión") {clicked=true} } }
        compose.onNodeWithText("Comenzar mi sesión").assertHasClickAction()
            .assertHeightIsAtLeast(56.dp).performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }

    @Test fun headingIsExposedAsAHeading() {
        compose.setContent { MaterialTheme { Heading("Tus audífonos, tus controles") } }
        compose.onNodeWithText("Tus audífonos, tus controles")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
    }

    @Test fun longSwitchLabelIsOneAccessibleActionAtLargeFontScale() {
        val enabled = mutableStateOf(true)
        val label = "Pausar mi sesión cuando deje de responder a las opciones"
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density=density,fontScale=2f)) {
                MaterialTheme { Column { ToggleRow(label,enabled.value){enabled.value=it} } }
            }
        }
        compose.onNodeWithText(label).assertHasClickAction().assertIsOn()
            .assertHeightIsAtLeast(56.dp).performClick()
        compose.runOnIdle {assertFalse(enabled.value)}
        compose.onNodeWithText(label).assertIsOff()
    }
}
