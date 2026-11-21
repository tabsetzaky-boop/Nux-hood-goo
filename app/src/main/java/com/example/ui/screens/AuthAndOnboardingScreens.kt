package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.core.NuxText
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOutUser
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.components.NuxAtomLogo
import com.example.ui.components.PinPadDialog
import com.example.ui.theme.LocalNuxColors
import kotlin.math.roundToInt

@Composable
fun AuthScreen(
    strings: NuxText,
    onAuthenticated: () -> Unit = {},
    onContinueWithoutSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAuto by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthenticated()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope
        )
    }

    ChatBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.widthIn(max = 460.dp),
                cornerRadius = 28.dp,
                strong = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NuxAtomLogo(size = 84.dp, animated = true, withGlow = true)

                    Text(
                        text = "Nux · نوكس",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = strings.appTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (strings.isArabic) {
                            "محادثات سريعة، توليد صور، بناء مواقع وتطبيقات، وذاكرة سحابية مؤمّنة لحسابك."
                        } else {
                            "Fast AI chat, image generation, site & APK studio, and encrypted cloud memory."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .padding(10.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    if (isCheckingAuto || isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = if (strings.isArabic) "جارٍ التحقق من حسابك..." else "Signing you in...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        BrandGradientButton(
                            text = strings.signInWithGoogle,
                            onClick = {
                                errorMessage = null
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthenticated()
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        errorMessage = msg
                                    },
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    },
                                    scope = scope
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button"),
                            height = 50.dp
                        )

                        OutlinedButton(
                            onClick = onContinueWithoutSignIn,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_guest_button")
                        ) {
                            Text(
                                text = strings.continueWithoutSignIn,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Text(
                            text = if (strings.isArabic) {
                                "يمكنك استخدام الدردشة والصور والاستوديو فورًا بدون حساب، والتسجيل لاحقًا لحفظ بياناتك سحابيًا."
                            } else {
                                "Try AI chat, images, and studios right away without an account, or sign in later for cloud sync."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    strings: NuxText,
    onComplete: (gender: String, age: Long) -> Unit
) {
    val nux = LocalNuxColors.current
    var selectedGender by remember { mutableStateOf("unspecified") }
    var ageFloat by remember { mutableFloatStateOf(24f) }
    val roundedAge = ageFloat.roundToInt().toLong().coerceIn(16L, 100L)

    val genderOptions = remember(strings.isArabic) {
        listOf(
            Triple("male", if (strings.isArabic) "ذكر" else "Male", "👨"),
            Triple("female", if (strings.isArabic) "أنثى" else "Female", "👩"),
            Triple("unspecified", if (strings.isArabic) "تفضيل عدم الذكر" else "Prefer not to say", "✨")
        )
    }

    ChatBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                cornerRadius = 28.dp,
                strong = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NuxAtomLogo(size = 64.dp, animated = true, withGlow = true)

                    Text(
                        text = if (strings.isArabic) "مرحبًا بك في Nux" else "Welcome to Nux",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (strings.isArabic) {
                            "خطوة سريعة لتخصيص تجربة الصوت والأسلوب بما يناسبك"
                        } else {
                            "A quick step to personalize voice and assistant tone"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (strings.isArabic) "اختر الجنس" else "Gender",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genderOptions.forEach { (key, label, emoji) ->
                            val selected = selectedGender == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else nux.glassSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else nux.glassBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedGender = key }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isArabic) "العمر" else "Age",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$roundedAge",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = ageFloat,
                        onValueChange = { ageFloat = it },
                        valueRange = 16f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (roundedAge < 18L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "تُطبّق حماية خصوصية إضافية تلقائيًا للفئة العمرية دون 18 عامًا."
                                } else {
                                    "Extra privacy safeguards are automatically enabled for users under 18."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    BrandGradientButton(
                        text = if (strings.isArabic) "بدء استخدام Nux" else "Start using Nux",
                        onClick = { onComplete(selectedGender, roundedAge) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_complete_button"),
                        height = 50.dp
                    )
                }
            }
        }
    }
}

@Composable
fun LoginPinGateScreen(
    strings: NuxText,
    onVerifyPin: (String) -> Boolean,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showPinPad by remember { mutableStateOf(true) }
    var pinError by remember { mutableStateOf<String?>(null) }

    ChatBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.widthIn(max = 420.dp),
                cornerRadius = 28.dp,
                strong = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (strings.isArabic) "التحقق برمز PIN" else "2FA PIN Verification",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (strings.isArabic) {
                            "هذا الجهاز يتطلب إدخال رمز تسجيل الدخول المكون من 6 أرقام للمتابعة."
                        } else {
                            "Enter your 6-digit login PIN to unlock your workspace on this device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    BrandGradientButton(
                        text = if (strings.isArabic) "إدخال رمز PIN" else "Enter PIN",
                        onClick = { showPinPad = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = {
                            signOutUser(credentialManager, scope)
                        }
                    ) {
                        Text(strings.signOut, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPinPad) {
        PinPadDialog(
            title = if (strings.isArabic) "أدخل رمز تسجيل الدخول" else "Enter Login PIN",
            subtitle = if (strings.isArabic) "6 أرقام لحماية حسابك" else "6 digits to unlock account",
            confirmMode = false,
            externalError = pinError,
            onDismiss = { showPinPad = false },
            onPinCompleted = { entered ->
                if (onVerifyPin(entered)) {
                    pinError = null
                    showPinPad = false
                    onVerified()
                } else {
                    pinError = if (strings.isArabic) "رمز PIN غير صحيح" else "Incorrect PIN"
                }
            }
        )
    }
}
