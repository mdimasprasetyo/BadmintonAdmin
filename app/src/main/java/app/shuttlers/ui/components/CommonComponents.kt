package app.shuttlers.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shuttlers.ui.theme.AppText
import app.shuttlers.ui.theme.Barlow
import app.shuttlers.ui.theme.Coral
import app.shuttlers.ui.theme.CourtNight
import app.shuttlers.ui.theme.GenderFemale
import app.shuttlers.ui.theme.GenderMale
import app.shuttlers.ui.theme.OnCoral
import app.shuttlers.ui.theme.OnGenderFemale
import app.shuttlers.ui.theme.OnGenderMale
import app.shuttlers.ui.theme.tierColor

/** Shared measurements so every screen uses the same corners and spacing. */
object Dimens {
    val ScreenPadding = 16.dp
    val CardCorner = 20.dp
    val InnerCorner = 14.dp
    val ChipCorner = 10.dp
    val CardPadding = 16.dp
    val ButtonHeight = 56.dp
    val ToolButtonHeight = 48.dp
}

val TIERS = listOf("S", "A", "B", "C", "D", "X")

/* ---------------------------------------------------------------- labels */

/** Small uppercase, letter-spaced section label ("WAITING LOUNGE"). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = AppText.SectionLabel,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing(this)
    }
}

/* ---------------------------------------------------------------- surfaces */

/** Rounded dark card with a thin outline: the basic building block of every screen. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    outlined: Boolean = true,
    shape: Shape = RoundedCornerShape(Dimens.CardCorner),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    }
}

/** Big condensed number over a small caps label. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Rolling(value) { Text(it, style = AppText.NumberMedium, color = valueColor, maxLines = 1) }
            Spacer(Modifier.height(2.dp))
            Text(label.uppercase(), style = AppText.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/* ---------------------------------------------------------------- buttons */

/** Primary action: solid coral, dark text, heavy weight (Pickles' "rally" button). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    height: Dp = Dimens.ButtonHeight
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).pressScale(source),
        interactionSource = source,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Coral,
            contentColor = OnCoral,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp), maxLines = 1)
    }
}

/** Secondary tool button: raised surface, light text, 14dp corners (Pickles' ToolButton). */
@Composable
fun ToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    height: Dp = Dimens.ToolButtonHeight
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).pressScale(source),
        interactionSource = source,
        shape = RoundedCornerShape(Dimens.InnerCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Outlined variant for less important actions inside cards. */
@Composable
fun OutlineToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    height: Dp = Dimens.ToolButtonHeight
) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height).pressScale(source),
        interactionSource = source,
        shape = RoundedCornerShape(Dimens.InnerCorner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Muted text button used for "Cancel" / "Close" in dialogs. */
@Composable
fun QuietTextButton(text: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/* ---------------------------------------------------------------- top bar */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle.uppercase(),
                        style = AppText.SectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

/* ---------------------------------------------------------------- players */

/** Square tier badge: condensed letter on a tinted tile, colour by tier. */
@Composable
fun TierBadge(
    tier: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    fontSize: TextUnit = 17.sp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val color = tierColor(tier)
    val shape = RoundedCornerShape(8.dp)
    var boxModifier = modifier
        .size(size)
        .clip(shape)
        .background(color.copy(alpha = 0.16f))
        .border(1.dp, color.copy(alpha = 0.55f), shape)
    if (onClick != null) {
        boxModifier = boxModifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Text(
            text = tier.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = fontSize),
            textAlign = TextAlign.Center
        )
    }
}

/** Small round gender mark (M teal-blue, W rose). */
@Composable
fun GenderMark(gender: String, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val isMale = gender.uppercase() == "M"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isMale) GenderMale else GenderFemale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isMale) "M" else "W",
            color = if (isMale) OnGenderMale else OnGenderFemale,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
    }
}

/** Pill with the player's name tinted by gender and the tier letter. */
@Composable
fun PlayerBadge(name: String, level: String, gender: String, modifier: Modifier = Modifier) {
    val isMale = gender.uppercase() == "M"
    val contentColor = if (isMale) OnGenderMale else OnGenderFemale
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.ChipCorner))
            .background(if (isMale) GenderMale else GenderFemale)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = level.uppercase(),
            color = tierColor(level),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        )
    }
}

/** One player line: gender mark, name, tier badge. Used on court cards. */
@Composable
fun PlayerLine(
    name: String,
    tier: String,
    gender: String,
    modifier: Modifier = Modifier,
    nameColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        GenderMark(gender)
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        TierBadge(tier, size = 24.dp, fontSize = 15.sp)
    }
}

/** "3–1" record pill. Shown only when the player has a result. */
@Composable
fun RecordPill(wins: Int, losses: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Rolling("$wins–$losses") { record ->
        Text(
            text = record,
            style = AppText.NumberSmall.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        }
    }
}

/* ---------------------------------------------------------------- selectors */

/** Row of six tier tiles; the selected one is filled with its tier colour. */
@Composable
fun TierSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tiers: List<String> = TIERS
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tiers.forEach { tier ->
            val color = tierColor(tier)
            val isSelected = tier.equals(selected, ignoreCase = true)
            val shape = RoundedCornerShape(12.dp)
            val fill by animateColorAsState(if (isSelected) color else color.copy(alpha = 0.10f), tween(Motion.SHORT), label = "tierFill")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(shape)
                    .background(fill)
                    .border(1.dp, color.copy(alpha = if (isSelected) 1f else 0.4f), shape)
                    .clickable(role = Role.RadioButton) { onSelect(tier) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tier,
                    color = if (isSelected) CourtNight else color,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                )
            }
        }
    }
}

/** Man / Woman choice in the gender chip colours. */
@Composable
fun GenderSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("M" to "Man", "W" to "Woman").forEach { (code, label) ->
            val isSelected = selected.equals(code, ignoreCase = true)
            val container = if (code == "M") GenderMale else GenderFemale
            val content = if (code == "M") OnGenderMale else OnGenderFemale
            val shape = RoundedCornerShape(12.dp)
            val fill by animateColorAsState(if (isSelected) container else Color.Transparent, tween(Motion.SHORT), label = "genderFill")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(shape)
                    .background(fill)
                    .border(1.dp, if (isSelected) content.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline, shape)
                    .clickable(role = Role.RadioButton) { onSelect(code) }
                    .alpha(if (isSelected) 1f else 0.75f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GenderMark(code, size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(label, color = if (isSelected) content else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/* ---------------------------------------------------------------- dialogs */

/** Confirmation dialog in the app style. [destructive] paints the confirm button in the danger colour. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                } else {
                    ButtonDefaults.buttonColors(containerColor = Coral, contentColor = OnCoral)
                }
            ) { Text(confirmText, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { QuietTextButton(dismissText, onDismiss) }
    )
}
