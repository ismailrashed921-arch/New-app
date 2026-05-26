package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Order
import com.example.model.OrderItem
import com.example.viewmodel.ActiveTab
import com.example.viewmodel.LoginState
import com.example.viewmodel.RiderViewModel
import com.example.viewmodel.SignUpState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

// Premium Delivery Startup Colors
private val BrandPrimary = Color(0xFF00C896)       // Vibrant Premium Green
private val BrandDark = Color(0xFF0F172A)          // Slate 900 / Deep Slate
private val BrandLightSlate = Color(0xFFF1F5F9)     // Slate 100 for subtle layers
private val BrandBackground = Color(0xFFF8FAFC)     // Main app background
private val BrandAccent = Color(0xFF14B8A6)         // Cool Teal
private val BrandSecondaryText = Color(0xFF64748B)  // Muted body text
private val BrandBorder = Color(0xFFE2E8F0)         // Slate 200 border
private val BrandWarning = Color(0xFFF59E0B)        // Amber/Orange status
private val BrandDanger = Color(0xFFEF4444)         // Pastel Coral Red

private fun getDynamicGreeting(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    return "$greeting, $name 👋"
}

@Composable
fun RiderAppUi(viewModel: RiderViewModel) {
    val currentRiderState by viewModel.currentRider.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BrandBackground)) {
        when {
            isInitializing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPrimary)
                }
            }
            currentRiderState == null -> {
                AuthScreen(viewModel)
            }
            currentRiderState?.status == "pending" -> {
                PendingScreen(viewModel)
            }
            else -> {
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: RiderViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    AnimatedContent(
        targetState = isSignUp,
        transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
        label = "auth_swap"
    ) { showSignUp ->
        if (showSignUp) {
            SignUpScreen(viewModel, onNavigateToLogin = { isSignUp = false })
        } else {
            LoginScreen(viewModel, onNavigateToSignUp = { isSignUp = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: RiderViewModel, onNavigateToSignUp: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Error) {
            Toast.makeText(context, (loginState as LoginState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(containerColor = BrandBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrandBorder, RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = "App Icon",
                            tint = BrandPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ki-Lagbe Rider",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = BrandDark
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = BrandPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandBorder,
                            focusedLabelColor = BrandPrimary,
                            unfocusedLabelColor = BrandSecondaryText
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Secret 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = BrandPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandBorder,
                            focusedLabelColor = BrandPrimary,
                            unfocusedLabelColor = BrandSecondaryText
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )

                    Button(
                        onClick = {
                            if (phone.isEmpty() || pin.isEmpty()) {
                                Toast.makeText(context, "Please enter Phone and PIN!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.login(context, phone, pin)
                            }
                        },
                        enabled = loginState !is LoginState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Log In Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.clickable { onNavigateToSignUp() }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Apply as New Partner", color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(viewModel: RiderViewModel, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var nidFrontUri by remember { mutableStateOf<Uri?>(null) }
    var nidBackUri by remember { mutableStateOf<Uri?>(null) }
    var areaMenuExpanded by remember { mutableStateOf(false) }

    val locationsState by viewModel.locations.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }
    val nidFrontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { nidFrontUri = it }
    val nidBackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { nidBackUri = it }

    LaunchedEffect(signUpState) {
        if (signUpState is SignUpState.Success) {
            Toast.makeText(context, "Application submitted successfully!", Toast.LENGTH_LONG).show()
            onNavigateToLogin()
        } else if (signUpState is SignUpState.Error) {
            Toast.makeText(context, (signUpState as SignUpState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(containerColor = BrandBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Partner Application",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = BrandDark
            )
            Text(
                text = "Complete your info to start earning",
                fontSize = 14.sp,
                color = BrandSecondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (as in NID)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = BrandPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandBorder),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Active Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = BrandPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandBorder),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Create 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = BrandPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandBorder),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        OutlinedTextField(
                            value = selectedArea,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Working Area") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = BrandPrimary) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = BrandPrimary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandBorder),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { areaMenuExpanded = true })
                        DropdownMenu(
                            expanded = areaMenuExpanded,
                            onDismissRequest = { areaMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                        ) {
                            locationsState.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedArea = loc
                                        areaMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selfie Image Clicker
                    Text("YOUR CLEAR SELFIE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                            .clickable { photoLauncher.launch("image/*") }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (photoUri != null) "Selfie Selected ✅" else "Tap to choose image file",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (photoUri != null) BrandPrimary else BrandSecondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("NID DOCUMENTS UPWARD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText, modifier = Modifier.padding(bottom = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                                .clickable { nidFrontLauncher.launch("image/*") }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (nidFrontUri != null) "Front ✅" else "NID Front Side",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (nidFrontUri != null) BrandPrimary else BrandSecondaryText
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                                .clickable { nidBackLauncher.launch("image/*") }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (nidBackUri != null) "Back ✅" else "NID Back Side",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (nidBackUri != null) BrandPrimary else BrandSecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.signUp(context, name, phone, pin, selectedArea, photoUri, nidFrontUri, nidBackUri)
                        },
                        enabled = signUpState !is SignUpState.Progress,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (signUpState is SignUpState.Progress) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text((signUpState as SignUpState.Progress).message, fontSize = 14.sp)
                            }
                        } else {
                            Text("Submit Secure Application", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Already submitted? Log in here",
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onNavigateToLogin() }.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PendingScreen(viewModel: RiderViewModel) {
    val context = LocalContext.current
    Scaffold(containerColor = BrandBackground) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp).clip(CircleShape).background(BrandWarning.copy(alpha = 0.12f))
            ) {
                Icon(Icons.Default.HourglassEmpty, null, tint = BrandWarning, modifier = Modifier.size(54.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Verification In Progress", fontWeight = FontWeight.Black, fontSize = 24.sp, color = BrandDark)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your agent profile is successfully filed and currently being thoroughly audited by administration. We'll send you an instant SMS confirmation standard.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = BrandSecondaryText,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = { viewModel.logout(context) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandDanger),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(220.dp).height(50.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel & Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun OnlineOfflinePill(
    viewModel: RiderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val backgroundColor = if (isOnline) Color(0xFFEFFFF4) else Color(0xFFFFF0F0)
    val borderColor = if (isOnline) BrandPrimary.copy(alpha = 0.4f) else BrandDanger.copy(alpha = 0.4f)
    val dotColor = if (isOnline) BrandPrimary else BrandDanger
    val textColor = if (isOnline) BrandPrimary else BrandDanger
    val textLabel = if (isOnline) "ONLINE" else "OFFLINE"

    val dotAlpha = if (isOnline) {
        val infiniteTransition = rememberInfiniteTransition(label = "top_bar_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        ).value
    } else {
        1.0f
    }

    Card(
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable { viewModel.setOnlineStatus(!isOnline, context) }
            .border(1.dp, borderColor, RoundedCornerShape(50.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )
            Text(
                text = textLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 0.5.sp
            )
            // Custom switch track and thumb
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOnline) BrandPrimary.copy(alpha = 0.2f) else BrandDanger.copy(alpha = 0.2f))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) BrandPrimary else BrandDanger)
                        .align(if (isOnline) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }
        }
    }
}

@Composable
fun RiderTopBar(
    title: String,
    viewModel: RiderViewModel,
    onMenuClick: () -> Unit,
    showOnlineToggle: Boolean = true,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Menu,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.08f))
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigate",
                        tint = BrandDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (title == "Ki-Lagbe Rider") {
                    Text("Ki-Lagbe", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BrandDark)
                    Text(" Rider", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BrandPrimary)
                } else {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = BrandDark)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showOnlineToggle) {
                    OnlineOfflinePill(viewModel = viewModel)
                }
                
                actions?.invoke(this)
            }
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: RiderViewModel, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.White,
        topBar = {
            RiderTopBar(title = "Ki-Lagbe Rider", viewModel = viewModel, onMenuClick = onMenuClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1.2f)) {
                DeliveryScooterCanvas()
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("You are currently OFFLINE", fontWeight = FontWeight.Black, fontSize = 22.sp, color = BrandDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Go online to start receiving orders securely around your dynamic region.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = BrandSecondaryText,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = { viewModel.setOnlineStatus(true, context) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(18.dp), spotColor = BrandPrimary)
            ) {
                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Go Online Now", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun MainAppScreen(viewModel: RiderViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val riderDetail by viewModel.currentRider.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth().statusBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(colors = listOf(BrandPrimary, Color(0xFF00C896))))
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(riderDetail?.name?.ifBlank { "Ki-Lagbe Rider" } ?: "Ki-Lagbe Rider", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isOnline) Color(0xFF4ADE80) else Color(0xFFF87171)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isOnline) "ONLINE" else "OFFLINE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Icon(Icons.Default.TwoWheeler, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val menuOptions = listOf(
                        Triple("Home", Icons.Default.GridView, ActiveTab.Home),
                        Triple("My Performance", Icons.Default.AccountBalance, ActiveTab.Wallet),
                        Triple("My Orders", Icons.Default.ListAlt, ActiveTab.Orders),
                        Triple("Profile", Icons.Default.Person, ActiveTab.Profile)
                    )

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        menuOptions.forEach { (label, icon, tab) ->
                            val isSelected = activeTab == tab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrandPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.activeTab.value = tab
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = if (isSelected) BrandPrimary else BrandSecondaryText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, color = if (isSelected) BrandPrimary else BrandDark, fontSize = 14.sp)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = BrandBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    try {
                                        val m = "Support inquiry from rider: ${riderDetail?.name ?: ""}"
                                        val u = "https://wa.me/8801642912431?text=" + URLEncoder.encode(m, "UTF-8")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HelpOutline, null, tint = BrandSecondaryText, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Help & Support", fontWeight = FontWeight.Bold, color = BrandDark, fontSize = 14.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                viewModel.logout(context)
                            }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, null, tint = BrandDanger, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Logout Partner", fontWeight = FontWeight.Black, color = BrandDanger, fontSize = 14.sp)
                    }
                }
            }
        },
        gesturesEnabled = false
    ) {
        Scaffold(
            containerColor = BrandBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).shadow(12.dp, spotColor = BrandDark.copy(alpha = 0.05f))
                ) {
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Home,
                        onClick = { viewModel.activeTab.value = ActiveTab.Home },
                        icon = { Icon(if (activeTab == ActiveTab.Home) Icons.Filled.GridView else Icons.Outlined.GridView, null) },
                        label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = BrandPrimary,
                            unselectedIconColor = BrandSecondaryText,
                            unselectedTextColor = BrandSecondaryText
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Wallet,
                        onClick = { viewModel.activeTab.value = ActiveTab.Wallet },
                        icon = { Icon(if (activeTab == ActiveTab.Wallet) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance, null) },
                        label = { Text("Performance", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = BrandPrimary,
                            unselectedIconColor = BrandSecondaryText,
                            unselectedTextColor = BrandSecondaryText
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Orders,
                        onClick = { viewModel.activeTab.value = ActiveTab.Orders },
                        icon = { Icon(if (activeTab == ActiveTab.Orders) Icons.Filled.ListAlt else Icons.Outlined.ListAlt, null) },
                        label = { Text("Orders", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = BrandPrimary,
                            unselectedIconColor = BrandSecondaryText,
                            unselectedTextColor = BrandSecondaryText
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Profile,
                        onClick = { viewModel.activeTab.value = ActiveTab.Profile },
                        icon = { Icon(if (activeTab == ActiveTab.Profile) Icons.Filled.Person else Icons.Outlined.Person, null) },
                        label = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = BrandPrimary,
                            unselectedIconColor = BrandSecondaryText,
                            unselectedTextColor = BrandSecondaryText
                        )
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (activeTab) {
                    ActiveTab.Home -> HomeTab(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                    ActiveTab.Orders -> OrdersTab(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                    ActiveTab.Wallet -> WalletTab(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                    ActiveTab.Profile -> ProfileTab(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                }
            }
        }
    }
}

@Composable
fun HomeTab(viewModel: RiderViewModel, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val currentActiveOrderId by viewModel.currentActiveOrderId.collectAsState()
    val riderDetail by viewModel.currentRider.collectAsState()

    // Dynamic Wallet & Performance stats
    val earnings by viewModel.walletEarnings.collectAsState()
    val countDel by viewModel.walletDeliveredCount.collectAsState()
    val countCan by viewModel.walletCancelledCount.collectAsState()
    val allOrders by viewModel.allOrdersList.collectAsState()

    // Local override state to toggle between Active Tracker and Dashboard
    var showTrackingScreen by remember(activeOrders.isEmpty()) {
        mutableStateOf(activeOrders.isNotEmpty())
    }

    val recentActivities = remember(allOrders) {
        allOrders.sortedByDescending { if (it.deliveredAt > 0) it.deliveredAt else it.time }.take(3)
    }

    // 1. OFFLINE WELCOME VIEW INTEGRATION
    if (!isOnline && activeOrders.isEmpty()) {
        WelcomeScreen(viewModel, onMenuClick)
        return
    }

    // 2. ACTIVE ORDER SCREEN LOGIC
    if (activeOrders.isNotEmpty() && showTrackingScreen) {
        val activeOrder = activeOrders.find { it.id == currentActiveOrderId } ?: activeOrders.first()
        var actionProgressing by remember { mutableStateOf(false) }

        if (activeOrder.status == "Assigned" || activeOrder.status == "Pending") {
            // Screen 1: New Instant Order Request Layout
            Scaffold(
                containerColor = BrandBackground,
                topBar = {
                    RiderTopBar(title = "Ki-Lagbe Rider", viewModel = viewModel, onMenuClick = onMenuClick)
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Good Morning Rased Green Card Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(getDynamicGreeting(riderDetail?.name ?: "Partner"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Stay safe. Deliver smile 😉", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                }
                            }
                        }
                    }

                    // Main Order Card Details
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Instant Order Queue", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrandDark)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("#${getOrderIdDisplay(activeOrder)}", fontWeight = FontWeight.Black, color = Color(0xFFD97706), fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                // User details row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CUSTOMER DETAILS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text(activeOrder.name, fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDark)
                                        Text(activeOrder.phone, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = BrandPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${activeOrder.phone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Dialer failed to initiate", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.background(BrandPrimary.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Call, null, tint = BrandPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dropoff target location
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, tint = BrandDanger, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("DELIVERY TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text(activeOrder.address.ifEmpty { activeOrder.area }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                    }
                                }

                                if (activeOrder.note.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFFFBEB))
                                            .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text("Rider Instruction: ${activeOrder.note}", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Table Items
                                Text("OUTLET DETAILS & PRODUCT PARCEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                activeOrder.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.qty}x ${item.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                        Text(item.source, fontSize = 11.sp, color = BrandSecondaryText, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("COLLECT FROM CUSTOMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text("Payable COD Cash", fontSize = 11.sp, color = BrandDanger, fontWeight = FontWeight.Bold)
                                    }
                                    Text("৳${activeOrder.total.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandDark)
                                }
                            }
                        }
                    }

                    // Bottom Action Panel
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.acceptOrder(activeOrder.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(54.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = BrandPrimary)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ACCEPT ORDER", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else {
            // Screen 4: Accepted / In-Progress Order Detailed Screen
            Scaffold(
                containerColor = BrandBackground,
                topBar = {
                    RiderTopBar(
                        title = "Delivery Tracking",
                        viewModel = viewModel,
                        onMenuClick = { showTrackingScreen = false },
                        showOnlineToggle = false,
                        navigationIcon = Icons.Default.ArrowBack,
                        actions = {
                            IconButton(
                                onClick = {
                                    try {
                                        val m = "Support needed for ongoing delivery #${getOrderIdDisplay(activeOrder)}"
                                        val u = "https://wa.me/8801642912431?text=" + URLEncoder.encode(m, "UTF-8")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.SupportAgent, null, tint = BrandPrimary)
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("ORDER IDENTIFIER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text("#${getOrderIdDisplay(activeOrder)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDark)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(activeOrder.status.uppercase(), fontWeight = FontWeight.Black, color = BrandPrimary, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("RECIPIENT CUSTOMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text(activeOrder.name, fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDark)
                                        Text(activeOrder.phone, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${activeOrder.phone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.background(BrandPrimary.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Call, null, tint = BrandPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, tint = BrandDanger, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("DELIVERY PLACE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text(activeOrder.address.ifEmpty { activeOrder.area }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                    }
                                }
                            }
                        }
                    }

                    // Title for product items list
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ORDERED PRODUCTS (${activeOrder.items.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandDark
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Loop through activeOrder.items and display each product beautifully with photo and name
                    items(activeOrder.items) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Image (Photo)
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandLightSlate)
                                        .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.img.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.img,
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Good looking fallback icon
                                        Icon(
                                            imageVector = Icons.Default.Fastfood,
                                            contentDescription = null,
                                            tint = BrandSecondaryText.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Product info
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = BrandDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (item.variant.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BrandLightSlate)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.variant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandSecondaryText
                                            )
                                        }
                                    }

                                    if (item.source.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = BrandPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.source,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Quantity and Price details
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandPrimary.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${item.qty}x",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "৳${item.buy * item.qty}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BrandDark
                                    )
                                }
                            }
                        }
                    }

                    // Customer Bill Invoice details below products list
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "CUSTOMER BILL DETAILS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandSecondaryText,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val computedSubtotal = if (activeOrder.subtotal > 0.0) activeOrder.subtotal else (activeOrder.total - activeOrder.deliveryFee - activeOrder.surcharge - activeOrder.handlingFee).coerceAtLeast(0.0)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Products Subtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                    Text("৳${computedSubtotal.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Delivery Charge", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                    Text("৳${activeOrder.deliveryFee.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark)
                                }

                                if (activeOrder.handlingFee > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Handling Fee", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                        Text("৳${activeOrder.handlingFee.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark)
                                    }
                                }

                                if (activeOrder.surcharge > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Surcharge", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                        Text("৳${activeOrder.surcharge.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CASH TO COLLECT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandSecondaryText)
                                        Text("Collect from Customer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandDanger)
                                    }
                                    Text("৳${activeOrder.total.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandDanger)
                                }
                            }
                        }
                    }

                    // Action buttons segment
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val nextState = when (activeOrder.status) {
                                        "Assigned", "Accepted", "Pending" -> "Processing"
                                        "Processing" -> "On the Way"
                                        "On the Way" -> "Delivered"
                                        else -> ""
                                    }
                                    if (nextState.isNotEmpty()) {
                                        if (nextState == "Delivered") {
                                            actionProgressing = true
                                        } else {
                                            viewModel.updateStatus(activeOrder.id, nextState) { }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (activeOrder.status) {
                                        "Assigned", "Accepted", "Pending" -> BrandPrimary
                                        "Processing" -> Color(0xFFFFB000)
                                        else -> BrandPrimary
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(54.dp).shadow(6.dp, RoundedCornerShape(16.dp))
                            ) {
                                val textLabel = when (activeOrder.status) {
                                    "Assigned", "Accepted", "Pending" -> "ARRIVED AT SHOP"
                                    "Processing" -> "DEPARTED / ON THE WAY"
                                    "On the Way" -> "SUCCESSFULLY DELIVERED"
                                    else -> "PROCEED"
                                }
                                Text(textLabel, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

                if (actionProgressing) {
                    AlertDialog(
                        onDismissRequest = { actionProgressing = false },
                        title = { Text("Complete Handover?", fontWeight = FontWeight.Black, color = BrandDark) },
                        text = { Text("Are you sure you hand-delivered all items safely and collected Cash on Delivery?") },
                        confirmButton = {
                            TextButton(onClick = {
                                actionProgressing = false
                                viewModel.updateStatus(activeOrder.id, "Delivered") { success ->
                                    if (success) {
                                        Toast.makeText(context, "Order successfully localized!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("CONFIRM HANDOVER", color = BrandPrimary, fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { actionProgressing = false }) {
                                Text("CANCEL", color = BrandSecondaryText)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        return
    }

    // 3. SCREEN 2: STANDALONE CLEAN HOME TAB (WAITING FOR ORDERS OR SHOWING ALL ACTIVE ORDERS)
    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            RiderTopBar(title = "Ki-Lagbe Rider", viewModel = viewModel, onMenuClick = onMenuClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Good Morning / Hi MD Rased Green Card Banner (ALWAYS SHOWN BENEATH HEADER)
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(getDynamicGreeting(riderDetail?.name ?: "Partner"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Stay safe. Deliver smile 😉", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            if (activeOrders.isEmpty()) {
                val homeInfiniteTransition = rememberInfiniteTransition(label = "home_pulse")
                val radarScale1 by homeInfiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "radarScale1"
                )
                val radarScale2 by homeInfiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "radarScale2"
                )
                val radarScale3 by homeInfiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "radarScale3"
                )
                val homeDotAlpha by homeInfiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(850, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "homeDotAlpha"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulse radar style circle layout
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(150.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .graphicsLayer {
                                    scaleX = radarScale1
                                    scaleY = radarScale1
                                }
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.08f))
                        )
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .graphicsLayer {
                                    scaleX = radarScale2
                                    scaleY = radarScale2
                                }
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .graphicsLayer {
                                    scaleX = radarScale3
                                    scaleY = radarScale3
                                }
                                .clip(CircleShape)
                                .background(BrandPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = homeDotAlpha))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Waiting for new orders...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandDark,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Seeking incoming delivery requests automatically. Keep your internet stable to receive nearby orders.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                ) {
                    item {
                        Text(
                            text = "ACTIVE ASSIGNED ORDERS (${activeOrders.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandSecondaryText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                items(activeOrders) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                viewModel.currentActiveOrderId.value = order.id
                                showTrackingScreen = true
                            }
                            .border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Header row: ID and Status badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("#${getOrderIdDisplay(order)}", fontWeight = FontWeight.Black, color = Color(0xFFD97706), fontSize = 11.sp)
                                }
                                
                                // Status Badge
                                val isNew = order.status == "Assigned" || order.status == "Pending"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isNew) BrandDanger.copy(alpha = 0.1f) else BrandPrimary.copy(alpha = 0.1f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = order.status.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNew) BrandDanger else BrandPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BrandBorder)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Customer details row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CUSTOMER DETAILS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                    Text(order.name, fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDark)
                                    Text(order.phone, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = BrandPrimary)
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Dialer failed to initiate", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.background(BrandPrimary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Call, null, tint = BrandPrimary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Dropoff location row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, tint = BrandDanger, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("DELIVERY TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                    Text(order.address.ifEmpty { order.area }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                }
                            }
                            
                            // Products count or summary
                            Spacer(modifier = Modifier.height(12.dp))
                            val itemsCount = order.items.sumOf { it.qty }
                            val itemsSummary = order.items.joinToString { "${it.qty}x ${it.name}" }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("ITEMS ($itemsCount)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                    Text(itemsSummary, fontSize = 12.sp, color = BrandDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = BrandBorder)
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Pay row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("COLLECT FROM CUSTOMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                    Text("Payable COD Cash", fontSize = 11.sp, color = BrandDanger, fontWeight = FontWeight.Bold)
                                }
                                Text("৳${order.total.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandDark)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Row of buttons for actions
                            if (order.status == "Assigned" || order.status == "Pending") {
                                Button(
                                    onClick = { viewModel.acceptOrder(order.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ACCEPT ORDER", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.currentActiveOrderId.value = order.id
                                        showTrackingScreen = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TRACK & UPDATE STATUS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun OrdersTab(viewModel: RiderViewModel, onMenuClick: () -> Unit) {
    val allOrders by viewModel.allOrdersList.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var activeOrderDetails by remember { mutableStateOf<Order?>(null) }

    val filtered = remember(allOrders, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> allOrders.filter { it.status in listOf("Assigned", "Accepted", "Pending", "Processing", "On the Way") }
            "Completed" -> allOrders.filter { it.status == "Delivered" }
            "Cancelled" -> allOrders.filter { it.status == "Cancelled" }
            else -> allOrders
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            RiderTopBar(
                title = "My Deliveries",
                viewModel = viewModel,
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, null, tint = BrandDark, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Sleeker Chip Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf("All", "Pending", "Completed", "Cancelled")
                chips.forEach { chip ->
                    val isSelected = selectedFilter == chip
                    val containerBg = if (isSelected) BrandDark else Color.White
                    val textFg = if (isSelected) Color.White else BrandSecondaryText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerBg)
                            .border(1.dp, if (isSelected) BrandDark else BrandBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = chip }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(chip, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = textFg)
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassDisabled, null, tint = BrandSecondaryText.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No deliveries found", fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { order ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BrandBorder, RoundedCornerShape(18.dp))
                                .clickable { activeOrderDetails = order }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(BrandPrimary.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.TwoWheeler, null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Order ID #${getOrderIdDisplay(order)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BrandDark)
                                            val orderDateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.time))
                                            Text(orderDateStr, fontSize = 10.sp, color = BrandSecondaryText, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (order.status) {
                                                    "Delivered" -> BrandPrimary.copy(alpha = 0.12f)
                                                    "Cancelled" -> BrandDanger.copy(alpha = 0.12f)
                                                    else -> BrandWarning.copy(alpha = 0.12f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val badgeColor = when (order.status) {
                                            "Delivered" -> BrandPrimary
                                            "Cancelled" -> BrandDanger
                                            else -> BrandWarning
                                        }
                                        Text(order.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = badgeColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = order.address.ifEmpty { order.area },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandDark,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = BrandBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CASH ON ARRIVAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                        Text("৳${order.total.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = BrandDark)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Details", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForwardIos, null, tint = BrandPrimary, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        activeOrderDetails?.let { order ->
            HistoryStoryDialog(order = order, onDismiss = { activeOrderDetails = null })
        }
    }
}

@Composable
fun WalletTab(viewModel: RiderViewModel, onMenuClick: () -> Unit) {
    val earnings by viewModel.walletEarnings.collectAsState()
    val collected by viewModel.walletCollected.collectAsState()
    val countDel by viewModel.walletDeliveredCount.collectAsState()
    val countCan by viewModel.walletCancelledCount.collectAsState()
    val dueAmount by viewModel.walletDueAmount.collectAsState()
    val historyOrders by viewModel.transactionHistory.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()

    var activeLedgerDetails by remember { mutableStateOf<Order?>(null) }
    val context = LocalContext.current

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            RiderTopBar(
                title = "My Performance",
                viewModel = viewModel,
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Ledger synchronized", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Refresh, null, tint = BrandDark, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Dropdowns for time selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("today" to "Today", "yesterday" to "Yesterday", "week" to "Last 7 Days")
                    filters.forEach { (tag, label) ->
                        val isSel = filter == tag
                        val bg = if (isSel) BrandDark else Color.White
                        val fg = if (isSel) Color.White else BrandSecondaryText
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bg)
                                .border(1.dp, if (isSel) BrandDark else BrandBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.setFilter(tag) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = fg)
                        }
                    }
                }
            }

            // Modern Balance Wallet Gradient Card
            item {
                val gradient = Brush.linearGradient(colors = listOf(BrandPrimary, BrandAccent))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = BrandPrimary.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(gradient).padding(24.dp)) {
                        Column {
                            Text("TOTAL EARNING BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.82f), letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("৳${earnings.toInt()}", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Settleable anytime on request", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { Toast.makeText(context, "Withdrawal initiated!", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.Start).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Withdraw Balance", color = BrandPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Statistics Grid Blocks: Combining Wallet statistics + Performance counts
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Collected COD and Due Owed
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(95.dp).border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("৳${collected.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark)
                                    Text("Collected COD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(95.dp).border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                val flowColor = if (dueAmount > 0.0) BrandDanger else BrandPrimary
                                Icon(Icons.Default.Schedule, null, tint = flowColor, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("৳${Math.round(Math.abs(dueAmount))}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = flowColor)
                                    val dueLabel = if (dueAmount > 0.0) "Due Owed" else "Settled"
                                    Text(dueLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                }
                            }
                        }
                    }

                    // Row 2: Completed Deliveries and Cancelled Orders
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFFF4)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(95.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.CheckCircle, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("${countDel}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark)
                                    Text("Completed Del.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(95.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Cancel, null, tint = BrandDanger, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("${countCan}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark)
                                    Text("Cancelled Orders", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                }
                            }
                        }
                    }
                }
            }

            // Online Hours Chart Card segment
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ONLINE TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                                Text("7h 22m Shift", fontSize = 15.sp, fontWeight = FontWeight.Black, color = BrandDark)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Active Logs", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OnlineHoursChartCanvas(modifier = Modifier.fillMaxWidth().height(140.dp))
                    }
                }
            }

            // Transaction histories header
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "TRANSACTION HISTORIES",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = BrandSecondaryText,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    letterSpacing = 0.5.sp
                )
            }

            // List of matching histories
            if (historyOrders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No logs found for this shift", fontWeight = FontWeight.Bold, color = BrandSecondaryText, fontSize = 12.sp)
                    }
                }
            } else {
                items(historyOrders) { o ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                            .clickable { activeLedgerDetails = o }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (o.status == "Delivered") BrandPrimary.copy(alpha = 0.08f)
                                            else BrandDanger.copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (o.status == "Delivered") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (o.status == "Delivered") BrandPrimary else BrandDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Order #${getOrderIdDisplay(o)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BrandDark)
                                    Text(if (o.status == "Delivered") "Success Delivery" else "Cancelled", fontSize = 11.sp, color = BrandSecondaryText, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val chg = o.dueChangeCustom
                                val textVal = if (chg > 0.0) "+৳${chg.toInt()}" else if (chg < 0.0) "-৳${Math.abs(chg).toInt()}" else "৳0"
                                val textClr = if (chg > 0.0) BrandDanger else if (chg < 0.0) BrandPrimary else BrandDark
                                Text(textVal, fontWeight = FontWeight.Black, fontSize = 14.sp, color = textClr)
                            }
                        }
                    }
                }
            }
        }

        activeLedgerDetails?.let { order ->
            HistoryStoryDialog(order = order, onDismiss = { activeLedgerDetails = null })
        }
    }
}

@Composable
fun ProfileTab(viewModel: RiderViewModel, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val rider by viewModel.currentRider.collectAsState()
    val pinState by viewModel.pinChangeState.collectAsState()

    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    LaunchedEffect(pinState) {
        pinState?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearPinChangeState()
            if (msg.contains("Success")) {
                oldPin = ""
                newPin = ""
            }
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            RiderTopBar(
                title = "My Profile",
                viewModel = viewModel,
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, null, tint = BrandDark, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BrandBackground),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = rider?.photo?.ifEmpty { "https://via.placeholder.com/120" },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, BrandPrimary, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = rider?.name ?: "Unknown Partner",
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                color = BrandDark
                            )
                            Text(
                                text = rider?.phone ?: "",
                                fontSize = 13.sp,
                                color = BrandSecondaryText,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = (rider?.area ?: "Unknown Area").uppercase(),
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "VERIFICATION SYSTEMS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandSecondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Approved & Active Duty", fontWeight = FontWeight.ExtraBold, color = BrandDark, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AssignmentInd, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("NID Verified by Administration", fontWeight = FontWeight.ExtraBold, color = BrandDark, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SECURITY AND PIN SETTINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandSecondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Change Secure 6-Digit PIN", fontWeight = FontWeight.Black, color = BrandDark, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { oldPin = it },
                            placeholder = { Text("Current PIN", fontWeight = FontWeight.Bold, color = BrandSecondaryText.copy(alpha = 0.6f)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .height(52.dp)
                        )
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6) newPin = it },
                            placeholder = { Text("New 6-Digit PIN", fontWeight = FontWeight.Bold, color = BrandSecondaryText.copy(alpha = 0.6f)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .height(52.dp)
                        )
                        Button(
                            onClick = { viewModel.changePin(oldPin, newPin) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Update Security Locks", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { viewModel.logout(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDanger),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOGOUT ACCOUNT", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun NewOrderPopupDialog(order: Order, onAccept: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(BrandPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.TwoWheeler, null, tint = BrandPrimary, modifier = Modifier.size(46.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Incoming Order!", fontWeight = FontWeight.Black, fontSize = 22.sp, color = BrandDark)
                Spacer(modifier = Modifier.height(10.dp))

                val fee = order.riderFee ?: order.deliveryFee
                val totalIncome = fee + order.surcharge
                val buyCost = order.items.sumOf { (it.buy * it.qty).toDouble() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandLightSlate)
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR INCOME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                            Text("৳${totalIncome.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandPrimary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BrandBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CASH NEEDED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandSecondaryText)
                            Text("৳${buyCost.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandDanger)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = BrandDanger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(order.address.ifEmpty { order.area }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandDark, maxLines = 1)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onAccept() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("ACCEPT ORDER", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HistoryStoryDialog(order: Order, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(BrandDark).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text("Order Historical Ledger", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    val fee = order.riderFee ?: order.deliveryFee
                    val income = fee + order.surcharge
                    val buyTotal = order.items.sumOf { (it.buy * it.qty).toDouble() }
                    val oDiff = order.total - (buyTotal + income)

                    val fmt = remember { SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault()) }
                    val labelTime = fmt.format(Date(order.time)).uppercase()

                    Text("ORDER ID #${getOrderIdDisplay(order)}", fontSize = 11.sp, color = BrandSecondaryText, fontWeight = FontWeight.Bold)
                    Text("DISPATCH LOGGED: $labelTime", fontSize = 10.sp, color = BrandSecondaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("৳${income.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = BrandPrimary)
                    Text("Your final completed fee", fontSize = 11.sp, color = BrandSecondaryText)

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = BrandBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Delivery Fee", fontSize = 13.sp, color = BrandDark)
                        Text("৳${fee.toInt()}", fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                    }
                    if (order.surcharge > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rain/Night Surcharge", fontSize = 13.sp, color = BrandDark)
                            Text("৳${order.surcharge.toInt()}", fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = BrandBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer Payment Due", fontSize = 13.sp, color = BrandSecondaryText)
                        Text("৳${order.total.toInt()}", fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Merchant Payout Paid", fontSize = 13.sp, color = BrandSecondaryText)
                        Text("৳${buyTotal.toInt()}", fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    val dLabel = if (oDiff >= 0.0) "Net Balance Owed to Company" else "Net Balance Company Pays You"
                    val dColor = if (oDiff >= 0.0) BrandDanger else BrandPrimary
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(dColor.copy(alpha = 0.08f)).padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = dColor)
                            Text("৳${Math.round(Math.abs(oDiff))}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = dColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("MERCHANT PURCHASE ITEMS", fontSize = 11.sp, color = BrandSecondaryText, fontWeight = FontWeight.Bold)
                    order.items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text("• ${item.qty}x ${item.name}", fontSize = 12.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun getOrderIdDisplay(order: Order): String {
    return order.oID
}

@Composable
fun OrderTimerText(assignedAt: Long) {
    var elapsedSeconds by remember(assignedAt) { mutableStateOf((System.currentTimeMillis() - assignedAt) / 1000) }

    LaunchedEffect(assignedAt) {
        while (true) {
            delay(1000)
            elapsedSeconds = (System.currentTimeMillis() - assignedAt) / 1000
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val text = String.format(Locale.getDefault(), "%02d:%02d Mins Ago", minutes, seconds)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, null, tint = BrandDark, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandDark)
    }
}

@Composable
fun DeliveryScooterCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "scooter")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scooter_bob"
    )

    Canvas(modifier = Modifier.size(160.dp)) {
        val h = size.height
        val w = size.width

        drawLine(
            color = Color(0xFFE2E8F0),
            start = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.8f),
            end = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.8f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        val centerY = h * 0.65f + bobbingOffset

        // Scooter Rear Wheel
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.08f, center = androidx.compose.ui.geometry.Offset(w * 0.35f, centerY))
        drawCircle(color = Color(0xFF94A3B8), radius = w * 0.04f, center = androidx.compose.ui.geometry.Offset(w * 0.35f, centerY))

        // Scooter Front Wheel
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.08f, center = androidx.compose.ui.geometry.Offset(w * 0.72f, centerY))
        drawCircle(color = Color(0xFF94A3B8), radius = w * 0.04f, center = androidx.compose.ui.geometry.Offset(w * 0.72f, centerY))

        // Chassis
        val framePath = Path().apply {
            moveTo(w * 0.35f, centerY)
            lineTo(w * 0.5f, centerY + w * 0.02f)
            lineTo(w * 0.6f, centerY)
            lineTo(w * 0.7f, centerY - w * 0.15f)
            lineTo(w * 0.72f, centerY)
        }
        drawPath(framePath, color = Color(0xFF64748B), style = Stroke(width = 6f))

        // Main Body Box
        drawRoundRect(
            color = Color(0xFF00C896),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.32f, centerY - w * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, w * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f)
        )

        // Passenger Seat
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, centerY - w * 0.24f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, w * 0.06f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )

        // Front Shield
        val shieldPath = Path().apply {
            moveTo(w * 0.62f, centerY - w * 0.08f)
            lineTo(w * 0.70f, centerY - w * 0.38f)
            lineTo(w * 0.74f, centerY - w * 0.38f)
            lineTo(w * 0.68f, centerY - w * 0.08f)
            close()
        }
        drawPath(shieldPath, color = Color(0xFF00C896))

        // Delivery Box
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, centerY - w * 0.36f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, w * 0.20f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )

        // Headlight
        drawCircle(color = Color(0xFFFFD700), radius = w * 0.02f, center = androidx.compose.ui.geometry.Offset(w * 0.74f, centerY - w * 0.35f))
    }
}

@Composable
fun SimulationMapCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_sim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "map_dot"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val grid = Color(0xFFE2E8F0)
        var x = 0f
        while (x < w) {
            drawLine(grid, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), 1.5f)
            x += 50.dp.toPx()
        }
        var y = 0f
        while (y < h) {
            drawLine(grid, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 1.5f)
            y += 50.dp.toPx()
        }

        // Draw Simulated roads/streets path
        val route = Path().apply {
            moveTo(w * 0.2f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.8f, h * 0.7f)
        }

        drawPath(route, color = Color(0xFF00C896).copy(alpha = 0.15f), style = Stroke(12f, cap = StrokeCap.Round))
        drawPath(route, color = Color(0xFF00C896), style = Stroke(5f, cap = StrokeCap.Round))

        // Start point (Shop)
        drawCircle(Color.White, 12f, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.3f))
        drawCircle(BrandDark, 7f, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.3f))

        // Dropoff point (Customer)
        drawCircle(Color.White, 12f, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.7f))
        drawCircle(BrandDanger, 7f, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.7f))

        // Animating motorcycle state marker
        val finalX: Float
        val finalY: Float
        if (progress < 0.4f) {
            val ratio = progress / 0.4f
            finalX = w * 0.2f + (w * 0.3f) * ratio
            finalY = h * 0.3f
        } else if (progress < 0.75f) {
            val ratio = (progress - 0.4f) / 0.35f
            finalX = w * 0.5f
            finalY = h * 0.3f + (h * 0.4f) * ratio
        } else {
            val ratio = (progress - 0.75f) / 0.25f
            finalX = w * 0.5f + (w * 0.3f) * ratio
            finalY = h * 0.7f
        }

        drawCircle(Color(0xFF00C896).copy(alpha = 0.35f), 18f, androidx.compose.ui.geometry.Offset(finalX, finalY))
        drawCircle(Color(0xFF0F172A), 8f, androidx.compose.ui.geometry.Offset(finalX, finalY))
    }
}

@Composable
fun OnlineHoursChartCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawLine(Color(0xFFE2E8F0), androidx.compose.ui.geometry.Offset(0f, h * 0.9f), androidx.compose.ui.geometry.Offset(w, h * 0.9f), 2f)

        val pts = listOf(
            androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.8f),
            androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.55f),
            androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.4f),
            androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.65f),
            androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.3f),
            androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.7f)
        )

        val fill = Path().apply {
            moveTo(pts.first().x, h * 0.9f)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, h * 0.9f)
            close()
        }

        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00C896).copy(alpha = 0.3f), Color.Transparent),
                startY = h * 0.2f,
                endY = h * 0.9f
            )
        )

        val stroke = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                lineTo(pts[i].x, pts[i].y)
            }
        }
        drawPath(stroke, color = Color(0xFF00C896), style = Stroke(6f, cap = StrokeCap.Round))

        pts.forEach { pt ->
            drawCircle(Color.White, 8f, pt)
            drawCircle(Color(0xFF00C896), 4f, pt)
        }
    }
}
