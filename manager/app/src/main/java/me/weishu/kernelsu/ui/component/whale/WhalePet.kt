package me.weishu.kernelsu.ui.component.whale

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class WhaleCanvas(val w: Int = 1280, val h: Int = 1280)

@Serializable
data class WhaleLayerDef(
    val id: String = "",
    val file: String = "",
    val group: String = "body",
    val role: String = "",
)

@Serializable
data class WhaleMotion(
    val breath: Float = 0.01f,
    val sway: Float = 0.006f,
    val headTurn: Float = 5f,
)

@Serializable
data class WhaleMouth(
    val rest: String = "open",
    val squashMin: Float = 0.3f,
    val squashRotation: Float = 0f,
    val squashOffset: List<Float> = listOf(0f, 0f),
)

@Serializable
data class WhaleRig(
    val headPivot: List<Float> = listOf(0f, 0f),
    val tailPivot: List<Float> = listOf(0f, 0f),
    val mouthPivot: List<Float> = listOf(0f, 0f),
    val tailAmp: Float = 0.1f,
    val blinkLash: Float = 10f,
    val headTilt: Float = 0.005f,
    val motion: WhaleMotion = WhaleMotion(),
    val mouth: WhaleMouth = WhaleMouth(),
)

@Serializable
data class WhaleManifest(
    val canvas: WhaleCanvas = WhaleCanvas(),
    val rigOptions: WhaleRig = WhaleRig(),
    val layers: List<WhaleLayerDef> = emptyList(),
)

private data class LoadedLayer(val def: WhaleLayerDef, val bitmap: ImageBitmap)

private data class LoadedSkin(val manifest: WhaleManifest, val layers: List<LoadedLayer>)

private val whaleJson = Json { ignoreUnknownKeys = true }

private suspend fun loadWhaleSkin(context: Context, dir: String): LoadedSkin? =
    withContext(Dispatchers.IO) {
        runCatching {
            val text = context.assets.open("$dir/manifest.json")
                .bufferedReader().use { it.readText() }
            val manifest = whaleJson.decodeFromString<WhaleManifest>(text)
            val layers = manifest.layers.mapNotNull { def ->
                runCatching {
                    context.assets.open("$dir/${def.file}").use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()?.let { LoadedLayer(def, it) }
            }
            if (layers.isEmpty()) null else LoadedSkin(manifest, layers)
        }.getOrNull()
    }

enum class WhaleState { Idle, Weak }

/**
 * 分层立绘 + 代码动画。
 *
 * @param skinDir assets 下的皮肤目录，默认 skin/feiyu-02
 * @param talking 说话时嘴部开合
 * @param state   Weak 时整体变淡（未接入内核）
 */
@Composable
fun WhalePet(
    modifier: Modifier = Modifier,
    skinDir: String = "skin/feiyu-02",
    talking: Boolean = false,
    state: WhaleState = WhaleState.Idle,
    onTap: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val skin by produceState<LoadedSkin?>(initialValue = null, skinDir) {
        value = loadWhaleSkin(context, skinDir)
    }
    val loaded = skin ?: return

    val canvasW = loaded.manifest.canvas.w.toFloat()
    val canvasH = loaded.manifest.canvas.h.toFloat()
    val rig = loaded.manifest.rigOptions

    val infinite = rememberInfiniteTransition(label = "whale")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )

    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3500, 7000))
            blink.animateTo(1f, tween(90))
            blink.animateTo(0f, tween(110))
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(canvasW / canvasH)
            .then(
                if (onTap != null) Modifier.clickable { onTap() } else Modifier
            )
    ) {
        loaded.layers.forEach { layer ->
            val role = layer.def.role
            val isHead = layer.def.group == "head"
            val isLash = role.startsWith("eye.lash")
            val isTail = role == "tail"
            val isMouth = role == "mouth"

            Image(
                bitmap = layer.bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = sin(phase)
                        var sy = 1f + rig.motion.breath * s
                        var ty = -size.height * 0.004f * s

                        if (isHead) {
                            translationX = rig.motion.headTurn * (size.height / canvasH) * s * 0.15f
                            rotationZ = rig.headTilt * s * (180f / PI.toFloat())
                        }
                        if (isTail) {
                            transformOrigin = TransformOrigin(
                                rig.tailPivot[0] / canvasW,
                                rig.tailPivot[1] / canvasH,
                            )
                            rotationZ = rig.tailAmp * sin(phase - 0.6f) * (180f / PI.toFloat())
                        }
                        if (isLash) {
                            translationY = rig.blinkLash * blink.value * (size.height / canvasH)
                        }
                        if (isMouth) {
                            val amt = if (talking) (sin(phase * 6f) * 0.5f + 0.5f) else 0f
                            transformOrigin = TransformOrigin(
                                rig.mouthPivot[0] / canvasW,
                                rig.mouthPivot[1] / canvasH,
                            )
                            if (rig.mouth.rest == "open") {
                                sy *= 1f - (1f - rig.mouth.squashMin) * amt
                                rotationZ = rig.mouth.squashRotation * amt
                                translationY += rig.mouth.squashOffset[1] * amt * (size.height / canvasH)
                            } else {
                                sy *= 1f + 0.12f * amt
                            }
                        }
                        scaleY = sy
                        translationY += ty
                        alpha = if (state == WhaleState.Weak) 0.55f else 1f
                    }
            )
        }
    }
}
