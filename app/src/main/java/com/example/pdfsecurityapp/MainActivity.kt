package com.example.pdfsecurityapp
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.WriterProperties
import java.io.File
import com.itextpdf.kernel.pdf.EncryptionConstants
import java.io.FileOutputStream
import java.io.OutputStream


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EncryptPdfFromDiskScreen()
        }
    }
}

@Composable
fun EncryptPdfFromDiskScreen() {
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
            Text("Selected file: ${getFileName(context, selectedPdfUri!!)}")
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
                    val inputFile = copyUriToInternalFile(context, selectedPdfUri!!, "temp_input.pdf")

                    val outputUri = saveEncryptedPdfToDownloads(context, inputFile, password.text)
                    statusMessage = "PDF Encrypted.\nSaved to Downloads"
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

fun copyUriToInternalFile(context: Context, uri: Uri, filename: String): File {
    val file = File(context.filesDir, filename)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst()) {
            name = it.getString(nameIndex)
        }
    }
    return name
}

fun saveEncryptedPdfToDownloads(context: Context, inputPdfFile: File, password: String): Uri {
    try {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "encrypted_pdf_${System.currentTimeMillis()}.pdf"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }

        val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw Exception("Failed to create new file in Downloads")

        val outputStream: OutputStream? = uri.let { contentResolver.openOutputStream(it) }

        val reader = PdfReader(inputPdfFile)
        val writerProperties = WriterProperties()
            .setStandardEncryption(
                password.toByteArray(),
                password.toByteArray(),
                EncryptionConstants.ALLOW_PRINTING,
                EncryptionConstants.ENCRYPTION_AES_128
            )

        val pdfWriter = PdfWriter(outputStream, writerProperties)
        val pdfDoc = PdfDocument(reader, pdfWriter)
        pdfDoc.close()

        return uri
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEncryptPdfScreen() {
    EncryptPdfFromDiskScreen()
}
