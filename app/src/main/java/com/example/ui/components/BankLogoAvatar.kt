package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.PersianLapis
import java.util.zip.ZipInputStream

/**
 * In-memory cache that scans `context.assets.list("")` on startup for any `.zip` or `.png` files,
 * reads all `ic_bank_*.png` entries via `ZipInputStream` + `BitmapFactory.decodeStream`,
 * and caches them as `Map<String, ImageBitmap>` keyed by resource name without extension (e.g. `"ic_bank_blu"`).
 */
object JibemanAssetLogoCache {
    @Volatile
    private var cachedLogos: Map<String, ImageBitmap>? = null

    val ALL_38_IRANIAN_BANKS: List<Pair<String, String>> = listOf(
        "بلو" to "ic_bank_blu",
        "سپه" to "ic_bank_sepah",
        "پاسارگاد" to "ic_bank_pasargad",
        "ملت" to "ic_bank_mellat",
        "ویپاد" to "ic_bank_wepod",
        "ملی ایران" to "ic_bank_melli",
        "تجارت" to "ic_bank_tejarat",
        "صادرات ایران" to "ic_bank_saderat",
        "رفاه کارگران" to "ic_bank_refah",
        "سامان" to "ic_bank_saman",
        "پارسیان" to "ic_bank_parsian",
        "کشاورزی" to "ic_bank_keshavarzi",
        "مسکن" to "ic_bank_maskan",
        "قرض‌الحسنه رسالت" to "ic_bank_resalat",
        "قرض‌الحسنه مهر ایران" to "ic_bank_mehr_iran",
        "گردشگری" to "ic_bank_gardeshgari",
        "اقتصاد نوین" to "ic_bank_eghtesad_novin",
        "شهر" to "ic_bank_shahr",
        "سینا" to "ic_bank_sina",
        "سرمایه" to "ic_bank_sarmayeh",
        "کارآفرین" to "ic_bank_karafarin",
        "پست ایران" to "ic_bank_post",
        "دی" to "ic_bank_dey",
        "خاورمیانه" to "ic_bank_khavar_mianeh",
        "ایران زمین" to "ic_bank_iran_zamin",
        "توسعه تعاون" to "ic_bank_tosee_taavon",
        "بانکینو" to "ic_bank_bankino",
        "انصار" to "ic_bank_ansar",
        "قوامین" to "ic_bank_ghavamin",
        "حکمت ایرانیان" to "ic_bank_hekmat",
        "ایران ونزوئلا" to "ic_bank_iran_venezuela",
        "کوثر" to "ic_bank_kosar",
        "مهر اقتصاد" to "ic_bank_mehr_eghtesad",
        "مؤسسه اعتباری ملل" to "ic_bank_melall",
        "مؤسسه اعتباری نور" to "ic_bank_noor",
        "صنعت و معدن" to "ic_bank_sanat_madan",
        "مؤسسه اعتباری توسعه" to "ic_bank_tosee",
        "توسعه صادرات" to "ic_bank_tosee_saderat"
    )

    fun ensureLoaded(context: Context): Map<String, ImageBitmap> {
        cachedLogos?.let { if (it.isNotEmpty()) return it }
        synchronized(this) {
            cachedLogos?.let { if (it.isNotEmpty()) return it }
            val result = LinkedHashMap<String, ImageBitmap>()
            try {
                val assetFiles = context.assets.list("") ?: emptyArray()
                // 1. Scan any .zip files in assets (e.g. jibeman_android_drawables_*.zip)
                for (fileName in assetFiles) {
                    if (fileName.endsWith(".zip", ignoreCase = true)) {
                        try {
                            context.assets.open(fileName).use { rawStream ->
                                ZipInputStream(rawStream).use { zis ->
                                    var entry = zis.nextEntry
                                    while (entry != null) {
                                        val entryBaseName = entry.name.substringAfterLast('/')
                                        if (!entry.isDirectory &&
                                            entryBaseName.startsWith("ic_bank_") &&
                                            entryBaseName.endsWith(".png", ignoreCase = true)
                                        ) {
                                            val key = entryBaseName.substringBeforeLast('.')
                                            val bytes = zis.readBytes()
                                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bmp != null) {
                                                result[key] = bmp.asImageBitmap()
                                            }
                                        }
                                        zis.closeEntry()
                                        entry = zis.nextEntry
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    } else if (fileName.startsWith("ic_bank_") && fileName.endsWith(".png", ignoreCase = true)) {
                        try {
                            val key = fileName.substringBeforeLast('.')
                            context.assets.open(fileName).use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) {
                                    result[key] = bmp.asImageBitmap()
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
            cachedLogos = result
            return result
        }
    }

    fun resolveBankLogoKey(bankName: String): String? {
        val clean = PersianNumberUtils.normalizePersianText(bankName)
            .replace("بانک", "")
            .replace("کارت", "")
            .replace("حساب", "")
            .trim()
        if (clean.isBlank()) return null

        if (clean.startsWith("ic_bank_")) return clean

        return when {
            clean.contains("بلو") || clean.equals("Blu", ignoreCase = true) -> "ic_bank_blu"
            clean.contains("سپه") || clean.equals("Sepah", ignoreCase = true) -> "ic_bank_sepah"
            clean.contains("پاسارگاد") || clean.equals("Pasargad", ignoreCase = true) -> "ic_bank_pasargad"
            clean.contains("ملت") || clean.equals("Mellat", ignoreCase = true) -> "ic_bank_mellat"
            clean.contains("ویپاد") || clean.equals("Wepod", ignoreCase = true) -> "ic_bank_wepod"
            clean.contains("ملی") || clean.equals("Melli", ignoreCase = true) -> "ic_bank_melli"
            clean.contains("تجارت") || clean.equals("Tejarat", ignoreCase = true) -> "ic_bank_tejarat"
            clean.contains("صادرات") && clean.contains("توسعه") -> "ic_bank_tosee_saderat"
            clean.contains("صادرات") || clean.equals("Saderat", ignoreCase = true) -> "ic_bank_saderat"
            clean.contains("رفاه") || clean.equals("Refah", ignoreCase = true) -> "ic_bank_refah"
            clean.contains("سامان") || clean.equals("Saman", ignoreCase = true) -> "ic_bank_saman"
            clean.contains("پارسیان") || clean.equals("Parsian", ignoreCase = true) -> "ic_bank_parsian"
            clean.contains("کشاورزی") || clean.equals("Keshavarzi", ignoreCase = true) -> "ic_bank_keshavarzi"
            clean.contains("مسکن") || clean.equals("Maskan", ignoreCase = true) -> "ic_bank_maskan"
            clean.contains("رسالت") || clean.equals("Resalat", ignoreCase = true) -> "ic_bank_resalat"
            clean.contains("مهر ایران") || clean.equals("Mehr Iran", ignoreCase = true) -> "ic_bank_mehr_iran"
            clean.contains("مهر اقتصاد") || clean.equals("Mehr Eghtesad", ignoreCase = true) -> "ic_bank_mehr_eghtesad"
            clean.contains("گردشگری") || clean.equals("Tourism", ignoreCase = true) -> "ic_bank_gardeshgari"
            clean.contains("اقتصاد نوین") || clean.equals("Eghtesad Novin", ignoreCase = true) -> "ic_bank_eghtesad_novin"
            clean.contains("شهر") || clean.equals("Shahr", ignoreCase = true) -> "ic_bank_shahr"
            clean.contains("سینا") || clean.equals("Sina", ignoreCase = true) -> "ic_bank_sina"
            clean.contains("سرمایه") || clean.equals("Sarmayeh", ignoreCase = true) -> "ic_bank_sarmayeh"
            clean.contains("کارآفرین") || clean.contains("کارافرین") || clean.equals("Karafarin", ignoreCase = true) -> "ic_bank_karafarin"
            clean.contains("پست") || clean.equals("Post", ignoreCase = true) -> "ic_bank_post"
            clean == "دی" || clean.contains("بانک دی") || clean.equals("Dey", ignoreCase = true) || clean.equals("Day", ignoreCase = true) -> "ic_bank_dey"
            clean.contains("خاورمیانه") || clean.equals("Middle East", ignoreCase = true) -> "ic_bank_khavar_mianeh"
            clean.contains("ایران زمین") || clean.contains("ایران‌زمین") || clean.equals("Iran Zamin", ignoreCase = true) -> "ic_bank_iran_zamin"
            clean.contains("توسعه تعاون") || clean.equals("Tosee Taavon", ignoreCase = true) -> "ic_bank_tosee_taavon"
            clean.contains("بانکینو") || clean.equals("Bankino", ignoreCase = true) -> "ic_bank_bankino"
            clean.contains("انصار") || clean.equals("Ansar", ignoreCase = true) -> "ic_bank_ansar"
            clean.contains("قوامین") || clean.equals("Ghavamin", ignoreCase = true) -> "ic_bank_ghavamin"
            clean.contains("حکمت") || clean.equals("Hekmat", ignoreCase = true) -> "ic_bank_hekmat"
            clean.contains("ونزوئلا") || clean.equals("Iran Venezuela", ignoreCase = true) -> "ic_bank_iran_venezuela"
            clean.contains("کوثر") || clean.equals("Kosar", ignoreCase = true) -> "ic_bank_kosar"
            clean.contains("ملل") || clean.equals("Melal", ignoreCase = true) -> "ic_bank_melall"
            clean.contains("نور") || clean.equals("Noor", ignoreCase = true) -> "ic_bank_noor"
            clean.contains("صنعت و معدن") || clean.equals("Industry & Mine", ignoreCase = true) -> "ic_bank_sanat_madan"
            clean.contains("توسعه") -> "ic_bank_tosee"
            else -> null
        }
    }
}

@DrawableRes
fun resolveBankLogoDrawableRes(bankName: String): Int? {
    return when (JibemanAssetLogoCache.resolveBankLogoKey(bankName)) {
        "ic_bank_blu" -> R.drawable.ic_bank_blu
        "ic_bank_sepah" -> R.drawable.ic_bank_sepah
        "ic_bank_pasargad" -> R.drawable.ic_bank_pasargad
        "ic_bank_mellat" -> R.drawable.ic_bank_mellat
        "ic_bank_wepod" -> R.drawable.ic_bank_wepod
        "ic_bank_tejarat" -> R.drawable.ic_bank_tejarat
        "ic_bank_melli" -> R.drawable.ic_bank_melli
        else -> null
    }
}

@Composable
fun BankLogoAvatar(
    bankName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    val logoKey = remember(bankName) { JibemanAssetLogoCache.resolveBankLogoKey(bankName) }
    val pngBitmap: ImageBitmap? = remember(logoKey, context) {
        if (logoKey == null) null else JibemanAssetLogoCache.ensureLoaded(context)[logoKey]
    }

    if (pngBitmap != null) {
        // PNGs in the Jibeman ZIP already have their own 192x192 circular frame and white background
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size)
        ) {
            Image(
                bitmap = pngBitmap,
                contentDescription = "لوگوی بانک $bankName",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        val fallbackVectorRes = resolveBankLogoDrawableRes(bankName)
        if (fallbackVectorRes != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = fallbackVectorRes),
                    contentDescription = "لوگوی بانک $bankName",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            val isManualOrCash = bankName.contains("نقدی") || bankName.contains("دستی") || bankName.contains("کیف پول")
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
}
