package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.PersianLapis

@DrawableRes
fun resolveBankLogoDrawableRes(bankName: String): Int? {
    val clean = bankName.trim()
    return when {
        clean.contains("بلو") || clean.equals("Blu", ignoreCase = true) -> R.drawable.ic_bank_blu
        clean.contains("سپه") || clean.equals("Sepah", ignoreCase = true) -> R.drawable.ic_bank_sepah
        clean.contains("پاسارگاد") || clean.equals("Pasargad", ignoreCase = true) -> R.drawable.ic_bank_pasargad
        clean.contains("ملت") || clean.equals("Mellat", ignoreCase = true) -> R.drawable.ic_bank_mellat
        clean.contains("ویپاد") || clean.equals("Wepod", ignoreCase = true) -> R.drawable.ic_bank_wepod
        clean.contains("تجارت") || clean.equals("Tejarat", ignoreCase = true) -> R.drawable.ic_bank_tejarat
        clean.contains("ملی") || clean.equals("Melli", ignoreCase = true) -> R.drawable.ic_bank_melli
        else -> null
    }
}

@Composable
fun BankLogoAvatar(
    bankName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val logoRes = resolveBankLogoDrawableRes(bankName)
    if (logoRes != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "لوگوی بانک $bankName",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        val isManualOrCash = bankName.contains("نقدی") || bankName.contains("دستی")
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(PersianLapis)
                .border(1.dp, OrosiHairline, CircleShape)
        ) {
            Icon(
                imageVector = if (isManualOrCash) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                contentDescription = bankName,
                tint = NishapurTurquoiseSoft,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}
