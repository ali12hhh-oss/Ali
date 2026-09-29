package com.velocity.editor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import com.velocity.editor.data.ClipEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExportRequest(
    val projectName: String,
    val videoClips: List<ClipEntity>,
    val musicClips: List<ClipEntity>,
    val muteVideoAudio: Boolean,
    val muteMusic: Boolean,
    val height: Int,
    val fps: Int,
    val bitrateMbps: Int,
    val filterId: String?,
    val titleId: String?,
    /** Custom caption text; falls back to projectName when null/blank. */
    val titleText: String?,
    val watermark: Boolean,
)

sealed interface ExportEvent {
    data class Progress(val fraction: Float) : ExportEvent
    data class Completed(val uri: Uri) : ExportEvent
    data class Failure(val message: String) : ExportEvent
}

@OptIn(UnstableApi::class)
@Singleton
class VideoExporter @Inject constructor(@ApplicationContext private val context: Context) {

    /** Must be collected on the main thread (Transformer requires a Looper thread). */
    fun export(request: ExportRequest): Flow<ExportEvent> = callbackFlow {
        val outFile = File(context.cacheDir, "velocity_export_${System.currentTimeMillis()}.mp4")

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(request.bitrateMbps * 1_000_000).build(),
            ).build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    launch(Dispatchers.IO) {
                        val uri = saveToGallery(outFile, request.projectName)
                        trySend(if (uri != null) ExportEvent.Completed(uri) else ExportEvent.Failure("Could not save the file"))
                        close()
                    }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    trySend(ExportEvent.Failure(exportException.message ?: "Unknown error"))
                    close()
                }
            })
            .build()

        transformer.start(buildComposition(request), outFile.absolutePath)

        val holder = ProgressHolder()
        val poller = launch {
            while (isActive) {
                if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    trySend(ExportEvent.Progress(holder.progress / 100f))
                }
                delay(250)
            }
        }
        awaitClose {
            poller.cancel()
            runCatching { transformer.cancel() }
            outFile.delete()
        }
    }

    private fun buildComposition(request: ExportRequest): Composition {
        val videoEffects = mutableListOf<Effect>()
        filterEffect(request.filterId)?.let { videoEffects += it }
        val width = request.height * 16 / 9
        videoEffects += Presentation.createForWidthAndHeight(width, request.height, Presentation.LAYOUT_SCALE_TO_FIT)
        videoEffects += FrameDropEffect.createDefaultFrameDropEffect(request.fps.toFloat())

        val overlays = mutableListOf<TextureOverlay>()
        val captionText = request.titleText?.takeIf { it.isNotBlank() } ?: request.projectName
        request.titleId?.let { titleOverlay(it, captionText) }?.let { overlays += it }
        if (request.watermark) overlays += watermarkOverlay()
        if (overlays.isNotEmpty()) videoEffects += OverlayEffect(ImmutableList.copyOf(overlays))

        val videoItems = request.videoClips.map { clip ->
            EditedMediaItem.Builder(MediaItem.fromUri(clip.uri))
                .setRemoveAudio(request.muteVideoAudio)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()
        }
        val videoSequence = EditedMediaItemSequence.Builder(*videoItems.toTypedArray()).build()

        val musicItems = if (request.muteMusic) emptyList() else request.musicClips.map { clip ->
            EditedMediaItem.Builder(MediaItem.fromUri(clip.uri)).setRemoveVideo(true).build()
        }
        return if (musicItems.isEmpty()) {
            Composition.Builder(videoSequence).build()
        } else {
            Composition.Builder(videoSequence, EditedMediaItemSequence.Builder(*musicItems.toTypedArray()).build()).build()
        }
    }

    private fun filterEffect(id: String?): Effect? = when (id) {
        "bw" -> RgbFilter.createGrayscaleFilter()
        "vivid" -> Contrast(0.25f)
        "warm" -> RgbAdjustment.Builder().setRedScale(1.15f).setBlueScale(0.88f).build()
        "cool" -> RgbAdjustment.Builder().setRedScale(0.9f).setBlueScale(1.15f).build()
        "faded" -> Contrast(-0.25f)
        else -> null
    }

    private fun titleOverlay(style: String, text: String): TextureOverlay {
        val span = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.WHITE), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(if (style == "bold") 72 else if (style == "minimal") 36 else 52), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (style == "bold") setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val settings = when (style) {
            "bold" -> OverlaySettings.Builder().setBackgroundFrameAnchor(0f, 0.7f).setOverlayFrameAnchor(0f, 0f)
            "minimal" -> OverlaySettings.Builder().setBackgroundFrameAnchor(-0.92f, -0.88f).setOverlayFrameAnchor(-1f, -1f)
            else -> OverlaySettings.Builder().setBackgroundFrameAnchor(0f, -0.8f).setOverlayFrameAnchor(0f, 0f)
        }.build()
        return TextOverlay.createStaticTextOverlay(span, settings)
    }

    private fun watermarkOverlay(): TextureOverlay {
        val span = SpannableString("VELOCITY").apply {
            setSpan(ForegroundColorSpan(Color.argb(190, 255, 255, 255)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(34), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val settings = OverlaySettings.Builder().setBackgroundFrameAnchor(0.94f, -0.92f).setOverlayFrameAnchor(1f, -1f).build()
        return TextOverlay.createStaticTextOverlay(span, settings)
    }

    private suspend fun saveToGallery(file: File, projectName: String): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Velocity_${projectName.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")}_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Velocity")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching null
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        }.getOrNull()
    }
}
