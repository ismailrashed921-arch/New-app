package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RiderAppUi(viewModel: RiderViewModel) {
    val context = LocalContext.current
    val currentRiderState by viewModel.currentRider.collectAsState()
    val assignedOrderPopup by viewModel.assignedOrderPopup.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            currentRiderState == null -> {
                AuthScreen(viewModel)
            }
            currentRiderState?.status == "pending" -> {
                PendingScreen(viewModel)
            }
            !viewModel.isOnline.collectAsState().value && viewModel.activeOrders.collectAsState().value.isEmpty() -> {
                WelcomeScreen(viewModel)
            }
            else -> {
                MainAppScreen(viewModel)
            }
        }

        assignedOrderPopup?.let { order ->
            NewOrderPopupDialog(order = order, onAccept = {
                viewModel.acceptOrder(order.id)
            })
        }
    }
}

@Composable
fun AuthScreen(viewModel: RiderViewModel) {
    var isSignUp by remember { mutableStateOf(false) }

    if (isSignUp) {
        SignUpScreen(viewModel, onNavigateToLogin = { isSignUp = false })
    } else {
        LoginScreen(viewModel, onNavigateToSignUp = { isSignUp = true })
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

    Scaffold(
        containerColor = Color(0xFFF4F7F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Ki-Lagbe Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Ki-Lagbe",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color(0xFF2D3436)
                )
                Text(
                    text = " Rider",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color(0xFF00B894)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Login to Panel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF2D3436),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Registered Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Secret 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = {
                            if (phone.isEmpty() || pin.isEmpty()) {
                                Toast.makeText(context, "Enter Phone and PIN!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.login(context, phone, pin)
                            }
                        },
                        enabled = loginState !is LoginState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Apply as New Rider",
                        color = Color(0xFF0984E3),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onNavigateToSignUp() }
                            .padding(8.dp)
                    )
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

    val locationsState by viewModel.locations.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()

    var areaMenuExpanded by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }
    val nidFrontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { nidFrontUri = it }
    val nidBackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { nidBackUri = it }

    LaunchedEffect(signUpState) {
        if (signUpState is SignUpState.Success) {
            Toast.makeText(context, "Application Submitted! Please wait for Admin approval.", Toast.LENGTH_LONG).show()
            onNavigateToLogin()
        } else if (signUpState is SignUpState.Error) {
            Toast.makeText(context, (signUpState as SignUpState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F7F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Ki-Lagbe Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Apply",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color(0xFF2D3436)
                )
                Text(
                    text = " Rider",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color(0xFF00B894)
                )
            }

            Text(
                text = "Admin Verification Required.",
                color = Color(0xFFFF4757),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (As per NID)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Active Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Create 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // Area Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        OutlinedTextField(
                            value = selectedArea,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Working Area") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Area") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { areaMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = areaMenuExpanded,
                            onDismissRequest = { areaMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth().background(Color.White)
                        ) {
                            locationsState.forEach { areaName ->
                                DropdownMenuItem(
                                    text = { Text(areaName) },
                                    onClick = {
                                        selectedArea = areaName
                                        areaMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selfie Image Picker
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(
                            text = "YOUR CLEAR SELFIE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(14.dp))
                                .clickable { photoLauncher.launch("image/*") }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, "Selfie", tint = Color(0xFF00B894))
                            Text(
                                text = if (photoUri != null) "Selfie Selected ✅" else "Choose Image File",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (photoUri != null) Color(0xFF00B894) else Color.DarkGray
                            )
                        }
                    }

                    // NID identity cards upload
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        Text(
                            text = "NID FRONT & BACK MATCH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(14.dp))
                                    .clickable { nidFrontLauncher.launch("image/*") }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (nidFrontUri != null) "Front ✅" else "NID Front",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nidFrontUri != null) Color(0xFF00B894) else Color.DarkGray
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(14.dp))
                                    .clickable { nidBackLauncher.launch("image/*") }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (nidBackUri != null) "Back ✅" else "NID Back",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nidBackUri != null) Color(0xFF00B894) else Color.DarkGray
                                )
                            }
                        }
                    }

                    // Registration Action
                    Button(
                        onClick = {
                            viewModel.signUp(
                                context,
                                name,
                                phone,
                                pin,
                                selectedArea,
                                photoUri,
                                nidFrontUri,
                                nidBackUri
                            )
                        },
                        enabled = signUpState !is SignUpState.Progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
                    ) {
                        if (signUpState is SignUpState.Progress) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (signUpState as SignUpState.Progress).message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text("Submit Application", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Already Applied? Login",
                        color = Color(0xFF0984E3),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onNavigateToLogin() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PendingScreen(viewModel: RiderViewModel) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color(0xFFF4F7F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = "Pending Approval",
                tint = Color(0xFFF39C12),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Application Pending",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color(0xFF2D3436)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your account has been submitted successfully and is awaiting admin approval. We will notify you once verified.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.logout(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4757)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.width(180.dp).height(50.dp)
            ) {
                Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Application", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: RiderViewModel) {
    Scaffold(
        containerColor = Color(0xFFF4F7F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TwoWheeler,
                contentDescription = "Get Started",
                tint = Color(0xFF00B894),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome Back!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color(0xFF2D3436)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap the button below to go online and start receiving active orders in your area.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    viewModel.setOnlineStatus(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
            ) {
                Icon(Icons.Default.PowerSettingsNew, "Go Online", tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Get Started & Go Online", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: RiderViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF4F7F6),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Home,
                    onClick = { viewModel.activeTab.value = ActiveTab.Home },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00B894),
                        selectedTextColor = Color(0xFF00B894),
                        indicatorColor = Color(0x1100B894),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Wallet,
                    onClick = { viewModel.activeTab.value = ActiveTab.Wallet },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, "Wallet") },
                    label = { Text("Wallet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00B894),
                        selectedTextColor = Color(0xFF00B894),
                        indicatorColor = Color(0x1100B894),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Profile,
                    onClick = { viewModel.activeTab.value = ActiveTab.Profile },
                    icon = { Icon(Icons.Default.AccountCircle, "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00B894),
                        selectedTextColor = Color(0xFF00B894),
                        indicatorColor = Color(0x1100B894),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeTab) {
                ActiveTab.Home -> HomeTab(viewModel)
                ActiveTab.Wallet -> WalletTab(viewModel)
                ActiveTab.Profile -> ProfileTab(viewModel)
            }
        }
    }
}

@Composable
fun HomeTab(viewModel: RiderViewModel) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val currentActiveOrderId by viewModel.currentActiveOrderId.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ki-Lagbe",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF2D3436)
                    )
                    Text(
                        text = " Rider",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF00B894)
                    )
                }

                Button(
                    onClick = { viewModel.setOnlineStatus(!isOnline) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) Color(0xFF00B894) else Color(0xFFFF4757)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "Online Status",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "ONLINE" else "OFFLINE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (activeOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FreeBreakfast,
                        contentDescription = "No Tasks",
                        tint = Color(0xFFDDDDDD),
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active Tasks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stay online to receive new orders.",
                        fontSize = 13.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                items(activeOrders) { order ->
                    val isSelected = order.id == currentActiveOrderId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFF1C40F) else Color(0xFFF4F7F6))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFF1C40F) else Color(0xFFDDDDDD),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.currentActiveOrderId.value = order.id }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Order",
                            tint = if (isSelected) Color(0xFF2D3436) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "#${order.oID}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color(0xFF2D3436) else Color.Gray
                        )
                    }
                }
            }

            val selectedOrder = activeOrders.find { it.id == currentActiveOrderId }
            selectedOrder?.let { order ->
                var actionProgressing by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 20.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1C40F))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ORDER ID: #${order.oID}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF2D3436),
                                letterSpacing = 0.5.sp
                            )
                            OrderTimerText(assignedAt = if (order.riderAssignedAt != 0L) order.riderAssignedAt else order.time)
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = order.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = Color(0xFF2D3436)
                                            )
                                            Text(
                                                text = order.phone,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF0984E3),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2D3436))
                                        ) {
                                            Icon(Icons.Default.Call, "Dial", tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color(0xFFF0F0F0))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = "Pin",
                                            tint = Color(0xFFFF4757),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Column(modifier = Modifier.padding(start = 6.dp)) {
                                            Text(
                                                text = order.address.ifEmpty { order.area },
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF555555),
                                                lineHeight = 18.sp
                                            )
                                            
                                            if (order.note.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFFFF3E0))
                                                        .border(
                                                            width = 1.dp,
                                                            color = Color(0xFFF39C12),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Comment, "Note", tint = Color(0xFFD35400), modifier = Modifier.size(14.dp))
                                                        Text(
                                                            text = " Note: ${order.note}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD35400)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "PICKUP ITEMS & SOURCE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                            )

                            order.items.forEach { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2D3436))
                                        ) {
                                            Text(
                                                text = "${item.qty}",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        AsyncImage(
                                            model = item.img.ifEmpty { "https://via.placeholder.com/60" },
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1.0f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF2D3436)
                                            )
                                            if (item.variant.isNotEmpty()) {
                                                Text(
                                                    text = "Option: ${item.variant}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = "🏪 Shop: ${item.source}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF8F9FA))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                            Text(
                                                text = "Pay to Shop: ৳${item.buy * item.qty} (৳${item.buy} x ${item.qty})",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = Color(0xFFFF4757),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "PAYMENT INFO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val riderFee = order.riderFee ?: order.deliveryFee
                                    val surcharge = order.surcharge
                                    val handlingFee = order.handlingFee
                                    val totalPaymentExpected = order.total
                                    val subtotal = if (order.subtotal > 0.0) order.subtotal else (order.total - riderFee - surcharge - handlingFee)

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Items Sub-total", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                        Text("৳${subtotal.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Delivery Fee", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                        Text("৳${riderFee.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    }
                                    if (handlingFee > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Handling Fee", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                            Text("৳${handlingFee.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        }
                                    }
                                    if (surcharge > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Surcharge (Rain/Night)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00B894))
                                            Text("৳${surcharge.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B894))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color(0xFFAAAAAA))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("COLLECT FROM USER", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                                        Text("৳${totalPaymentExpected.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D3436))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val nextState = when (order.status) {
                                            "Assigned", "Accepted", "Pending" -> "Processing"
                                            "Processing" -> "On the Way"
                                            "On the Way" -> "Delivered"
                                            else -> ""
                                        }
                                        if (nextState.isNotEmpty()) {
                                            if (nextState == "Delivered") {
                                                actionProgressing = true
                                            } else {
                                                viewModel.updateStatus(order.id, nextState) { }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (order.status) {
                                            "Assigned", "Accepted", "Pending" -> Color(0xFF0984E3)
                                            "Processing" -> Color(0xFFF39C12)
                                            "On the Way" -> Color(0xFF00B894)
                                            else -> Color.DarkGray
                                        }
                                    )
                                ) {
                                    val textLabel = when (order.status) {
                                        "Assigned", "Accepted", "Pending" -> "ARRIVED AT SHOP"
                                        "Processing" -> "ON THE WAY"
                                        "On the Way" -> "FINISH DELIVERY"
                                        else -> "CONTINUE"
                                    }
                                    Text(textLabel, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val query = "Hello Admin, I need help with Order #${order.oID}"
                                            val url = "https://wa.me/8801642912431?text=" + URLEncoder.encode(query, "UTF-8")
                                            val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(whatsappIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.SupportAgent, "WhatsApp Support", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                if (actionProgressing) {
                    AlertDialog(
                        onDismissRequest = { actionProgressing = false },
                        title = { Text("Complete Delivery?", fontWeight = FontWeight.Bold, color = Color(0xFF2D3436)) },
                        text = { Text("Did you collect the full cash and deliver all items to the customer?", fontSize = 14.sp) },
                        confirmButton = {
                            TextButton(onClick = {
                                actionProgressing = false
                                viewModel.updateStatus(order.id, "Delivered") { success ->
                                    if (success) {
                                        Toast.makeText(context, "Order Delivered successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("YES, DELIVERED", color = Color(0xFF00B894), fontWeight = FontWeight.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { actionProgressing = false }) {
                                Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
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
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d Mins Ago", minutes, seconds)
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, "Time elapsed", tint = Color(0xFF2D3436), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formattedTime,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3436)
        )
    }
}

@Composable
fun WalletTab(viewModel: RiderViewModel) {
    val earnings by viewModel.walletEarnings.collectAsState()
    val collected by viewModel.walletCollected.collectAsState()
    val countDel by viewModel.walletDeliveredCount.collectAsState()
    val countCan by viewModel.walletCancelledCount.collectAsState()
    val dueAmount by viewModel.walletDueAmount.collectAsState()
    val historyOrders by viewModel.transactionHistory.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()

    var activeLedgerDetails by remember { mutableStateOf<Order?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF00B894))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "My Performance",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val filters = listOf("today" to "Today", "yesterday" to "Yesterday", "week" to "Last 7 Days")
            filters.forEach { (tag, label) ->
                val isSel = filter == tag
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) Color(0xFF2D3436) else Color.White)
                        .border(1.dp, if (isSel) Color(0xFF2D3436) else Color(0xFFDDDDDD), RoundedCornerShape(20.dp))
                        .clickable { viewModel.setFilter(tag) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.White else Color.DarkGray
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL EARNING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("৳${earnings.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D3436))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CASH COLLECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("৳${collected.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D3436))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("COMPLETED DELIVERIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("$countDel", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D3436))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CANCELLED ORDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("$countCan", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4757))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Payments",
                        tint = Color(0xFFF39C12),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val dueText = if (dueAmount > 0.0) "Due Amount" else if (dueAmount < 0.0) "Company Owed" else "Balance Settled"
                        val dueSub = if (dueAmount > 0.0) "You owe to company" else if (dueAmount < 0.0) "Company will pay you" else "All Cash Settled"
                        Text(
                            text = dueText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF2D3436)
                        )
                        Text(
                            text = dueSub,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val flowColor = if (dueAmount > 0.0) Color(0xFFFF4757) else if (dueAmount < 0.0) Color(0xFF00B894) else Color(0xFF2D3436)
                Text(
                    text = "৳${Math.round(Math.abs(dueAmount))}",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = flowColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DUE TRANSACTIONS",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        if (historyOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records for this period.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyOrders) { o ->
                    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    val timeStr = formatter.format(Date(o.time))

                    val fee = o.riderFee ?: o.deliveryFee
                    val totalIncome = fee + o.surcharge
                    var shopBill = 0.0
                    o.items.forEach { item -> shopBill += item.buy * item.qty }

                    val diff = o.total - (totalIncome + shopBill)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeLedgerDetails = o }
                            .border(1.dp, Color(0xFFF1F1F1), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (o.status == "Delivered") Color(0xFFF1F2F6) else Color(0xFFFFEAA7))
                                ) {
                                    Icon(
                                        imageVector = if (o.status == "Delivered") Icons.Default.TwoWheeler else Icons.Default.Close,
                                        contentDescription = o.status,
                                        tint = if (o.status == "Delivered") Color.DarkGray else Color(0xFFD35400),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "Delivery #${o.oID}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D3436)
                                    )
                                    Text(
                                        text = if (o.status == "Delivered") timeStr else "$timeStr (Cancelled)",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (o.status == "Delivered") {
                                    Text(
                                        text = if (diff >= 0.0) "+ ৳${diff.toInt()}" else "- ৳${Math.abs(diff).toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (diff >= 0.0) Color(0xFFFF4757) else Color(0xFF00B894)
                                    )
                                    Text(
                                        text = if (o.cashSettled) "Settled" else "Pending Settle",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (o.cashSettled) Color(0xFF00B894) else Color(0xFFFF4757),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "৳0",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.DarkGray
                                    )
                                }
                            }
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

@Composable
fun ProfileTab(viewModel: RiderViewModel) {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Profile",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF2D3436)
                )
            }
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                AsyncImage(
                    model = rider?.photo?.ifEmpty { "https://via.placeholder.com/120" },
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF00B894), CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = rider?.name ?: "Rider Name",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color(0xFF2D3436)
                )

                Text(
                    text = rider?.phone ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF00B894))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, "Area", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = " " + (rider?.area ?: "Area"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ACCOUNT STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Active", tint = Color(0xFF00B894), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approved & Active", fontWeight = FontWeight.ExtraBold, color = Color(0xFF00B894), fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFF1F1F1))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "IDENTITY VERIFICATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentInd, "NID", tint = Color(0xFF0984E3), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NID Verified by Admin", fontWeight = FontWeight.ExtraBold, color = Color(0xFF555555), fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SECURITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Change 6-Digit PIN",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2D3436),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        placeholder = { Text("Current PIN") },
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
                        placeholder = { Text("New 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .height(52.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.changePin(oldPin, newPin)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.logout(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4757)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("LOGOUT ACCOUNT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NewOrderPopupDialog(order: Order, onAccept: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = "New Order Alert",
                    tint = Color(0xFF00B894),
                    modifier = Modifier.size(60.dp)
                )

                Text(
                    text = "New Order!",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFF2D3436),
                    modifier = Modifier.padding(top = 8.dp)
                )

                val fee = order.riderFee ?: order.deliveryFee
                val totalIncome = fee + order.surcharge
                var buyCost = 0.0
                order.items.forEach { buyCost += it.buy * it.qty }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF4F7F6))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR INCOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("৳${totalIncome.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D3436))
                            if (order.surcharge > 0.0) {
                                Text(
                                    text = "(Fee:${fee.toInt()} + Sur:${order.surcharge.toInt()})",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFDDDDDD)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CASH NEEDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("৳${buyCost.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4757))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF9EB))
                        .border(1.dp, Color(0xFFF1C40F), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, "Marker", tint = Color(0xFFFF4757), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELIVERY LOCATION:", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.address.ifEmpty { order.area },
                            fontSize = 13.sp,
                            color = Color(0xFF2D3436),
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAccept() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
                ) {
                    Text("ACCEPT ORDER", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
fun HistoryStoryDialog(order: Order, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.DarkGray)
                    }
                    Text(
                        text = "Order Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2D3436)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val fee = order.riderFee ?: order.deliveryFee
                    val totalIncome = fee + order.surcharge
                    var shopPayment = 0.0
                    order.items.forEach { shopPayment += it.buy * it.qty }

                    val diff = order.total - (shopPayment + totalIncome)

                    val formatter = remember { SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault()) }
                    val timeStr = formatter.format(Date(order.time)).uppercase()

                    Text(
                        text = "ORDER ID # ${order.oID}\nDELIVERY EARNINGS | $timeStr",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "৳${totalIncome.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00B894),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Delivery Fee", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        Text("৳${fee.toInt()}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }

                    if (order.surcharge > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Surcharge", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            Text("৳${order.surcharge.toInt()}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFDDDDDD), modifier = Modifier.padding(bottom = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Income", fontSize = 15.sp, color = Color(0xFF2D3436), fontWeight = FontWeight.ExtraBold)
                        Text("৳${totalIncome.toInt()}", fontSize = 15.sp, color = Color(0xFF2D3436), fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("User Payment", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Text("৳${order.total.toInt()}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shop/Restaurant Bill", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Text("৳${shopPayment.toInt()}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                            if (order.handlingFee > 0.0) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Handling Fee", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text("৳${order.handlingFee.toInt()}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFFF1F1F1))
                            Spacer(modifier = Modifier.height(8.dp))

                            val dueText = if (diff >= 0.0) "You Pay to Company" else "Company Pays You"
                            val dueColor = if (diff >= 0.0) Color(0xFF2D3436) else Color(0xFF00B894)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(dueText, fontSize = 14.sp, color = dueColor, fontWeight = FontWeight.Black)
                                Text("৳${Math.round(Math.abs(diff))}", fontSize = 16.sp, color = dueColor, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Text(
                        text = "DELIVERY ITEMS",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    order.items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2D3436))
                            ) {
                                Text(
                                    text = "${item.qty}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.Close, "qty-icon", tint = Color.LightGray, modifier = Modifier.size(10.dp))
                            Text(
                                text = item.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ORDER ROUTE INFO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2D3436)))
                            Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color(0xFFDDDDDD)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF4757)))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            val firstStore = order.items.firstOrNull()?.source ?: "Ki-Lagbe Shop"
                            Text(
                                text = firstStore,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(30.dp))
                            Text(
                                text = order.address.ifEmpty { order.area },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDDDDD))
                        ) {
                            Icon(Icons.Default.Person, "User", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = order.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFF2D3436)
                        )
                    }
                }
            }
        }
    }
}
