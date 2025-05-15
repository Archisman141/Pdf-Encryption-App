package com.example.pdfsecurityapp.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.EncryptionConstants
import java.io.File
import java.io.OutputStream

object PdfEncryptor {
    fun encryptAndSavePdf(context: Context, inputPdfFile: File, password: String): Uri {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "encrypted_pdf_${System.currentTimeMillis()}.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }

        val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw Exception("Failed to create new file in Downloads")

        val outputStream: OutputStream = uri.let {
            contentResolver.openOutputStream(it)
                ?: throw Exception("Failed to open output stream")
        }

        val reader = PdfReader(inputPdfFile)
        val writerProps = WriterProperties()
            .setStandardEncryption(
                password.toByteArray(),
                password.toByteArray(),
                EncryptionConstants.ALLOW_PRINTING,
                EncryptionConstants.ENCRYPTION_AES_256
            )

        val writer = PdfWriter(outputStream, writerProps)
        val pdfDoc = PdfDocument(reader, writer)
        pdfDoc.close()

        return uri
    }
}
