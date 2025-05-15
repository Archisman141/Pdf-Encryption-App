package com.example.pdfsecurityapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.pdfsecurityapp.util.FileUtils
import com.example.pdfsecurityapp.util.PdfEncryptor

@Composable
fun EncryptPdfScreen() {
    val context = LocalContext.current
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var statusMessage by remember { mutableStateOf("") }

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> selectedPdfUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Encrypt a PDF from Local Storage", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(onClick = { openPdfLauncher.launch(arrayOf("application/pdf")) }) {
            Text("Pick PDF File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPdfUri != null) {
            Text("Selected file: ${FileUtils.getFileName(context, selectedPdfUri!!)}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedPdfUri == null || password.text.isEmpty()) {
                    statusMessage = "Select a PDF and enter a password."
                    return@Button
                }

                try {
                    val inputFile = FileUtils.copyUriToInternalFile(context, selectedPdfUri!!, "temp_input.pdf")
                    val outputUri = PdfEncryptor.encryptAndSavePdf(context, inputFile, password.text)
                    statusMessage = "PDF Encrypted.\nSaved to: $outputUri"
                    Toast.makeText(context, "PDF encrypted successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusMessage = "Error: ${e.message}"
                    e.printStackTrace()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Encrypt PDF")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage)
        }
    }
}
