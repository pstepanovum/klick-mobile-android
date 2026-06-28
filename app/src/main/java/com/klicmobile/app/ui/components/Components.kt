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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Fully-rounded flat primary button — no shadow, no border (design rules). */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val resolvedFill = if (fill == Color.Unspecified) MaterialTheme.colorScheme.primary else fill
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedFill,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
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
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor      = Color.Transparent,
            unfocusedBorderColor    = Color.Transparent,
            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** Circular in-call control using a brand Painter icon. */
@Composable
fun CircleControl(
    painter: Painter,
    contentDescription: String,
    fill: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
    diameter: Int = 64,
    onClick: () -> Unit,
) {
    val resolvedFill = if (fill == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant else fill
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(diameter.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = resolvedFill,
            contentColor   = resolvedTint,
        ),
    ) {
        Box {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size((diameter * 0.44f).dp),
            )
        }
    }
}
