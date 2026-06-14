package com.mdp.badmintonadmin.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mdp.badmintonadmin.ui.theme.GenderFemale
import com.mdp.badmintonadmin.ui.theme.GenderMale
import com.mdp.badmintonadmin.ui.theme.OnGenderFemale
import com.mdp.badmintonadmin.ui.theme.OnGenderMale

@Composable
fun PlayerBadge(name: String, level: String, gender: String, modifier: Modifier = Modifier) {
    val backgroundColor = if (gender == "M") GenderMale else GenderFemale
    val contentColor = if (gender == "M") OnGenderMale else OnGenderFemale
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "$name ($level)",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
