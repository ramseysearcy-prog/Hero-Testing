package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.domain.EhtOcrParser
import com.example.domain.ParsedOcrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed interface MediaScanUpdate {
    data class Progress(
        val currentFrame: Int,
        val totalFrames: Int,
        val detectedItems: List<ParsedOcrItem>
    ) : MediaScanUpdate

    data class Completed(
        val framesScanned: Int,
        val detectedItems: List<ParsedOcrItem>,
        val lastRawText: String
    ) : MediaScanUpdate

    data class Failed(val message: String) : MediaScanUpdate
}

object VideoOcrProcessor {
    private const val TAG = "VideoOcrProcessor"

    fun scanVideo(context: Context, videoUri: Uri): Flow<MediaScanUpdate> = flow {
        val retriever = MediaMetadataRetriever()
        val detected = mutableListOf<ParsedOcrItem>()
        val seenSignatures = mutableSetOf<String>()
        var lastRawText = ""

        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1000L) ?: 5000L

            // Sample ~1.5 frames per second, bounded between 4 and 36 frames for crisp speed & recall
            val stepMs = when {
                durationMs <= 6_000L -> 550L
                durationMs <= 20_000L -> 750L
                else -> (durationMs / 36L).coerceAtLeast(900L)
            }

            val sampleTimestampsUs = mutableListOf<Long>()
            var tMs = 200L
            while (tMs < durationMs && sampleTimestampsUs.size < 36) {
                sampleTimestampsUs.add(tMs * 1000L)
                tMs += stepMs
            }
            if (sampleTimestampsUs.isEmpty()) {
                sampleTimestampsUs.add(0L)
            }

            val total = sampleTimestampsUs.size
            for ((index, timeUs) in sampleTimestampsUs.withIndex()) {
                currentCoroutineContext().ensureActive()
                val rawBitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (rawBitmap != null) {
                    val scaled = scaleForOcr(rawBitmap)
                    if (scaled !== rawBitmap) rawBitmap.recycle()
                    try {
                        val text = MlKitOcrEngine.recognizeText(scaled)
                        if (text.isNotBlank()) {
                            lastRawText = text
                            val parsed = EhtOcrParser.parse(text)
                            if (parsed != null && seenSignatures.add(parsed.dedupSignature)) {
                                detected.add(parsed)
                            }
                        }
                    } finally {
                        scaled.recycle()
                    }
                }
                emit(MediaScanUpdate.Progress(index + 1, total, detected.toList()))
            }

            emit(MediaScanUpdate.Completed(total, detected.toList(), lastRawText))
        } catch (e: Exception) {
            Log.e(TAG, "Video OCR failed", e)
            emit(MediaScanUpdate.Failed(e.localizedMessage ?: "Failed to read video frames."))
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }.flowOn(Dispatchers.Default)

    suspend fun scanImage(context: Context, imageUri: Uri): Result<Pair<ParsedOcrItem?, String>> =
        withContext(Dispatchers.Default) {
            try {
                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@withContext Result.failure(IllegalStateException("Could not open selected image."))

                val scaled = scaleForOcr(bitmap)
                if (scaled !== bitmap) bitmap.recycle()
                try {
                    val rawText = MlKitOcrEngine.recognizeText(scaled)
                    val parsed = EhtOcrParser.parse(rawText)
                    Result.success(parsed to rawText)
                } finally {
                    scaled.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image OCR failed", e)
                Result.failure(e)
            }
        }

    private fun scaleForOcr(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= 1440) return bitmap
        val ratio = 1440f / maxDim.toFloat()
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
