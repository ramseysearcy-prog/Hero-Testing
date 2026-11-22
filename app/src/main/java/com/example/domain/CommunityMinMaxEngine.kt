package com.example.domain

import kotlin.math.min
import kotlin.math.roundToInt

data class RollEvaluation(
    val efficiencyPct: Int,
    val gradeLabel: String,
    val verdict: String,
    val topHighlights: List<String>
)

data class BuildFitResult(
    val buildName: String,
    val fitPercent: Int,
    val matchedStats: List<String>
)

data class StatDeltaRow(
    val statType: StatType,
    val valueA: Float,
    val valueB: Float,
    val deltaBMinusA: Float
)

data class ItemComparisonResult(
    val itemA: VaultItem,
    val itemB: VaultItem,
    val evalA: RollEvaluation,
    val evalB: RollEvaluation,
    val statRows: List<StatDeltaRow>,
    val recommendedForHero: String,
    val winnerLabel: String
)

data class CapProgress(
    val label: String,
    val rawValue: Float,
    val effectiveValue: Float,
    val capValue: Float,
    val isFlatDecimal: Boolean,
    val isOvercapped: Boolean,
    val minTarget: Float? = null,
    val maxTarget: Float? = null
)

data class SlotRecommendation(
    val slot: GearSlot,
    val bestItem: VaultItem?,
    val runnerUpItem: VaultItem?,
    val rationale: String
)

data class RuneRecommendation(
    val slotIndex: Int,
    val rune: VaultItem,
    val rationale: String
)

data class OptimizedLoadoutResult(
    val hero: HeroProfile,
    val assignedBuild: CustomBuild?,
    val slotRecommendations: List<SlotRecommendation>,
    val runeRecommendations: List<RuneRecommendation>,
    val rawTotals: StatMap,
    val effectiveTotals: StatMap,
    val capProgressList: List<CapProgress>,
    val buildRangeProgressList: List<CapProgress>,
    val overcapWarnings: List<String>,
    val communityNotes: List<String>,
    val combatScore: Double
)

data class CommunityGuideSection(
    val id: String,
    val title: String,
    val badge: String,
    val summary: String,
    val keyRules: List<String>
)

data class GearChangeAction(
    val id: String,
    val heroId: Long,
    val heroName: String,
    val heroAdvancement: HeroAdvancement,
    val heroRole: HeroRole,
    val buildName: String?,
    val slotLabel: String,
    val slot: GearSlot,
    val isRune: Boolean,
    val currentItem: VaultItem?,
    val suggestedItem: VaultItem,
    val previousOwnerHeroName: String?,
    val rationale: String,
    val statGainHighlights: List<String>
)

data class HeroChangeGroup(
    val hero: HeroProfile,
    val assignedBuild: CustomBuild?,
    val recommendedLoadout: OptimizedLoadoutResult,
    val changes: List<GearChangeAction>
)

data class RosterChangePlan(
    val heroGroups: List<HeroChangeGroup>,
    val totalChangesCount: Int,
    val heroesNeedingChangesCount: Int
)

object CommunityMinMaxEngine {

    const val CAP_ASPD_BONUS = 0.25f
    const val CAP_CRIT_CHANCE = 50f
    const val CAP_EVASION = 40f
    const val SOFT_CAP_DMG_REDUCTION = 70f
    const val SOFT_CAP_LIFESTEAL = 20f

    fun evaluateItemRoll(item: VaultItem): RollEvaluation {
        val entries = item.stats.nonZeroEntries()
        if (entries.isEmpty()) {
            return RollEvaluation(0, "D", "No recognized combat stats", emptyList())
        }

        val scale = item.tier.singleLineCeilingScale
        val lineRatios = entries.map { (type, value) ->
            val maxLine = type.maxSingleRollBenchmark * scale
            (value / maxLine).coerceIn(0f, 1.15f)
        }
        val avgRatio = lineRatios.average().toFloat()
        val lineCountBonus = when {
            item.kind == ItemKind.RUNE -> 0.06f
            entries.size >= 4 -> 0.10f
            entries.size == 3 -> 0.03f
            entries.size == 2 -> -0.10f
            else -> -0.22f
        }
        val synergyBonus = computeItemSynergyBonus(item)
        val eff = (((avgRatio * 0.85f) + lineCountBonus + synergyBonus) * 100f)
            .roundToInt()
            .coerceIn(18, 100)

        val (grade, verdict) = when {
            eff >= 90 -> "S+" to "God Roll • Lock & Enhance to +20"
            eff >= 80 -> "S" to "BiS Keeper • Endgame Worthy"
            eff >= 68 -> "A" to "Strong Starter / Transitional"
            eff >= 52 -> "B" to "Usable Placeholder"
            else -> "C" to "Salvage / Reroll Candidate"
        }

        val highlights = entries
            .sortedByDescending { (type, value) -> value / (type.maxSingleRollBenchmark * scale) }
            .take(3)
            .map { (type, value) -> "${type.shortLabel} ${type.formatValue(value)}" }

        return RollEvaluation(eff, grade, verdict, highlights)
    }

    fun evaluateItemFitForBuild(item: VaultItem, build: CustomBuild): BuildFitResult {
        if (build.statRanges.isEmpty()) {
            val roll = evaluateItemRoll(item)
            return BuildFitResult(build.name, roll.efficiencyPct, roll.topHighlights)
        }

        val totalPriority = build.statRanges.values.sumOf { it.priorityWeight.toDouble() }.coerceAtLeast(1.0)
        var matchedScore = 0.0
        val matchedLabels = mutableListOf<String>()
        val scale = item.tier.singleLineCeilingScale

        for ((statType, range) in build.statRanges) {
            val itemVal = item.stats.valueOf(statType)
            if (itemVal > 0.001f && range.maxDesired > 0f) {
                val benchmark = statType.maxSingleRollBenchmark * scale
                val quality = (itemVal / benchmark).coerceIn(0.2f, 1.15f)
                matchedScore += range.priorityWeight * quality
                matchedLabels.add("${statType.shortLabel} ${statType.formatValue(itemVal)}")
            }
        }

        // Normalize against top ~3.5 weighted lines an item/rune can carry
        val denominator = if (item.kind == ItemKind.RUNE) {
            build.statRanges.values.sortedByDescending { it.priorityWeight }.take(2).sumOf { it.priorityWeight.toDouble() }.coerceAtLeast(1.0)
        } else {
            build.statRanges.values.sortedByDescending { it.priorityWeight }.take(4).sumOf { it.priorityWeight.toDouble() }.coerceAtLeast(1.0)
        }

        val pct = ((matchedScore / denominator.coerceAtMost(totalPriority)) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        return BuildFitResult(build.name, pct, matchedLabels)
    }

    private fun computeItemSynergyBonus(item: VaultItem): Float {
        val s = item.stats
        var bonus = 0f
        if (s.atkSpd >= 0.08f && (s.critChance >= 8f || s.critDmg >= 22f)) bonus += 0.05f
        if (s.atkPct >= 14f && s.critDmg >= 25f) bonus += 0.05f
        if (s.dmgReduction >= 12f && s.evasion >= 8f) bonus += 0.05f
        if (s.bossDmg >= 25f && s.critDmg >= 25f) bonus += 0.04f
        return bonus
    }

    fun compareItems(
        itemA: VaultItem,
        itemB: VaultItem,
        hero: HeroProfile?,
        assignedBuild: CustomBuild? = null
    ): ItemComparisonResult {
        val evalA = evaluateItemRoll(itemA)
        val evalB = evaluateItemRoll(itemB)

        val rows = StatType.entries.mapNotNull { type ->
            val a = itemA.stats.valueOf(type)
            val b = itemB.stats.valueOf(type)
            if (a > 0.001f || b > 0.001f) {
                StatDeltaRow(type, a, b, b - a)
            } else {
                null
            }
        }

        val targetHero = hero ?: HeroProfile(
            name = "Standard Chaos Carry",
            baseClass = HeroBaseClass.BERSERKER,
            advancement = HeroAdvancement.EXECUTIONER
        )
        val weights = weightsForHero(targetHero, assignedBuild)
        val scoreA = scoreStatMapMarginal(itemA.stats, weights) + itemA.tier.rank * 3.0
        val scoreB = scoreStatMapMarginal(itemB.stats, weights) + itemB.tier.rank * 3.0

        val winner = when {
            scoreB > scoreA + 2.0 -> "${itemB.name} wins (+${(scoreB - scoreA).roundToInt()} pts)"
            scoreA > scoreB + 2.0 -> "${itemA.name} wins (+${(scoreA - scoreB).roundToInt()} pts)"
            else -> "Near tie • Choose based on ASPD/Crit cap needs"
        }

        val buildContext = assignedBuild?.let { " • Build: ${it.name}" } ?: ""
        val heroVerdict = "Evaluated for ${targetHero.advancement.displayName} (${targetHero.role.displayName})$buildContext"
        return ItemComparisonResult(itemA, itemB, evalA, evalB, rows, heroVerdict, winner)
    }

    fun optimizeLoadout(
        hero: HeroProfile,
        allItems: List<VaultItem>,
        ignoreItemsEquippedByOtherHeroes: Boolean,
        assignedBuild: CustomBuild? = null
    ): OptimizedLoadoutResult {
        val available = allItems.filter { item ->
            !ignoreItemsEquippedByOtherHeroes ||
                item.equippedHeroId == null ||
                item.equippedHeroId == hero.id
        }

        val weights = weightsForHero(hero, assignedBuild)
        val baseStats = heroBaseBuffStats(hero)

        val slotCandidates: Map<GearSlot, List<VaultItem>> = GearSlot.equipmentSlots.associateWith { slot ->
            available
                .filter { it.kind == ItemKind.GEAR && it.slot == slot }
                .sortedByDescending { scoreStatMapMarginal(it.stats, weights) + it.tier.rank * 2.5 }
                .take(4)
        }

        data class BeamState(
            val chosen: Map<GearSlot, VaultItem>,
            val accumulatedStats: StatMap,
            val score: Double
        )

        var beam = listOf(
            BeamState(
                chosen = emptyMap(),
                accumulatedStats = baseStats,
                score = scoreCappedStats(baseStats, hero, assignedBuild, weights)
            )
        )

        for (slot in GearSlot.equipmentSlots) {
            val candidates = slotCandidates[slot].orEmpty()
            if (candidates.isEmpty()) continue
            val nextStates = ArrayList<BeamState>(beam.size * candidates.size)
            for (state in beam) {
                for (candidate in candidates) {
                    val combined = state.accumulatedStats + candidate.stats
                    val tierBonus = candidate.tier.rank * 2.0 + candidate.enhancementLevel * 0.4
                    val totalScore = scoreCappedStats(combined, hero, assignedBuild, weights) + tierBonus
                    nextStates.add(
                        BeamState(
                            chosen = state.chosen + (slot to candidate),
                            accumulatedStats = combined,
                            score = totalScore
                        )
                    )
                }
            }
            beam = nextStates.sortedByDescending { it.score }.take(32)
        }

        val bestGearState = beam.first()
        var currentStats = bestGearState.accumulatedStats

        val runePool = available.filter { it.kind == ItemKind.RUNE }.toMutableList()
        val chosenRunes = mutableListOf<RuneRecommendation>()
        val maxRunes = hero.runeSlots.coerceIn(1, 5)

        for (slotIdx in 1..maxRunes) {
            if (runePool.isEmpty()) break
            val beforeScore = scoreCappedStats(currentStats, hero, assignedBuild, weights)
            val bestRune = runePool.maxByOrNull { rune ->
                scoreCappedStats(currentStats + rune.stats, hero, assignedBuild, weights) - beforeScore + rune.tier.rank * 1.5
            } ?: break

            val delta = scoreCappedStats(currentStats + bestRune.stats, hero, assignedBuild, weights) - beforeScore
            if (delta <= 0.1 && chosenRunes.isNotEmpty()) break

            val rationale = buildRuneRationale(bestRune, currentStats, hero, assignedBuild)
            chosenRunes.add(RuneRecommendation(slotIdx, bestRune, rationale))
            currentStats += bestRune.stats
            runePool.remove(bestRune)
        }

        val slotRecommendations = GearSlot.equipmentSlots.map { slot ->
            val chosen = bestGearState.chosen[slot]
            val candidates = slotCandidates[slot].orEmpty()
            val runnerUp = candidates.firstOrNull { it.id != chosen?.id }
            val rationale = if (chosen == null) {
                "No ${slot.displayName} logged in Vault yet — scan or add one!"
            } else {
                buildSlotRationale(chosen, hero, assignedBuild)
            }
            SlotRecommendation(slot, chosen, runnerUp, rationale)
        }

        val effective = applyCaps(currentStats, assignedBuild)
        val caps = buildCapProgress(currentStats, effective)
        val buildRangeProgress = buildCustomRangeProgress(currentStats, assignedBuild)
        val warnings = buildOvercapWarnings(currentStats, hero, assignedBuild)
        val notes = buildCommunityNotes(hero, assignedBuild, currentStats, chosenRunes.size)
        val finalScore = scoreCappedStats(currentStats, hero, assignedBuild, weights)

        return OptimizedLoadoutResult(
            hero = hero,
            assignedBuild = assignedBuild,
            slotRecommendations = slotRecommendations,
            runeRecommendations = chosenRunes,
            rawTotals = currentStats,
            effectiveTotals = effective,
            capProgressList = caps,
            buildRangeProgressList = buildRangeProgress,
            overcapWarnings = warnings,
            communityNotes = notes,
            combatScore = finalScore
        )
    }

    /**
     * Plans conflict-free gear and rune swaps across every hero in the roster at once,
     * comparing each hero's currently equipped items against the optimal available allocation.
     */
    fun planAllHeroGearChanges(
        heroes: List<HeroProfile>,
        allItems: List<VaultItem>,
        buildsById: Map<Long, CustomBuild>
    ): RosterChangePlan {
        if (heroes.isEmpty()) {
            return RosterChangePlan(emptyList(), 0, 0)
        }

        val heroesById = heroes.associateBy { it.id }
        val reservedItemIds = mutableSetOf<Long>()
        val groups = mutableListOf<HeroChangeGroup>()

        for (hero in heroes) {
            val availableForHero = allItems.filter { it.id !in reservedItemIds }
            val assignedBuild = hero.assignedBuildId?.let { buildsById[it] }
            val optimized = optimizeLoadout(
                hero = hero,
                allItems = availableForHero,
                ignoreItemsEquippedByOtherHeroes = false,
                assignedBuild = assignedBuild
            )

            // Reserve all items & runes assigned to this hero so subsequent heroes won't double-claim them
            optimized.slotRecommendations.forEach { rec ->
                rec.bestItem?.let { reservedItemIds.add(it.id) }
            }
            optimized.runeRecommendations.forEach { runeRec ->
                reservedItemIds.add(runeRec.rune.id)
            }

            val currentlyEquipped = allItems.filter { it.equippedHeroId == hero.id }
            val currentGearBySlot = currentlyEquipped
                .filter { it.kind == ItemKind.GEAR }
                .associateBy { it.slot }

            val actions = mutableListOf<GearChangeAction>()

            // 1. Compare 8 equipment slots
            for (rec in optimized.slotRecommendations) {
                val suggested = rec.bestItem ?: continue
                val current = currentGearBySlot[rec.slot]
                if (suggested.id != current?.id) {
                    val prevOwner = suggested.equippedHeroId
                        ?.takeIf { it != hero.id }
                        ?.let { heroesById[it]?.name }
                    actions.add(
                        GearChangeAction(
                            id = "hero_${hero.id}_slot_${rec.slot.name}_${suggested.id}",
                            heroId = hero.id,
                            heroName = hero.name,
                            heroAdvancement = hero.advancement,
                            heroRole = hero.role,
                            buildName = assignedBuild?.name,
                            slotLabel = rec.slot.displayName,
                            slot = rec.slot,
                            isRune = false,
                            currentItem = current,
                            suggestedItem = suggested,
                            previousOwnerHeroName = prevOwner,
                            rationale = rec.rationale,
                            statGainHighlights = computeStatDiffHighlights(current?.stats, suggested.stats)
                        )
                    )
                }
            }

            // 2. Compare runes
            val currentRunes = currentlyEquipped.filter { it.kind == ItemKind.RUNE }
            val currentRuneIds = currentRunes.map { it.id }.toSet()
            val suggestedRuneIds = optimized.runeRecommendations.map { it.rune.id }.toSet()
            val unusedCurrentRunes = currentRunes.filter { it.id !in suggestedRuneIds }.toMutableList()

            for (runeRec in optimized.runeRecommendations) {
                val suggestedRune = runeRec.rune
                if (suggestedRune.id !in currentRuneIds) {
                    val replaced = unusedCurrentRunes.removeFirstOrNull()
                    val prevOwner = suggestedRune.equippedHeroId
                        ?.takeIf { it != hero.id }
                        ?.let { heroesById[it]?.name }
                    actions.add(
                        GearChangeAction(
                            id = "hero_${hero.id}_rune_${runeRec.slotIndex}_${suggestedRune.id}",
                            heroId = hero.id,
                            heroName = hero.name,
                            heroAdvancement = hero.advancement,
                            heroRole = hero.role,
                            buildName = assignedBuild?.name,
                            slotLabel = "Rune #${runeRec.slotIndex}",
                            slot = GearSlot.RUNE,
                            isRune = true,
                            currentItem = replaced,
                            suggestedItem = suggestedRune,
                            previousOwnerHeroName = prevOwner,
                            rationale = runeRec.rationale,
                            statGainHighlights = computeStatDiffHighlights(replaced?.stats, suggestedRune.stats)
                        )
                    )
                }
            }

            groups.add(
                HeroChangeGroup(
                    hero = hero,
                    assignedBuild = assignedBuild,
                    recommendedLoadout = optimized,
                    changes = actions
                )
            )
        }

        val totalChanges = groups.sumOf { it.changes.size }
        val heroesWithChanges = groups.count { it.changes.isNotEmpty() }
        return RosterChangePlan(
            heroGroups = groups,
            totalChangesCount = totalChanges,
            heroesNeedingChangesCount = heroesWithChanges
        )
    }

    private fun computeStatDiffHighlights(old: StatMap?, new: StatMap): List<String> {
        val deltas = StatType.entries.mapNotNull { type ->
            val oldVal = old?.valueOf(type) ?: 0f
            val newVal = new.valueOf(type)
            val delta = newVal - oldVal
            val threshold = if (type.isFlatDecimal) 0.005f else 0.4f
            if (kotlin.math.abs(delta) >= threshold) {
                type to delta
            } else {
                null
            }
        }
        return deltas
            .sortedByDescending { (type, delta) ->
                val scaled = if (type.isFlatDecimal) delta * 100f else delta
                // Show biggest positive gains first, then major trade-offs
                if (scaled > 0f) scaled + 1000f else kotlin.math.abs(scaled)
            }
            .take(4)
            .map { (type, delta) -> "${type.shortLabel} ${type.formatDelta(delta)}" }
    }

    fun heroBaseBuffStats(hero: HeroProfile): StatMap {
        val traitStats = hero.trait.bonusStats
        val quickStrikeAspd = if (hero.hasQuickStrikeBuff) 0.06f else 0f
        val guildAspd = (hero.guildAspdBuffPct / 100f) * 0.25f
        return traitStats.copy(
            atkSpd = traitStats.atkSpd + quickStrikeAspd + guildAspd,
            critChance = traitStats.critChance + hero.guildCritBuffPct
        )
    }

    private fun applyCaps(raw: StatMap, assignedBuild: CustomBuild?): StatMap {
        val aspdCap = assignedBuild?.statRanges?.get(StatType.ATK_SPD)?.maxDesired?.coerceAtMost(CAP_ASPD_BONUS) ?: CAP_ASPD_BONUS
        val critCap = assignedBuild?.statRanges?.get(StatType.CRIT_CHANCE)?.maxDesired?.coerceAtMost(CAP_CRIT_CHANCE) ?: CAP_CRIT_CHANCE
        val evaCap = assignedBuild?.statRanges?.get(StatType.EVASION)?.maxDesired?.coerceAtMost(CAP_EVASION) ?: CAP_EVASION

        return raw.copy(
            atkSpd = min(raw.atkSpd, aspdCap),
            critChance = min(raw.critChance, critCap),
            evasion = min(raw.evasion, evaCap),
            dmgReduction = min(raw.dmgReduction, SOFT_CAP_DMG_REDUCTION),
            lifesteal = min(raw.lifesteal, SOFT_CAP_LIFESTEAL)
        )
    }

    private data class StatWeights(
        val atk: Double,
        val aspdUnit: Double,
        val critChance: Double,
        val critDmg: Double,
        val evasion: Double,
        val def: Double,
        val hp: Double,
        val dmgRed: Double,
        val lifesteal: Double,
        val bossDmg: Double,
        val targetRaceDmg: Double,
        val ignoreDef: Double,
        val skillCast: Double,
        val moveSpd: Double
    )

    private fun weightsForHero(hero: HeroProfile, build: CustomBuild?): StatWeights {
        val effectiveRole = build?.role ?: hero.role
        val base = when {
            effectiveRole == HeroRole.TANK_ANCHOR || hero.archetype == BuildArchetype.TANK_FRONTLINE -> StatWeights(
                atk = 0.75, aspdUnit = 2.4, critChance = 1.0, critDmg = 0.8,
                evasion = 3.3, def = 1.8, hp = 1.6, dmgRed = 3.6, lifesteal = 2.1,
                bossDmg = 0.35, targetRaceDmg = 0.35, ignoreDef = 0.5, skillCast = 0.8, moveSpd = 0.3
            )
            effectiveRole == HeroRole.BOSS_SHREDDER || hero.archetype == BuildArchetype.WORLD_BOSS || hero.archetype == BuildArchetype.MOLEN_RAID -> StatWeights(
                atk = 1.85, aspdUnit = 4.8, critChance = 2.6, critDmg = 1.8,
                evasion = 1.1, def = 0.35, hp = 0.35, dmgRed = 0.9, lifesteal = 0.85,
                bossDmg = 2.0, targetRaceDmg = 0.35, ignoreDef = 1.9,
                skillCast = if (hero.baseClass == HeroBaseClass.SORCERER) 2.1 else 0.8, moveSpd = 0.2
            )
            effectiveRole == HeroRole.PVP_BRUISER || hero.archetype == BuildArchetype.COLOSSEUM_PVP -> StatWeights(
                atk = 1.35, aspdUnit = 3.8, critChance = 2.0, critDmg = 1.35,
                evasion = 3.1, def = 1.1, hp = 1.15, dmgRed = 2.9, lifesteal = 1.6,
                bossDmg = 0.0, targetRaceDmg = 0.0, ignoreDef = 2.2,
                skillCast = if (hero.baseClass == HeroBaseClass.SORCERER) 2.3 else 0.9, moveSpd = 0.7
            )
            else -> StatWeights(
                atk = 1.65, aspdUnit = 4.6, critChance = 2.4, critDmg = 1.45,
                evasion = 2.1, def = 0.45, hp = 0.40, dmgRed = 1.15, lifesteal = 0.90,
                bossDmg = 0.45, targetRaceDmg = 1.55, ignoreDef = 1.10,
                skillCast = if (hero.baseClass == HeroBaseClass.SORCERER) 2.2 else 0.7, moveSpd = 0.65
            )
        }

        // Apply custom build priority multipliers if assigned
        val boosted = if (build != null && build.statRanges.isNotEmpty()) {
            fun mul(type: StatType, defaultVal: Double): Double {
                val r = build.statRanges[type] ?: return defaultVal * 0.55
                return defaultVal * (0.65 + r.priorityWeight * 0.65)
            }
            base.copy(
                atk = mul(StatType.ATK_PCT, base.atk),
                aspdUnit = mul(StatType.ATK_SPD, base.aspdUnit),
                critChance = mul(StatType.CRIT_CHANCE, base.critChance),
                critDmg = mul(StatType.CRIT_DMG, base.critDmg),
                evasion = mul(StatType.EVASION, base.evasion),
                def = mul(StatType.DEF_PCT, base.def),
                hp = mul(StatType.HP_PCT, base.hp),
                dmgRed = mul(StatType.DMG_REDUCTION, base.dmgRed),
                lifesteal = mul(StatType.LIFESTEAL, base.lifesteal),
                bossDmg = mul(StatType.BOSS_DMG, base.bossDmg),
                ignoreDef = mul(StatType.IGNORE_DEF, base.ignoreDef),
                skillCast = mul(StatType.SKILL_CAST, base.skillCast),
                moveSpd = mul(StatType.MOVE_SPD, base.moveSpd)
            )
        } else {
            base
        }

        return when (hero.baseClass) {
            HeroBaseClass.BERSERKER -> boosted.copy(aspdUnit = boosted.aspdUnit * 1.12, critDmg = boosted.critDmg * 1.08)
            HeroBaseClass.PALADIN -> boosted.copy(dmgRed = boosted.dmgRed * 1.15, evasion = boosted.evasion * 1.10)
            HeroBaseClass.RANGER -> boosted.copy(critChance = boosted.critChance * 1.10, critDmg = boosted.critDmg * 1.10)
            HeroBaseClass.SORCERER -> boosted.copy(skillCast = boosted.skillCast * 1.25, critDmg = boosted.critDmg * 1.06)
            HeroBaseClass.DARK_LORD -> boosted.copy(atk = boosted.atk * 1.08, ignoreDef = boosted.ignoreDef * 1.12)
        }
    }

    private fun scoreStatMapMarginal(stats: StatMap, w: StatWeights): Double {
        return stats.atkPct * w.atk +
            (stats.atkSpd * 100.0) * w.aspdUnit +
            stats.critChance * w.critChance +
            stats.critDmg * w.critDmg +
            stats.evasion * w.evasion +
            stats.defPct * w.def +
            stats.hpPct * w.hp +
            stats.dmgReduction * w.dmgRed +
            stats.lifesteal * w.lifesteal +
            stats.bossDmg * w.bossDmg +
            (stats.demonDmg + stats.primateDmg + stats.animalDmg + stats.undeadDmg) * (w.targetRaceDmg * 0.5) +
            stats.ignoreDef * w.ignoreDef +
            stats.skillCast * w.skillCast +
            stats.moveSpd * w.moveSpd
    }

    private fun scoreCappedStats(
        raw: StatMap,
        hero: HeroProfile,
        assignedBuild: CustomBuild?,
        w: StatWeights
    ): Double {
        val eff = applyCaps(raw, assignedBuild)
        val effectiveRace = assignedBuild?.targetRace ?: hero.targetRace
        val raceStat = when (effectiveRace) {
            TargetRace.ALL -> (raw.demonDmg + raw.primateDmg + raw.animalDmg + raw.undeadDmg) * 0.45f
            TargetRace.DEMON -> raw.demonDmg
            TargetRace.PRIMATE -> raw.primateDmg
            TargetRace.ANIMAL -> raw.animalDmg
            TargetRace.UNDEAD -> raw.undeadDmg
            TargetRace.BOSS -> raw.bossDmg * 0.4f
        }

        val overAspd = (raw.atkSpd - CAP_ASPD_BONUS).coerceAtLeast(0f) * 100f
        val overCrit = (raw.critChance - CAP_CRIT_CHANCE).coerceAtLeast(0f)
        val overEva = (raw.evasion - CAP_EVASION).coerceAtLeast(0f)
        val overPenalty = overAspd * 0.6 + overCrit * 0.35 + overEva * 0.35

        // Custom build desired [min..max] range bonus / penalty
        var buildRangeAdjustment = 0.0
        if (assignedBuild != null && assignedBuild.statRanges.isNotEmpty()) {
            for ((statType, range) in assignedBuild.statRanges) {
                val actual = raw.valueOf(statType)
                val unitScale = if (statType.isFlatDecimal) 100.0 else 1.0
                val clamped = actual.coerceAtMost(range.maxDesired)
                // Reward progress inside desired range, and give strong bonus for meeting minDesired!
                buildRangeAdjustment += (clamped * unitScale) * (range.priorityWeight * 0.35)
                if (range.minDesired > 0f) {
                    if (actual >= range.minDesired) {
                        buildRangeAdjustment += 28.0 * range.priorityWeight
                    } else {
                        val deficit = (range.minDesired - actual) * unitScale
                        buildRangeAdjustment -= deficit * (0.45 * range.priorityWeight)
                    }
                }
                if (actual > range.maxDesired) {
                    val over = (actual - range.maxDesired) * unitScale
                    buildRangeAdjustment -= over * 0.25
                }
            }
        }

        return eff.atkPct * w.atk +
            (eff.atkSpd * 100.0) * w.aspdUnit +
            eff.critChance * w.critChance +
            eff.critDmg * w.critDmg +
            eff.evasion * w.evasion +
            eff.defPct * w.def +
            eff.hpPct * w.hp +
            eff.dmgReduction * w.dmgRed +
            eff.lifesteal * w.lifesteal +
            eff.bossDmg * w.bossDmg +
            raceStat * w.targetRaceDmg +
            eff.ignoreDef * w.ignoreDef +
            eff.skillCast * w.skillCast +
            eff.moveSpd * w.moveSpd -
            overPenalty +
            buildRangeAdjustment
    }

    private fun buildSlotRationale(item: VaultItem, hero: HeroProfile, build: CustomBuild?): String {
        if (build != null) {
            val fit = evaluateItemFitForBuild(item, build)
            if (fit.matchedStats.isNotEmpty()) {
                return "${fit.fitPercent}% fit for '${build.name}' (${fit.matchedStats.take(3).joinToString(", ")})"
            }
        }
        val s = item.stats
        val parts = mutableListOf<String>()
        if (s.atkSpd >= 0.05f) parts.add("pushes ASPD toward 0.25 cap")
        if (s.critChance >= 6f) parts.add("boosts Crit toward 50%")
        if (s.critDmg >= 20f) parts.add("high Crit DMG scaling")
        if (s.dmgReduction >= 10f && (hero.role == HeroRole.TANK_ANCHOR || hero.archetype == BuildArchetype.TANK_FRONTLINE)) {
            parts.add("key DMG Reduction")
        }
        val raceType = (build?.targetRace ?: hero.targetRace).statType
        if (raceType != null && s.valueOf(raceType) >= 15f) {
            parts.add("matches ${(build?.targetRace ?: hero.targetRace).displayName}")
        }
        if (parts.isEmpty()) {
            val eval = evaluateItemRoll(item)
            return "${item.tier.displayName} ${eval.gradeLabel} (${eval.efficiencyPct}%) — best available stat roll"
        }
        return parts.joinToString(" • ").replaceFirstChar { it.uppercase() }
    }

    private fun buildRuneRationale(
        rune: VaultItem,
        preRuneStats: StatMap,
        hero: HeroProfile,
        build: CustomBuild?
    ): String {
        val s = rune.stats
        val targetRace = build?.targetRace ?: hero.targetRace
        return when {
            s.atkSpd > 0f && preRuneStats.atkSpd < CAP_ASPD_BONUS ->
                "Closes Attack Speed gap (${String.format(java.util.Locale.US, "%.2f", preRuneStats.atkSpd)} -> ${String.format(java.util.Locale.US, "%.2f", min(CAP_ASPD_BONUS, preRuneStats.atkSpd + s.atkSpd))})"
            s.critChance > 0f && preRuneStats.critChance < CAP_CRIT_CHANCE ->
                "Pushes Crit Chance closer to 50% cap"
            s.evasion > 0f && preRuneStats.evasion < CAP_EVASION ->
                "Improves survival toward 40% Evasion cap"
            s.critDmg >= 15f ->
                "Pure Crit DMG multiplier after hitting speed/crit targets"
            targetRace.statType != null && s.valueOf(targetRace.statType) > 0f ->
                "Amplifies ${targetRace.displayName} damage"
            else -> "Best remaining stat utility for ${build?.name ?: hero.advancement.displayName}"
        }
    }

    private fun buildCapProgress(raw: StatMap, eff: StatMap): List<CapProgress> = listOf(
        CapProgress("Attack Speed", raw.atkSpd, eff.atkSpd, CAP_ASPD_BONUS, true, raw.atkSpd > CAP_ASPD_BONUS + 0.01f),
        CapProgress("Crit Chance", raw.critChance, eff.critChance, CAP_CRIT_CHANCE, false, raw.critChance > CAP_CRIT_CHANCE + 2f),
        CapProgress("Evasion", raw.evasion, eff.evasion, CAP_EVASION, false, raw.evasion > CAP_EVASION + 2f),
        CapProgress("DMG Reduction", raw.dmgReduction, eff.dmgReduction, SOFT_CAP_DMG_REDUCTION, false, raw.dmgReduction > SOFT_CAP_DMG_REDUCTION + 3f),
        CapProgress("Lifesteal", raw.lifesteal, eff.lifesteal, SOFT_CAP_LIFESTEAL, false, raw.lifesteal > SOFT_CAP_LIFESTEAL + 3f)
    )

    private fun buildCustomRangeProgress(raw: StatMap, build: CustomBuild?): List<CapProgress> {
        if (build == null || build.statRanges.isEmpty()) return emptyList()
        return build.statRanges.values.map { range ->
            val current = raw.valueOf(range.statType)
            val maxCap = range.maxDesired.coerceAtLeast(0.01f)
            CapProgress(
                label = range.statType.label,
                rawValue = current,
                effectiveValue = current.coerceAtMost(maxCap),
                capValue = maxCap,
                isFlatDecimal = range.statType.isFlatDecimal,
                isOvercapped = current > range.maxDesired + (if (range.statType.isFlatDecimal) 0.01f else 2f),
                minTarget = range.minDesired,
                maxTarget = range.maxDesired
            )
        }
    }

    private fun buildOvercapWarnings(raw: StatMap, hero: HeroProfile, build: CustomBuild?): List<String> {
        val warnings = mutableListOf<String>()
        if (raw.atkSpd > CAP_ASPD_BONUS + 0.01f) {
            val waste = raw.atkSpd - CAP_ASPD_BONUS
            warnings.add("Attack Speed is overcapped by +${String.format(java.util.Locale.US, "%.2f", waste)} — swap an ASPD rune or gear line for Crit DMG or ATK%.")
        } else if (raw.atkSpd < CAP_ASPD_BONUS - 0.02f && hero.role != HeroRole.TANK_ANCHOR) {
            val missing = CAP_ASPD_BONUS - raw.atkSpd
            warnings.add("Missing ${String.format(java.util.Locale.US, "%.2f", missing)} Attack Speed to reach the 0.25 community breakpoint.")
        }

        if (raw.critChance > CAP_CRIT_CHANCE + 2f) {
            warnings.add("Crit Chance exceeds the 50% gear/trait cap by +${(raw.critChance - CAP_CRIT_CHANCE).roundToInt()}%.")
        }
        if (raw.evasion > CAP_EVASION + 2f) {
            warnings.add("Evasion exceeds the 40% hard cap by +${(raw.evasion - CAP_EVASION).roundToInt()}%.")
        }
        build?.statRanges?.values?.forEach { range ->
            val actual = raw.valueOf(range.statType)
            if (range.minDesired > 0f && actual < range.minDesired) {
                val gap = range.minDesired - actual
                warnings.add("${build.name}: ${range.statType.shortLabel} is ${range.statType.formatPlainValue(gap)} below your minimum target (${range.statType.formatPlainValue(range.minDesired)}).")
            }
        }
        return warnings
    }

    private fun buildCommunityNotes(
        hero: HeroProfile,
        build: CustomBuild?,
        raw: StatMap,
        runeCount: Int
    ): List<String> {
        val notes = mutableListOf<String>()
        if (build != null) {
            notes.add("Optimizing for Build '${build.name}' (${build.role.displayName}) targeting ${build.targetRace.displayName}.")
        }
        notes.add(hero.advancement.communityTip)
        notes.add("Trait (${hero.trait.displayName}) + Guild/Buff settings are factored into your cap math before gear selection.")
        if (runeCount == 0) {
            notes.add("No Runes in Vault yet — scan or log Runes to unlock joint gear + rune breakpoint filling!")
        } else if (raw.atkSpd >= CAP_ASPD_BONUS) {
            notes.add("0.25 Attack Speed cap reached! Community rule: invest remaining slots into Crit DMG%, ATK%, and ${(build?.targetRace ?: hero.targetRace).displayName}.")
        }
        return notes
    }

    fun starterBuildTemplates(): List<CustomBuild> = listOf(
        CustomBuild(
            name = "Chaos Field Carry",
            role = HeroRole.FARMER_AOE,
            targetRace = TargetRace.DEMON,
            notes = "Community standard for Torment/Chaos wave clearing.",
            statRanges = mapOf(
                StatType.ATK_SPD to StatTargetRange(StatType.ATK_SPD, 0.25f, 0.25f, 3.0f),
                StatType.CRIT_CHANCE to StatTargetRange(StatType.CRIT_CHANCE, 45f, 50f, 3.0f),
                StatType.EVASION to StatTargetRange(StatType.EVASION, 35f, 40f, 2.5f),
                StatType.CRIT_DMG to StatTargetRange(StatType.CRIT_DMG, 180f, 320f, 2.5f),
                StatType.ATK_PCT to StatTargetRange(StatType.ATK_PCT, 100f, 220f, 2.0f),
                StatType.DEMON_DMG to StatTargetRange(StatType.DEMON_DMG, 80f, 240f, 2.0f)
            )
        ),
        CustomBuild(
            name = "Immortal Frontline Tank",
            role = HeroRole.TANK_ANCHOR,
            targetRace = TargetRace.ALL,
            notes = "Inquisitor / Crusader frontline survival setup.",
            statRanges = mapOf(
                StatType.DMG_REDUCTION to StatTargetRange(StatType.DMG_REDUCTION, 60f, 75f, 3.0f),
                StatType.EVASION to StatTargetRange(StatType.EVASION, 38f, 40f, 3.0f),
                StatType.DEF_PCT to StatTargetRange(StatType.DEF_PCT, 100f, 180f, 2.0f),
                StatType.HP_PCT to StatTargetRange(StatType.HP_PCT, 90f, 180f, 2.0f),
                StatType.LIFESTEAL to StatTargetRange(StatType.LIFESTEAL, 12f, 22f, 2.0f)
            )
        ),
        CustomBuild(
            name = "Darkness / World Boss",
            role = HeroRole.BOSS_SHREDDER,
            targetRace = TargetRace.BOSS,
            notes = "Arcane Archer / Executioner boss melt build.",
            statRanges = mapOf(
                StatType.ATK_SPD to StatTargetRange(StatType.ATK_SPD, 0.25f, 0.25f, 3.0f),
                StatType.CRIT_CHANCE to StatTargetRange(StatType.CRIT_CHANCE, 48f, 50f, 3.0f),
                StatType.BOSS_DMG to StatTargetRange(StatType.BOSS_DMG, 120f, 260f, 3.0f),
                StatType.CRIT_DMG to StatTargetRange(StatType.CRIT_DMG, 200f, 320f, 2.5f),
                StatType.IGNORE_DEF to StatTargetRange(StatType.IGNORE_DEF, 30f, 70f, 2.5f),
                StatType.ATK_PCT to StatTargetRange(StatType.ATK_PCT, 110f, 220f, 2.0f)
            )
        )
    )

    val communityGuides: List<CommunityGuideSection> = listOf(
        CommunityGuideSection(
            id = "caps",
            title = "Core Stat Caps & Breakpoints",
            badge = "MANDATORY MATH",
            summary = "Evil Hunter Tycoon enforces strict stat caps. Going over a cap yields 0 combat gain — always check caps before rolling or equipping.",
            keyRules = listOf(
                "Attack Speed Cap: 0.25 total bonus (lowest attack delay). Factor in Swift/Heroic trait, Quick Strike/Fury buffs, and Guild Buffs first.",
                "Crit Chance Cap: 50% from gear/runes/traits (combines with base & class passives). Never stack past 50% on tooltips.",
                "Evasion Cap: 40% hard cap across all classes. Essential for Chaos Boost & Colosseum survival.",
                "Damage Reduction: Soft target 60–70% for Paladin/Inquisitor tanks; 25–40% helps DPS survive Super Boost/Chaos.",
                "Lifesteal: 12–20% is plenty for Berserkers/Dark Lords; over-stacking sacrifices Crit DMG."
            )
        ),
        CommunityGuideSection(
            id = "classes",
            title = "Class & 3rd Advancement Priorities",
            badge = "META TIERS",
            summary = "Stat weights shift dramatically by 3rd Class Advancement and whether the hunter farms Chaos fields or World Bosses.",
            keyRules = listOf(
                "Executioner / Sword Saint: 0.25 ASPD -> 50% Crit -> 40% Eva -> Crit DMG% -> ATK% -> Demon/Zone DMG.",
                "Inquisitor / Crusader (Tank): DMG Reduction (65%+) -> 40% Eva -> DEF% -> HP% -> Lifesteal.",
                "Arcane Archer (Boss / Darkness): 0.25 ASPD -> 50% Crit -> Boss DMG% -> Crit DMG% -> Ignore DEF%.",
                "Illusionist (Sorcerer): 40% Eva -> Skill Cast% -> Crit DMG% -> ATK% (less reliant on 0.25 ASPD for Meteor)."
            )
        ),
        CommunityGuideSection(
            id = "tiers",
            title = "Gear Tier Ladder & Roll Quality",
            badge = "KEEP VS SALVAGE",
            summary = "Higher tiers unlock bigger single-line roll ceilings and 4-stat perfection.",
            keyRules = listOf(
                "Progression: Heroic -> Legendary -> Ultimate -> Boss -> Primal -> Original -> Chaos (Abyssal).",
                "God Roll Rule: A 4-line Primal/Original piece with 3 synergistic DPS lines (e.g. ASPD + Crit + Crit DMG + ATK%) beats a poorly rolled Chaos item.",
                "Always save at least 2–3 pieces of Attack Speed gear in your Vault so new recruits can hit 0.25 ASPD immediately."
            )
        ),
        CommunityGuideSection(
            id = "runes",
            title = "Rune Min-Maxing Strategy",
            badge = "LOADOUT GLUE",
            summary = "Runes exist to patch missing caps first, then stack pure damage multipliers.",
            keyRules = listOf(
                "Rule #1: Use Attack Speed / Crit Chance / Evasion runes ONLY to fill gaps below 0.25 ASPD, 50% Crit, or 40% Eva.",
                "Rule #2: Once capped, slot Crit DMG%, ATK%, Boss DMG%, or Demon/Primate/Animal/Undead DMG runes.",
                "Rule #3: Match Racial DMG runes (e.g., Demon DMG in Torment/Chaos) for massive multiplicative clear speed."
            )
        )
    )
}
