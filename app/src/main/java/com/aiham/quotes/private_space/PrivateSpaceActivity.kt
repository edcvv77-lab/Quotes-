package com.aiham.quotes.private_space

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.niunaijun.blackbox.BlackBoxCore

class PrivateSpaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF020617)
                )
            ) {
                PrivateSpaceDashboard(
                    onLaunch = { pkg -> runVirtualPackage(pkg) },
                    onInstall = { pkg -> cloneLocalPackage(pkg) }
                )
            }
        }
    }

    private fun runVirtualPackage(packageName: String) {
        try {
            val launched = BlackBoxCore.get().launchApk(packageName, 0)
            if (!launched) {
                Toast.makeText(this, "يرجى نسخ التطبيق إلى البيئة المعزولة أولاً", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ أثناء التشغيل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cloneLocalPackage(packageName: String) {
        try {
            val result = BlackBoxCore.get().installPackageAsUser(packageName, 0)
            if (result.success) {
                Toast.makeText(this, "تم استنساخ التطبيق في المساحة المعزولة بنجاح", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "فشل الاستنساخ: ${result.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceDashboard(
    onLaunch: (String) -> Unit,
    onInstall: (String) -> Unit
) {
    val managedApps = remember {
        listOf(
            Triple("WhatsApp", "com.whatsapp", "تطبيق المراسلة المعزول"),
            Triple("Telegram", "org.telegram.messenger", "المحادثات الآمنة"),
            Triple("WhatsApp Business", "com.whatsapp.w4b", "نسخة الأعمال المعزولة")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aiham Private Space",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617))
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "البيئة الافتراضية نشطة. التطبيقات هنا تعمل في وضع عزل كامل للذاكرة وقواعد البيانات دون تداخل مع النظام الخارجي.",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(managedApps) { (appName, pkgName, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onInstall(pkgName) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("استنساخ", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onLaunch(pkgName) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تشغيل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
