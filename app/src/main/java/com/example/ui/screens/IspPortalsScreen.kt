package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.IspPortal
import com.example.ui.NocViewModel
import com.example.ui.theme.*

@Composable
fun IspPortalsScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val portals by viewModel.ispPortals.collectAsState()

    val safeIndex = if (portals.isNotEmpty()) uiState.selectedIspIndex.coerceIn(0, portals.lastIndex) else 0
    val activePortal = portals.getOrNull(safeIndex) ?: portals.firstOrNull()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(100) }
    var isWebLoading by remember { mutableStateOf(false) }
    var currentLoadedUrl by remember { mutableStateOf("") }
    var reloadNonce by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("isp_portals_screen")
    ) {
        // --- 1. Top Header & ISP Slider Bar ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ISP Web Login Portals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "BSNL FMS • Meghbela • GTPL KCBPL • Cyber City Radius",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Slide arrow navigation helpers
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (portals.isNotEmpty()) {
                                    val newIndex = (safeIndex - 1 + portals.size) % portals.size
                                    viewModel.selectIspPortalIndex(newIndex)
                                }
                            },
                            modifier = Modifier.size(34.dp).testTag("isp_prev_btn")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous ISP", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "${if (portals.isNotEmpty()) safeIndex + 1 else 0}/${portals.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                if (portals.isNotEmpty()) {
                                    val newIndex = (safeIndex + 1) % portals.size
                                    viewModel.selectIspPortalIndex(newIndex)
                                }
                            },
                            modifier = Modifier.size(34.dp).testTag("isp_next_btn")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next ISP", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Slider / Horizontal Scrollable Tab Row
                if (portals.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = safeIndex,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        indicator = { tabPositions ->
                            if (safeIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[safeIndex]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                    ) {
                        portals.forEachIndexed { index, portal ->
                            val isSelected = safeIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectIspPortalIndex(index)
                                },
                                modifier = Modifier
                                    .testTag("isp_tab_$index")
                                    .padding(vertical = 2.dp),
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(portal.primaryColorHex))
                                        )
                                        Text(
                                            text = portal.shortName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (activePortal != null) {
            // --- 2. Saved Passwords & Credentials Vault Card ---
            IspCredentialsVaultCard(
                portal = activePortal,
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePassword = { viewModel.togglePasswordVisibility() },
                onEditCredentials = { viewModel.openEditCredentialsDialog(activePortal) },
                onAutofill = {
                    webViewInstance?.let { webView ->
                        val escapedUser = activePortal.username.replace("\\", "\\\\").replace("'", "\\'")
                        val escapedPass = activePortal.password.replace("\\", "\\\\").replace("'", "\\'")
                        val escapedPartner = activePortal.partnerCode.replace("\\", "\\\\").replace("'", "\\'")
                        val js = """
                            (function() {
                                var userInputs = document.querySelectorAll("input[type='text'], input[name*='user' i], input[id*='user' i], input[name*='login' i], input[id*='login' i], input[name*='email' i]");
                                if (userInputs.length > 0) {
                                    userInputs[0].value = '$escapedUser';
                                    userInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                    userInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                                }
                                var passInputs = document.querySelectorAll("input[type='password'], input[name*='pass' i], input[id*='pass' i]");
                                if (passInputs.length > 0) {
                                    passInputs[0].value = '$escapedPass';
                                    passInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                    passInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                                }
                                var codeInputs = document.querySelectorAll("input[name*='partner' i], input[id*='partner' i], input[id*='ssa' i]");
                                if (codeInputs.length > 0) {
                                    codeInputs[0].value = '$escapedPartner';
                                    codeInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                    codeInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(js, null)
                        Toast.makeText(context, "Autofilled credentials for ${activePortal.shortName}!", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenExternal = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activePortal.url))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open external browser: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // --- 3. Live Web Login Address Bar & Navigation Controls ---
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "SSL Secure",
                                tint = NocSuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLoadedUrl.ifBlank { activePortal.url },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = NocSuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "WEB LOGIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NocSuccessGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    }
                                },
                                modifier = Modifier.size(28.dp).testTag("isp_web_back_btn")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(15.dp))
                            }

                            IconButton(
                                onClick = {
                                    reloadNonce++
                                    webViewInstance?.loadUrl(activePortal.url)
                                },
                                modifier = Modifier.size(28.dp).testTag("isp_reload_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload Web Login", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (isWebLoading) {
                        LinearProgressIndicator(
                            progress = { (webProgress / 100f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // --- 4. Direct Web Login WebView ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                IspPortalWebView(
                    portal = activePortal,
                    reloadNonce = reloadNonce,
                    onWebViewCreated = { webViewInstance = it },
                    onUrlChanged = { url -> currentLoadedUrl = url },
                    onProgressChanged = { progress ->
                        webProgress = progress
                        isWebLoading = progress < 100
                    }
                )
            }
        }
    }

    // --- 5. Edit Saved Password & Credentials Dialog ---
    if (uiState.isEditCredentialsDialogOpen && uiState.editingPortal != null) {
        EditIspCredentialsDialog(
            portal = uiState.editingPortal!!,
            onDismiss = { viewModel.closeEditCredentialsDialog() },
            onSave = { user, pass, partnerCode, notes ->
                viewModel.saveIspCredentials(
                    id = uiState.editingPortal!!.id,
                    username = user,
                    password = pass,
                    partnerCode = partnerCode,
                    notes = notes
                )
                Toast.makeText(context, "Password saved securely in local database!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun IspCredentialsVaultCard(
    portal: IspPortal,
    isPasswordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onEditCredentials: () -> Unit,
    onAutofill: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(portal.primaryColorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(portal.primaryColorHex),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${portal.shortName} • Saved Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Partner ID: ${portal.partnerCode}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEditCredentials,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp).testTag("edit_credentials_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit / Save", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // User & Password details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Username Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("USERNAME / ID", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = portal.username.ifBlank { "Not set" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Username", portal.username))
                                Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy User", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Password Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PASSWORD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isPasswordVisible) portal.password else "••••••••",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (!isPasswordVisible) FontFamily.Default else FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onTogglePassword,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Password", portal.password))
                                    Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row: Autofill & Open External
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAutofill,
                    modifier = Modifier.weight(1f).height(34.dp).testTag("autofill_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Autofill Web Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chrome", fontSize = 11.sp)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun IspPortalWebView(
    portal: IspPortal,
    reloadNonce: Int,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit
) {
    val loadKey = "${portal.id}_${portal.url}_$reloadNonce"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (!url.isNullOrBlank()) {
                            onUrlChanged(url)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!url.isNullOrBlank()) {
                            onUrlChanged(url)
                        }
                        onProgressChanged(100)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        onProgressChanged(100)
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.proceed()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }

                onWebViewCreated(this)
                tag = loadKey
                onUrlChanged(portal.url)
                loadUrl(portal.url)
            }
        },
        update = { webView ->
            // Only load when the selected ISP portal or explicit reload nonce changes
            if (webView.tag != loadKey) {
                webView.tag = loadKey
                onUrlChanged(portal.url)
                webView.loadUrl(portal.url)
            }
        },
        modifier = Modifier.fillMaxSize().testTag("isp_portal_webview")
    )
}

@Composable
private fun EditIspCredentialsDialog(
    portal: IspPortal,
    onDismiss: () -> Unit,
    onSave: (username: String, pass: String, partnerCode: String, notes: String) -> Unit
) {
    var username by remember(portal) { mutableStateOf(portal.username) }
    var password by remember(portal) { mutableStateOf(portal.password) }
    var partnerCode by remember(portal) { mutableStateOf(portal.partnerCode) }
    var notes by remember(portal) { mutableStateOf(portal.notes) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Password - ${portal.shortName}")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Store your credentials in the local database for quick 1-tap autofill & login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / LCO ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("edit_password_input")
                )

                OutlinedTextField(
                    value = partnerCode,
                    onValueChange = { partnerCode = it },
                    label = { Text("Partner Code / SSA ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Operator Notes / SSA") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, password, partnerCode, notes) },
                modifier = Modifier.testTag("save_credentials_confirm_btn")
            ) {
                Text("Save Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
