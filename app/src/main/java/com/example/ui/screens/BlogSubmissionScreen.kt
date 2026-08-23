package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalSaffron
import com.example.ui.viewmodel.BlogSubmissionViewModel

@Composable
fun BlogSubmissionScreen(viewModel: BlogSubmissionViewModel) {
    val context = LocalContext.current
    val submitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var writerInfo by remember { mutableStateOf("") }
    var articleTitle by remember { mutableStateOf("") }
    var articleBody by remember { mutableStateOf("") }
    var photoName by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var docBytes by remember { mutableStateOf<ByteArray?>(null) }
    var docFileName by remember { mutableStateOf("article.doc") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            photoName = uri.lastPathSegment.orEmpty().substringAfterLast('/')
            photoBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            fileName = uri.lastPathSegment.orEmpty().substringAfterLast('/')
            docFileName = fileName.ifBlank { "article.doc" }
            docBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }

    val scheme = MaterialTheme.colorScheme
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PortalSaffron,
        unfocusedBorderColor = scheme.outline,
        focusedContainerColor = scheme.surface,
        unfocusedContainerColor = scheme.surface,
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        cursorColor = PortalSaffron
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Surface(
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("লেখা জমাদান", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = scheme.secondary)
                Text("নিংশিং আৰ্টিকল ফৰ্ম", fontFamily = Kalpurush, fontSize = 14.sp, color = scheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "নাঙ, ঠিকানা, লেখকর ছবি বারো আর্টিকেল পাঠুইক। ওয়েবসাইটর ফর্মহান অহানেই অ্যাপহাত আসে।",
                    fontFamily = Kalpurush,
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            item { OutlinedTextField(name, { name = it }, label = { Text("নাঙহান *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors) }
            item { OutlinedTextField(facebook, { facebook = it }, label = { Text("ফেইসবুক প্রফাইল লিংক *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors) }
            item { OutlinedTextField(address, { address = it }, label = { Text("ঠিকানাহান *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors) }
            item { OutlinedTextField(email, { email = it }, label = { Text("ইমেইল *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)) }
            item { OutlinedTextField(phone, { phone = it }, label = { Text("ফোন নম্বর *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
            item {
                OutlinedButton(onClick = { photoPicker.launch("image/*") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(if (photoName.isBlank()) "লেখকর ছবি বাছিক (.jpg, .png) *" else "ছবি: $photoName", fontFamily = Kalpurush)
                }
            }
            item { OutlinedTextField(writerInfo, { writerInfo = it }, label = { Text("লেখক পরিচিতি *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth().height(110.dp), colors = colors) }
            item { OutlinedTextField(articleTitle, { articleTitle = it }, label = { Text("আৰ্টিকলর টাইটেল *", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth(), colors = colors) }
            item { OutlinedTextField(articleBody, { articleBody = it }, label = { Text("আৰ্টিকল", fontFamily = Kalpurush) }, modifier = Modifier.fillMaxWidth().height(180.dp), colors = colors) }
            item {
                OutlinedButton(onClick = { filePicker.launch("*/*") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(if (fileName.isBlank()) "অথবা আৰ্টিকল ফাইল (.doc, .pdf, .zip)" else "ফাইল: $fileName", fontFamily = Kalpurush)
                }
            }
            if (!status.isNullOrBlank()) {
                item { Text(status.orEmpty(), fontFamily = Kalpurush, color = PortalSaffron, fontSize = 14.sp) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.submit(
                            name = name,
                            facebook = facebook,
                            address = address,
                            email = email,
                            phone = phone,
                            writerInfo = writerInfo,
                            articleTitle = articleTitle,
                            articleBody = articleBody,
                            photoBytes = photoBytes,
                            photoName = photoName.ifBlank { "author.jpg" },
                            fileBytes = docBytes,
                            fileName = docFileName
                        )
                    },
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = PortalSaffron),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blog_submit_button")
                ) {
                    if (submitting) CircularProgressIndicator(modifier = Modifier.height(18.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                    else Text("আৰ্টিকল পাঠুইক", fontFamily = Kalpurush, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
