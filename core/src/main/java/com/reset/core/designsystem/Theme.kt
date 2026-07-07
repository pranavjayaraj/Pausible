package com.reset.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reset.core.R

/**
 * Centralised app design tokens (the "Pausible" palette from the approved BloomNow design).
 * Shared across feature modules — screens reference these instead of hardcoding
 * colours, type, or dimensions.
 */

// ── Fonts ─────────────────────────────────────────────────────
object AppFonts {
    val Sans = FontFamily(
        Font(R.font.nunito_regular, FontWeight.Normal),
        Font(R.font.nunito_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    )
}

// ── Colours ───────────────────────────────────────────────────
object AppColors {
    // Brand
    val accent = Color(0xFFF0813C)
    val accentDark = Color(0xFFC0552F)
    val teal = Color(0xFF1F6F8B)
    val tealDeep = Color(0xFF123543)

    // Surfaces
    val surface = Color(0xFFFDF9F1)
    val surfaceWarm = Color(0xFFFCEFE6)
    val surfaceBreak = Color(0xFFFCF7EF)
    val surfaceWhite = Color(0xFFFFFFFF)
    val breathingBg = Color(0xFF20606E)

    // Ink (text on light surfaces)
    val ink = Color(0xFF1A1A1A)
    val inkBody = Color(0xFF6B6B6B)
    val inkSoft = Color(0xFF5A5348)
    val inkMuted = Color(0xFF8A8175)
    val inkFaint = Color(0xFF9A9186)
    val builderInk = Color(0xFF29264A)
    val builderSub = Color(0xFF6B6580)
    val panelSub = Color(0xFF8A8578)

    // Text on dark/brand surfaces
    val textOnDark = Color.White
    val textOnDarkSoft = Color.White.copy(alpha = 0.85f)
    val textOnDarkMuted = Color.White.copy(alpha = 0.75f)
    val textOnDarkFaint = Color.White.copy(alpha = 0.65f)

    // Hairlines / panels / chips
    val hairline = Color(0xFFEFE8DB)
    val hairlineCard = Color(0xFFECE4D6)
    val divider = Color(0xFFE5DDD2)
    val panel = Color(0xFFF3F1EC)
    val panelDivider = Color(0xFFE5E2DA)
    val chipUnselected = Color(0xFFEFEDE8)
    val exploreBorder = Color(0xFFD8CFC0)
    val toggleOff = Color(0xFFD8D5CC)

    // Home tiles
    val breathTile = Color(0xFFF3DFA4)
    val breathTileInk = Color(0xFF6B5A2E)
    val breathTileWave = Color(0xFF8A6D2F)
    val breathTileLeaf = Color(0xFFE8C36E)
    val customTile = Color(0xFF206E78)
    val homeTabPill = Color(0xFFF8D9A8)

    // Builder
    val suggestCard = Color(0xFFFFF3E3)

    // Break suggestion cards
    val breakHeader = Color(0xFFF4C33C)
    val breakHeaderInk = Color(0xFF5A4D24)
    val stretchCardStart = Color(0xFFF4913C)
    val stretchCardEnd = Color(0xFFF7A24A)
    val meditateCard = Color(0xFFF5C63E)
    val meditateCardInk = Color(0xFF4A3A10)
    val meditateCardSub = Color(0xFF7A6528)
    val breathingCard = Color(0xFF25C4D6)

    // Focus ring
    val ringTrack = Color.White.copy(alpha = 0.15f)
    val ringProgress = Color(0xFF5FD3E0)

    // Breathing guide
    val guideHalo = Color.White.copy(alpha = 0.12f)
    val cycleDotActive = Color(0xFFF5C542)
    val cycleDotIdle = Color.White.copy(alpha = 0.30f)
    val finishEarlyBorder = Color.White.copy(alpha = 0.40f)

    // Stats
    val statsBreaksCard = Color(0xFF29C5D6)
    val statsBreaksLabel = Color(0xFF0E5560)
    val statsBreaksInk = Color(0xFF083D45)
    val streakCardStart = Color(0xFFF5C542)
    val streakCardEnd = Color(0xFFF0A93C)
    val streakInk = Color(0xFF4A3A10)
    val streakLabel = Color(0xFF6B5210)
    val barIdle = Color(0xFFE8DFD0)
    val barActive = Color(0xFFF29A38)
    val statsTabInk = Color(0xFF0E7A88)
    val statsTabPill = Color(0xFFBDEEF4)

    // Dashboard bar
    val timerTabPill = Color(0xFF8A5A22)
    val timerTabShadow = Color(0xFF8A5A22).copy(alpha = 0.42f)

    // Sign in
    val googleButton = Color(0xFFF2E3D0)
    val googleG = Color(0xFFDB4437)

    // Sprout mascot
    val sproutLeafDark = Color(0xFF4E8A3C)
    val sproutLeafLight = Color(0xFF6BA94F)
    val sproutBody = Color(0xFFF29A38)
    val sproutBodyCream = Color(0xFFFBE0BC)
    val sproutBodyPeach = Color(0xFFFFD9A8)
    val sproutFace = Color(0xFF5A3410)
    val sproutCheek = Color(0xFFE8622C)

    val stretchCardGradient: Brush = Brush.linearGradient(listOf(stretchCardStart, stretchCardEnd))
    val streakCardGradient: Brush = Brush.linearGradient(listOf(streakCardStart, streakCardEnd))
}

// ── Shapes ────────────────────────────────────────────────────
object AppShapes {
    val bell = RoundedCornerShape(13.dp)
    val input = RoundedCornerShape(16.dp)
    val tipCard = RoundedCornerShape(18.dp)
    val chip = RoundedCornerShape(18.dp)
    val panel = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(22.dp)
    val button = RoundedCornerShape(26.dp)
    val cta = RoundedCornerShape(28.dp)
    val headerCurve = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    val pill = RoundedCornerShape(percent = 50)
}

// ── Dimensions ────────────────────────────────────────────────
object AppDimens {
    val screenPaddingH = 22.dp
    val screenPaddingV = 28.dp

    val cardPadding = 20.dp
    val tilePadding = 16.dp
    val tileHeight = 128.dp

    val buttonHeight = 50.dp
    val ctaHeight = 56.dp

    val ringSize = 280.dp
    val ringStroke = 22.dp

    val guideHaloOuter = 230.dp
    val guideHaloInner = 180.dp
    val guideMascot = 150.dp

    val touchTargetMin = 48.dp
}

// ── Type scale ────────────────────────────────────────────────
object AppType {
    private fun nunito(size: Float, weight: FontWeight, lineHeight: Float? = null, tracking: Float = 0f) =
        TextStyle(
            fontFamily = AppFonts.Sans,
            fontSize = size.sp,
            fontWeight = weight,
            letterSpacing = tracking.sp,
        ).let { if (lineHeight != null) it.copy(lineHeight = lineHeight.sp) else it }

    // Headings
    val displayTitle = nunito(30f, FontWeight.ExtraBold)
    val welcomeTitle = nunito(27f, FontWeight.ExtraBold, lineHeight = 32.4f)
    val factTitle = nunito(24f, FontWeight.ExtraBold, lineHeight = 32.4f)
    val cardTitle = nunito(24f, FontWeight.ExtraBold)
    val screenTitle = nunito(21f, FontWeight.ExtraBold)
    val sectionTitle = nunito(16f, FontWeight.ExtraBold)
    val statsSection = nunito(18f, FontWeight.ExtraBold)
    val headerTitle = nunito(17f, FontWeight.ExtraBold)

    // Labels / eyebrows
    val eyebrow = nunito(10f, FontWeight.ExtraBold, tracking = 1.4f)
    val eyebrowWide = nunito(11f, FontWeight.ExtraBold, tracking = 1.6f)
    val exploreLabel = nunito(10f, FontWeight.ExtraBold, tracking = 1f)
    val skipLabel = nunito(11f, FontWeight.ExtraBold, tracking = 1.4f)
    val cardMeta = nunito(10.5f, FontWeight.ExtraBold, tracking = 0.6f)
    val dividerLabel = nunito(10f, FontWeight.ExtraBold, tracking = 1.2f)
    val tab = nunito(10f, FontWeight.ExtraBold)

    // Body
    val body = nunito(13.5f, FontWeight.SemiBold, lineHeight = 20.3f)
    val bodySmall = nunito(13f, FontWeight.SemiBold, lineHeight = 19.5f)
    val tipBody = nunito(12.5f, FontWeight.SemiBold, lineHeight = 18.8f)
    val caption = nunito(12.5f, FontWeight.SemiBold)
    val legal = nunito(10.5f, FontWeight.SemiBold, lineHeight = 16.8f)
    val panelHint = nunito(10.5f, FontWeight.Bold)

    // Cards
    val heroPrompt = nunito(15f, FontWeight.ExtraBold)
    val breakCardTitle = nunito(16f, FontWeight.ExtraBold)
    val suggestTitle = nunito(12.5f, FontWeight.ExtraBold)
    val suggestSub = nunito(11.5f, FontWeight.Bold, lineHeight = 15.5f)
    val bannerTitle = nunito(14f, FontWeight.ExtraBold)
    val bannerBody = nunito(12f, FontWeight.SemiBold)

    // Controls
    val chip = nunito(12f, FontWeight.ExtraBold)
    val chipSmall = nunito(10.5f, FontWeight.ExtraBold)
    val chipSub = nunito(10.5f, FontWeight.Bold)
    val paceLabel = nunito(13f, FontWeight.ExtraBold)
    val toggleLabel = nunito(13.5f, FontWeight.ExtraBold)
    val button = nunito(14f, FontWeight.ExtraBold)
    val buttonSmall = nunito(12f, FontWeight.ExtraBold)
    val buttonTiny = nunito(11f, FontWeight.ExtraBold)
    val saveButton = nunito(12.5f, FontWeight.ExtraBold)
    val cta = nunito(16f, FontWeight.ExtraBold)
    val input = nunito(13f, FontWeight.ExtraBold)
    val soundName = nunito(11.5f, FontWeight.ExtraBold)
    val soundTag = nunito(8.5f, FontWeight.ExtraBold, tracking = 1.2f)

    // Big numbers
    val timer = nunito(54f, FontWeight.ExtraBold, tracking = 1f)
    val breathPhase = nunito(26f, FontWeight.ExtraBold)
    val statValue = nunito(44f, FontWeight.ExtraBold)
    val statUnit = nunito(18f, FontWeight.ExtraBold)
    val barDay = nunito(10f, FontWeight.ExtraBold)

    val errorTitle = nunito(18f, FontWeight.ExtraBold)
}
