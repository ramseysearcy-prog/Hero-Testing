package com.example.domain

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object BuildCodeCodec {
    private const val PREFIX = "HF1:"

    fun encode(build: CustomBuild): String {
        val root = JSONObject().apply {
            put("v", 1)
            put("n", build.name.take(60))
            put("r", build.role.name)
            put("tr", build.targetRace.name)
            put("nt", build.notes.take(240))
            val arr = JSONArray()
            build.statRanges.values.forEach { range ->
                arr.put(
                    JSONObject().apply {
                        put("s", range.statType.name)
                        put("min", range.minDesired.toDouble())
                        put("max", range.maxDesired.toDouble())
                        put("w", range.priorityWeight.toDouble())
                    }
                )
            }
            put("rg", arr)
        }
        val rawBytes = root.toString().toByteArray(Charsets.UTF_8)
        val compressed = deflate(rawBytes)
        val b64 = Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$PREFIX$b64"
    }

    fun decode(rawCode: String): Result<CustomBuild> {
        val trimmed = rawCode.trim()
        if (!trimmed.startsWith(PREFIX, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Build code must start with '$PREFIX'."))
        }
        return try {
            val payload = trimmed.substring(PREFIX.length).trim()
            val compressed = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val jsonStr = inflate(compressed).toString(Charsets.UTF_8)
            val root = JSONObject(jsonStr)

            val name = root.optString("n", "Imported Build").ifBlank { "Imported Build" }
            val roleName = root.optString("r", HeroRole.MAIN_CARRY.name)
            val raceName = root.optString("tr", TargetRace.DEMON.name)
            val notes = root.optString("nt", "")

            val role = enumValues<HeroRole>().firstOrNull { it.name == roleName } ?: HeroRole.MAIN_CARRY
            val race = enumValues<TargetRace>().firstOrNull { it.name == raceName } ?: TargetRace.DEMON

            val rangesMap = linkedMapOf<StatType, StatTargetRange>()
            val arr = root.optJSONArray("rg") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val statName = obj.optString("s", "")
                val statType = enumValues<StatType>().firstOrNull { it.name == statName } ?: continue
                val minV = obj.optDouble("min", 0.0).toFloat().coerceAtLeast(0f)
                val maxV = obj.optDouble("max", statType.suggestedLoadoutMax.toDouble()).toFloat().coerceAtLeast(minV)
                val weight = obj.optDouble("w", 2.0).toFloat().coerceIn(0.5f, 4.0f)
                rangesMap[statType] = StatTargetRange(statType, minV, maxV, weight)
            }

            Result.success(
                CustomBuild(
                    id = 0L,
                    name = name,
                    role = role,
                    targetRace = race,
                    notes = notes,
                    statRanges = rangesMap
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid or corrupted Hunter Forge build code."))
        }
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream(input.size)
        val buf = ByteArray(256)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            out.write(buf, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(input)
        val out = ByteArrayOutputStream(input.size * 2)
        val buf = ByteArray(256)
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0 && inflater.needsInput()) break
            out.write(buf, 0, count)
        }
        inflater.end()
        return out.toByteArray()
    }
}
