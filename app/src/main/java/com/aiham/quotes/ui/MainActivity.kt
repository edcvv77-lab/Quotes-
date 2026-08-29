package com.aiham.quotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiham.quotes.private_space.PrivateSpaceActivity
import com.aiham.quotes.security.SecurityTrigger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF59E0B),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFF1E293B)
                )
            ) {
                QuotesAppScreen(
                    onTriggerUnlocked = {
                        val intent = Intent(this, PrivateSpaceActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesAppScreen(onTriggerUnlocked: () -> Unit) {
    var quotesList by remember {
        mutableStateOf(
            listOf(
                "العقل مثل المظلة، لا يعمل إلا إذا كان مفتوحاً.",
                "لا تنتظر الفرصة، بل اصنعها بنفسك.",
                "أفضل طريقة للتنبؤ بالمستقبل هي أن تخترعه.",
                "البساطة هي قمة التطور والذكاء."
            )
        )
    }

    var isDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اقتباسات",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isDialogVisible = true },
                containerColor = Color(0xFFF59E0B),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة اقتباس")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(quotesList) { quote ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            text = "“ $quote ”",
                            modifier = Modifier.padding(20.dp),
                            color = Color(0xFFE2E8F0),
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        if (isDialogVisible) {
            AddQuoteModalDialog(
                onDismiss = { isDialogVisible = false },
                onQuoteSubmitted = { enteredText ->
                    isDialogVisible = false
                    if (SecurityTrigger.checkTrigger(enteredText)) {
                        onTriggerUnlocked()
                    } else if (enteredText.isNotBlank()) {
                        quotesList = quotesList + enteredText.trim()
                    }
                }
            )
        }
    }
}

@Composable
fun AddQuoteModalDialog(
    onDismiss: () -> Unit,
    onQuoteSubmitted: (String) -> Unit
) {
    var quoteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "أضف اقتباسك",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = quoteInput,
                onValueChange = { quoteInput = it },
                placeholder = { Text("اكتب الاقتباس هنا...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4
            )
        },
        confirmButton = {
            Button(
                onClick = { onQuoteSubmitted(quoteInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}
