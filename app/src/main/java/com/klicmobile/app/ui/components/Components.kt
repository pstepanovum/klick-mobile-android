package com.klicmobile.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.klicmobile.app.ui.theme.OnPrimary
import com.klicmobile.app.ui.theme.Primary
import com.klicmobile.app.ui.theme.Surface
import com.klicmobile.app.ui.theme.SurfaceRaised
import com.klicmobile.app.ui.theme.TextPrimary

/** Fully-rounded flat primary button — no shadow, no border (design rules). */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, fill: Color = Primary, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = OnPrimary),
    ) {
        Text(text, modifier = Modifier.padding(vertical = 6.dp))
    }
}

/** Flat capsule text field with no visible outline. */
@Composable
fun KlicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = CircleShape,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
    )
}

/** Circular in-call control (mic / camera / end). */
@Composable
fun CircleControl(
    icon: ImageVector,
    contentDescription: String,
    fill: Color = SurfaceRaised,
    tint: Color = TextPrimary,
    diameter: Int = 64,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(diameter.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = fill, contentColor = tint),
    ) {
        Box { Icon(icon, contentDescription = contentDescription) }
    }
}
