package com.example.domain

import kotlin.math.abs

data class ParsedOcrItem(
    val name: String,
    val kind: ItemKind,
    val slot: GearSlot,
    val tier: GearTier,
    val enhancementLevel: Int,
    val stats: StatMap,
    val rawOcrText: String,
    val confidence: Float,
    val dedupSignature: String
) {
    fun toVaultItem(source: ScanSource): VaultItem = VaultItem(
        name = name,
        kind = kind,
        slot = slot,
        tier = tier,
        enhancementLevel = enhancementLevel,
        stats = stats,
        rawOcrText = rawOcrText,
        source = source
    )
}

object EhtOcrParser {

    private val noiseTokens = setOf(
        "equip", "unequip", "enhance", "salvage", "disassemble", "sell", "lock",
        "unlock", "close", "ok", "confirm", "cancel", "options", "effect", "stat",
        "stats", "grade", "level", "lv", "inventory", "bag", "storage", "hunter",
        "compare", "equipped", "item info", "rune info", "owned"
    )

    private val enhancementRegex = Regex("""\+(\d{1,2})\b""")

    private val numberPercentRegex = Regex("""([+-]?\d+(?:[.,]\d+)?)\s*%?""")
    private val decimalAspdRegex = Regex("""([+-]?0[.,]\d{1,3}|[+-]?\d+(?:[.,]\d+)?)\s*(?:sec|s)?""", RegexOption.IGNORE_CASE)

    fun parse(rawText: String): ParsedOcrItem? {
        if (rawText.isBlank()) return null

        val normalizedLines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.length >= 2 }

        if (normalizedLines.isEmpty()) return null

        val fullLower = normalizedLines.joinToString("\n").lowercase()

        val isRune = detectIsRune(normalizedLines, fullLower)
        val kind = if (isRune) ItemKind.RUNE else ItemKind.GEAR
        val slot = if (isRune) GearSlot.RUNE else detectGearSlot(fullLower)
        val tier = detectTier(fullLower)
        val enhancement = detectEnhancement(normalizedLines)
        val stats = extractStats(normalizedLines)
        val statCount = stats.nonZeroEntries().size

        // Strict HUD noise rejection for live screen monitoring / scrolling video OCR:
        // Must contain at least 1 valid EHT stat AND (2+ stats OR explicit slot/tier/rune clue).
        val hasStrongKeyword = isRune || hasExplicitSlotOrTierKeyword(fullLower)
        if (statCount == 0) return null
        if (statCount == 1 && !hasStrongKeyword) return null

        val name = extractItemName(normalizedLines, kind, slot, tier)
        val confidence = computeConfidence(statCount, hasStrongKeyword, name)

        val dedupSig = "${kind.name}|${slot.name}|${tier.name}|${stats.signatureKey()}"

        return ParsedOcrItem(
            name = name,
            kind = kind,
            slot = slot,
            tier = tier,
            enhancementLevel = enhancement,
            stats = stats,
            rawOcrText = rawText.trim(),
            confidence = confidence,
            dedupSignature = dedupSig
        )
    }

    private fun detectIsRune(lines: List<String>, fullLower: String): Boolean {
        if (fullLower.contains("rune")) return true
        val firstFew = lines.take(3).joinToString(" ").lowercase()
        return firstFew.contains("stone") && !firstFew.contains("weapon")
    }

    private fun detectGearSlot(fullLower: String): GearSlot {
        return when {
            fullLower.containsAny("weapon", "sword", "axe", "bow", "staff", "wand", "hammer", "mace", "scythe", "blade", "dagger", "spear") -> GearSlot.WEAPON
            fullLower.containsAny("armor", "plate", "robe", "mail", "chest", "tunic", "vest", "cuirass") -> GearSlot.ARMOR
            fullLower.containsAny("helm", "helmet", "crown", "hood", "hat", "cap", "circlet", "mask") -> GearSlot.HELM
            fullLower.containsAny("glove", "gloves", "gauntlet", "gauntlets", "bracer", "wrist") -> GearSlot.GLOVES
            fullLower.containsAny("boot", "boots", "greave", "greaves", "shoe", "shoes", "sabaton") -> GearSlot.BOOTS
            fullLower.containsAny("necklace", "amulet", "pendant", "choker", "collar") -> GearSlot.NECKLACE
            fullLower.containsAny("ring", "band", "signet") -> GearSlot.RING
            fullLower.containsAny("belt", "sash", "girdle", "waist") -> GearSlot.BELT
            else -> GearSlot.WEAPON
        }
    }

    private fun detectTier(fullLower: String): GearTier {
        return when {
            fullLower.containsAny("chaos", "abyssal", "abyss") -> GearTier.CHAOS
            fullLower.containsAny("original", "origin") -> GearTier.ORIGINAL
            fullLower.containsAny("primal", "ancient") -> GearTier.PRIMAL
            fullLower.containsAny("world boss", "boss", "gaben", "baphomet", "darkness", "molen") -> GearTier.BOSS
            fullLower.contains("ultimate") -> GearTier.ULTIMATE
            fullLower.contains("legendary") -> GearTier.LEGENDARY
            else -> GearTier.HEROIC
        }
    }

    private fun hasExplicitSlotOrTierKeyword(fullLower: String): Boolean {
        return fullLower.containsAny(
            "chaos", "original", "primal", "ultimate", "legendary", "heroic", "boss",
            "weapon", "armor", "helm", "glove", "boot", "necklace", "ring", "belt",
            "sword", "axe", "bow", "staff", "rune"
        )
    }

    private fun detectEnhancement(lines: List<String>): Int {
        for (line in lines.take(4)) {
            val match = enhancementRegex.find(line)
            if (match != null) {
                val lvl = match.groupValues[1].toIntOrNull() ?: 0
                if (lvl in 1..30) return lvl
            }
        }
        return 0
    }

    private fun extractItemName(
        lines: List<String>,
        kind: ItemKind,
        slot: GearSlot,
        tier: GearTier
    ): String {
        val candidate = lines.firstOrNull { line ->
            val lower = line.lowercase()
            val hasDigitPercent = line.contains("%") || Regex("""[+-]\d+(\.\d+)?\s*%""").containsMatchIn(line)
            val isNoise = noiseTokens.any { lower == it || lower.startsWith("$it ") }
            val isPureNumber = line.replace(Regex("""[+\-\s.,/]"""), "").all { it.isDigit() }
            !hasDigitPercent && !isNoise && !isPureNumber && line.length in 3..42
        }
        return candidate?.replace(enhancementRegex, "")?.trim()?.ifBlank { null }
            ?: if (kind == ItemKind.RUNE) "${tier.displayName} Rune" else "${tier.displayName} ${slot.displayName}"
    }

    private fun extractStats(lines: List<String>): StatMap {
        var map = StatMap()
        for (rawLine in lines) {
            val line = rawLine
                .replace("O%", "0%")
                .replace("o%", "0%")
                .replace(",", ".")
            val lower = line.lowercase()

            when {
                lower.containsAny("atk spd", "attack spd", "atk speed", "attack speed", "aspd") -> {
                    val v = extractAspdValue(line)
                    if (v > 0f) map = map.copy(atkSpd = maxOf(map.atkSpd, v))
                }
                lower.containsAny("crit dmg", "critical dmg", "crit damage", "critical damage", "cri dmg") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(critDmg = maxOf(map.critDmg, v))
                }
                lower.containsAny("crit chance", "crit rate", "critical chance", "critical rate", "critical hit", "crit hit", "cri chance") -> {
                    val v = extractPercentValue(line, maxCap = 60f)
                    if (v > 0f) map = map.copy(critChance = maxOf(map.critChance, v))
                }
                lower.containsAny("evasion", "dodge", "eva ") || lower.startsWith("eva") -> {
                    val v = extractPercentValue(line, maxCap = 50f)
                    if (v > 0f) map = map.copy(evasion = maxOf(map.evasion, v))
                }
                lower.containsAny("dmg reduction", "damage reduction", "dmg taken", "damage taken", "dmg red") -> {
                    val v = extractPercentValue(line, maxCap = 60f)
                    if (v > 0f) map = map.copy(dmgReduction = maxOf(map.dmgReduction, v))
                }
                lower.containsAny("lifesteal", "life steal", "hp absorb", "drain", "leech") -> {
                    val v = extractPercentValue(line, maxCap = 40f)
                    if (v > 0f) map = map.copy(lifesteal = maxOf(map.lifesteal, v))
                }
                lower.containsAny("ignore def", "ignore defense", "def penetration", "penetration") -> {
                    val v = extractPercentValue(line, maxCap = 60f)
                    if (v > 0f) map = map.copy(ignoreDef = maxOf(map.ignoreDef, v))
                }
                lower.containsAny("boss dmg", "boss damage", "dmg to boss") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(bossDmg = maxOf(map.bossDmg, v))
                }
                lower.containsAny("demon dmg", "demon damage", "dmg to demon", "vs demon") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(demonDmg = maxOf(map.demonDmg, v))
                }
                lower.containsAny("primate dmg", "primate damage", "dmg to primate", "vs primate") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(primateDmg = maxOf(map.primateDmg, v))
                }
                lower.containsAny("animal dmg", "animal damage", "dmg to animal", "vs animal") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(animalDmg = maxOf(map.animalDmg, v))
                }
                lower.containsAny("undead dmg", "undead damage", "dmg to undead", "vs undead") -> {
                    val v = extractPercentValue(line, maxCap = 120f)
                    if (v > 0f) map = map.copy(undeadDmg = maxOf(map.undeadDmg, v))
                }
                lower.containsAny("cast", "skill cooldown", "cooldown") -> {
                    val v = extractPercentValue(line, maxCap = 45f)
                    if (v > 0f) map = map.copy(skillCast = maxOf(map.skillCast, v))
                }
                lower.containsAny("move spd", "move speed", "movement speed", "mv spd") -> {
                    val v = extractPercentValue(line, maxCap = 60f)
                    if (v > 0f) map = map.copy(moveSpd = maxOf(map.moveSpd, v))
                }
                lower.containsAny("defense", "def ") || lower.startsWith("def") -> {
                    if (!lower.contains("ignore")) {
                        val v = extractPercentValue(line, maxCap = 80f)
                        if (v > 0f) map = map.copy(defPct = maxOf(map.defPct, v))
                    }
                }
                lower.containsAny("max hp", "hp +", "hp%", "health") -> {
                    val v = extractPercentValue(line, maxCap = 80f)
                    if (v > 0f) map = map.copy(hpPct = maxOf(map.hpPct, v))
                }
                lower.containsAny("attack", "atk") -> {
                    val v = extractPercentValue(line, maxCap = 90f)
                    if (v > 0f) map = map.copy(atkPct = maxOf(map.atkPct, v))
                }
            }
        }
        return map
    }

    private fun extractPercentValue(line: String, maxCap: Float): Float {
        val matches = numberPercentRegex.findAll(line).toList()
        for (m in matches.asReversed()) {
            val raw = m.groupValues[1].replace(",", ".").toFloatOrNull() ?: continue
            val mag = abs(raw)
            if (mag in 0.5f..maxCap) return mag
        }
        return 0f
    }

    private fun extractAspdValue(line: String): Float {
        val isPercent = line.contains("%")
        val matches = decimalAspdRegex.findAll(line).toList()
        for (m in matches.asReversed()) {
            val raw = m.groupValues[1].replace(",", ".").toFloatOrNull() ?: continue
            val mag = abs(raw)
            if (isPercent && mag in 1f..30f) {
                // Convert EHT percentage attack speed into flat 0.25-scale equivalent
                return (mag / 100f).coerceIn(0.01f, 0.25f)
            }
            if (!isPercent && mag in 0.01f..0.30f) {
                return mag
            }
        }
        return 0f
    }

    private fun computeConfidence(statCount: Int, hasStrongKeyword: Boolean, name: String): Float {
        var score = 0.40f
        score += (statCount.coerceAtMost(4) * 0.12f)
        if (hasStrongKeyword) score += 0.12f
        if (name.length >= 5) score += 0.05f
        return score.coerceIn(0.35f, 0.99f)
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { this.contains(it) }
}
