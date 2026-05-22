package com.example.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

object CloudinaryUploader {

    private val client = OkHttpClient()

    suspend fun uploadImageUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            
            // Resize image to ensure it's lightweight (max dimension 800)
            val out = ByteArrayOutputStream()
            val maxDimension = 800
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            val (newWidth, newHeight) = if (originalWidth > originalHeight) {
                if (originalWidth > maxDimension) {
                    val scale = maxDimension.toFloat() / originalWidth
                    Pair(maxDimension, (originalHeight * scale).toInt())
                } else Pair(originalWidth, originalHeight)
            } else {
                if (originalHeight > maxDimension) {
                    val scale = maxDimension.toFloat() / originalHeight
                    Pair((originalWidth * scale).toInt(), maxDimension)
                } else Pair(originalWidth, originalHeight)
            }
            
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            val bytes = out.toByteArray()
            
            // Replicate Web data URI structure for Cloudinary
            val base64String = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", base64String)
                .addFormDataPart("upload_preset", "ki_lagbe_preset")
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dqjxtpf6x/image/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                return@withContext json.optString("secure_url", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }
}
