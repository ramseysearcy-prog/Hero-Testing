package com.example.domain

import kotlin.math.abs

enum class ItemKind(val displayName: String) {
    GEAR("Gear"),
    RUNE("Rune")
}

enum class GearSlot(val displayName: String, val isEquipment: Boolean = true) {
    WEAPON("Weapon"),
    ARMOR("Armor"),
    HELM("Helm"),
    GLOVES("Gloves"),
    BOOTS("Boots"),
    NECKLACE("Necklace"),
    RING("Ring"),
    BELT("Belt"),
    RUNE("Rune", isEquipment = false);

    companion object {
        val equipmentSlots: List<GearSlot> = entries.filter { it.isEquipment }
    }
}

enum class GearTier(
    val displayName: String,
    val rank: Int,
    val singleLineCeilingScale: Float
) {
    HEROIC("Heroic", 1, 0.55f),
    LEGENDARY("Legendary", 2, 0.68f),
    ULTIMATE("Ultimate", 3, 0.78f),
    BOSS("Boss", 4, 0.85f),
    PRIMAL("Primal", 5, 0.92f),
    ORIGINAL("Original", 6, 0.96f),
    CHAOS("Chaos", 7, 1.00f)
}

enum class ScanSource(val displayName: String) {
    LIVE_MONITOR("Live Monitor"),
    VIDEO_OCR("Video OCR"),
    SCREENSHOT_OCR("Screenshot OCR"),
    MANUAL("Manual Entry")
}

enum class StatType(
    val label: String,
    val shortLabel: String,
    val isFlatDecimal: Boolean = false,
    val maxSingleRollBenchmark: Float,
    val suggestedLoadoutMax: Float
) {
    ATK_PCT("Attack %", "ATK", false, 26f, 220f),
    ATK_SPD("Attack Speed", "ASPD", true, 0.14f, 0.25f),
    CRIT_CHANCE("Crit Chance %", "CRIT%", false, 14f, 50f),
    CRIT_DMG("Crit Damage %", "CRIT DMG", false, 40f, 320f),
    EVASION("Evasion %", "EVA", false, 14f, 40f),
    DEF_PCT("Defense %", "DEF", false, 26f, 180f),
    HP_PCT("Max HP %", "HP", false, 26f, 180f),
    DMG_REDUCTION("Chance to Reduce Damage by 25%", "REDUCE CHANCE", false, 17f, 75f),
    LIFESTEAL("Lifesteal %", "LIFESTEAL", false, 12f, 25f),
    BOSS_DMG("Boss DMG %", "BOSS", false, 40f, 260f),
    DEMON_DMG("Demon DMG %", "DEMON", false, 40f, 260f),
    PRIMATE_DMG("Primate DMG %", "PRIMATE", false, 40f, 260f),
    ANIMAL_DMG("Animal DMG %", "ANIMAL", false, 40f, 260f),
    UNDEAD_DMG("Undead DMG %", "UNDEAD", false, 40f, 260f),
    IGNORE_DEF("Ignore DEF %", "IGN DEF", false, 16f, 80f),
    SKILL_CAST("Skill Cast %", "CAST%", false, 12f, 50f),
    MOVE_SPD("Move Speed %", "MV SPD", false, 20f, 80f);

    fun formatValue(value: Float): String = formatDelta(value)

    fun formatPlainValue(value: Float): String {
        return if (isFlatDecimal) {
            String.format(java.util.Locale.US, "%.2f", value)
        } else {
            if (abs(value - value.toInt()) < 0.05f) {
                "${value.toInt()}%"
            } else {
                String.format(java.util.Locale.US, "%.1f%%", value)
            }
        }
    }

    fun formatDelta(delta: Float): String {
        val sign = if (delta >= 0f) "+" else "-"
        val mag = abs(delta)
        return if (isFlatDecimal) {
            String.format(java.util.Locale.US, "%s%.2f", sign, mag)
        } else {
            if (abs(mag - mag.toInt()) < 0.05f) {
                "$sign${mag.toInt()}%"
            } else {
                String.format(java.util.Locale.US, "%s%.1f%%", sign, mag)
            }
        }
    }
}

data class StatMap(
    val atkPct: Float = 0f,
    val atkSpd: Float = 0f,
    val critChance: Float = 0f,
    val critDmg: Float = 0f,
    val evasion: Float = 0f,
    val defPct: Float = 0f,
    val hpPct: Float = 0f,
    val dmgReduction: Float = 0f,
    val lifesteal: Float = 0f,
    val bossDmg: Float = 0f,
    val demonDmg: Float = 0f,
    val primateDmg: Float = 0f,
    val animalDmg: Float = 0f,
    val undeadDmg: Float = 0f,
    val ignoreDef: Float = 0f,
    val skillCast: Float = 0f,
    val moveSpd: Float = 0f
) {
    operator fun plus(other: StatMap): StatMap = StatMap(
        atkPct = atkPct + other.atkPct,
        atkSpd = atkSpd + other.atkSpd,
        critChance = critChance + other.critChance,
        critDmg = critDmg + other.critDmg,
        evasion = evasion + other.evasion,
        defPct = defPct + other.defPct,
        hpPct = hpPct + other.hpPct,
        dmgReduction = dmgReduction + other.dmgReduction,
        lifesteal = lifesteal + other.lifesteal,
        bossDmg = bossDmg + other.bossDmg,
        demonDmg = demonDmg + other.demonDmg,
        primateDmg = primateDmg + other.primateDmg,
        animalDmg = animalDmg + other.animalDmg,
        undeadDmg = undeadDmg + other.undeadDmg,
        ignoreDef = ignoreDef + other.ignoreDef,
        skillCast = skillCast + other.skillCast,
        moveSpd = moveSpd + other.moveSpd
    )

    fun valueOf(type: StatType): Float = when (type) {
        StatType.ATK_PCT -> atkPct
        StatType.ATK_SPD -> atkSpd
        StatType.CRIT_CHANCE -> critChance
        StatType.CRIT_DMG -> critDmg
        StatType.EVASION -> evasion
        StatType.DEF_PCT -> defPct
        StatType.HP_PCT -> hpPct
        StatType.DMG_REDUCTION -> dmgReduction
        StatType.LIFESTEAL -> lifesteal
        StatType.BOSS_DMG -> bossDmg
        StatType.DEMON_DMG -> demonDmg
        StatType.PRIMATE_DMG -> primateDmg
        StatType.ANIMAL_DMG -> animalDmg
        StatType.UNDEAD_DMG -> undeadDmg
        StatType.IGNORE_DEF -> ignoreDef
        StatType.SKILL_CAST -> skillCast
        StatType.MOVE_SPD -> moveSpd
    }

    fun withStat(type: StatType, value: Float): StatMap = when (type) {
        StatType.ATK_PCT -> copy(atkPct = value)
        StatType.ATK_SPD -> copy(atkSpd = value)
        StatType.CRIT_CHANCE -> copy(critChance = value)
        StatType.CRIT_DMG -> copy(critDmg = value)
        StatType.EVASION -> copy(evasion = value)
        StatType.DEF_PCT -> copy(defPct = value)
        StatType.HP_PCT -> copy(hpPct = value)
        StatType.DMG_REDUCTION -> copy(dmgReduction = value)
        StatType.LIFESTEAL -> copy(lifesteal = value)
        StatType.BOSS_DMG -> copy(bossDmg = value)
        StatType.DEMON_DMG -> copy(demonDmg = value)
        StatType.PRIMATE_DMG -> copy(primateDmg = value)
        StatType.ANIMAL_DMG -> copy(animalDmg = value)
        StatType.UNDEAD_DMG -> copy(undeadDmg = value)
        StatType.IGNORE_DEF -> copy(ignoreDef = value)
        StatType.SKILL_CAST -> copy(skillCast = value)
        StatType.MOVE_SPD -> copy(moveSpd = value)
    }

    fun nonZeroEntries(): List<Pair<StatType, Float>> =
        StatType.entries.mapNotNull { type ->
            val v = valueOf(type)
            if (abs(v) > 0.001f) type to v else null
        }

    fun signatureKey(): String =
        nonZeroEntries().joinToString("|") { (t, v) ->
            val rounded = if (t.isFlatDecimal) String.format(java.util.Locale.US, "%.2f", v)
            else String.format(java.util.Locale.US, "%.1f", v)
            "${t.name}=$rounded"
        }
}

data class VaultItem(
    val id: Long = 0L,
    val name: String,
    val kind: ItemKind,
    val slot: GearSlot,
    val tier: GearTier,
    val enhancementLevel: Int = 0,
    val stats: StatMap,
    val rawOcrText: String = "",
    val source: ScanSource = ScanSource.MANUAL,
    val equippedHeroId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HeroRole(val displayName: String, val summary: String) {
    MAIN_CARRY("Main DPS Carry", "Primary field & wave damage dealer"),
    TANK_ANCHOR("Tank / Frontline", "Holds frontline & absorbs boss burst"),
    BOSS_SHREDDER("Boss Shredder", "Single-target World Boss / Darkness / Molen"),
    PVP_BRUISER("Colosseum / PvP", "Evasion, DMG Reduction & Ignore DEF"),
    FARMER_AOE("Chaos Farmer", "Speed clear & racial damage multiplier"),
    SUPPORT_HYBRID("Support / Hybrid", "Balanced skill cast, speed & sustain")
}

data class StatTargetRange(
    val statType: StatType,
    val minDesired: Float,
    val maxDesired: Float,
    val priorityWeight: Float = 2.0f
)

data class CustomBuild(
    val id: Long = 0L,
    val name: String,
    val role: HeroRole = HeroRole.MAIN_CARRY,
    val targetRace: TargetRace = TargetRace.DEMON,
    val notes: String = "",
    val statRanges: Map<StatType, StatTargetRange> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class HeroBaseClass(
    val displayName: String,
    val baseAtkSpeedNote: String,
    val roleSummary: String
) {
    BERSERKER("Berserker", "Fastest scaling with Fury/Berserk", "Melee AoE & Chaos Field Carry"),
    PALADIN("Paladin", "Tank & Sacred Barrier frontline", "Sustain Tank / Dungeon Anchor"),
    RANGER("Ranger", "Long-range sustained crit DPS", "Single-Target & Boss Shredder"),
    SORCERER("Sorcerer", "Meteor / Blizzard AoE burst", "Skill Cast & AoE Wave Clear"),
    DARK_LORD("Dark Lord", "Hybrid crit & curse powerhouse", "Endgame Carry / Raid & PvP")
}

enum class HeroAdvancement(
    val displayName: String,
    val baseClass: HeroBaseClass,
    val communityTip: String
) {
    SLAYER("Slayer", HeroBaseClass.BERSERKER, "Cap 0.25 ASPD early, stack Crit DMG & Demon DMG."),
    BARBARIAN("Barbarian", HeroBaseClass.BERSERKER, "Benefits from Lifesteal + 40% Eva for frontline survival."),
    EXECUTIONER("Executioner", HeroBaseClass.BERSERKER, "Premier Chaos field clearer; prioritize 0.25 ASPD, 50% Crit, Crit DMG."),
    CRUSADER("Crusader", HeroBaseClass.PALADIN, "Focus DMG Reduction + DEF + HP to anchor high-tier bosses."),
    TEMPLAR("Templar", HeroBaseClass.PALADIN, "Balanced bruiser; pair 40% Eva with DMG Reduction."),
    INQUISITOR("Inquisitor", HeroBaseClass.PALADIN, "Top-tier Chaos tank/hybrid; stack DMG Reduction, Eva 40%, Crit DMG."),
    SCOUT("Scout", HeroBaseClass.RANGER, "Needs 0.25 ASPD + 50% Crit + high Crit DMG."),
    MINSTREL("Minstrel", HeroBaseClass.RANGER, "Support/DPS hybrid; needs Skill Cast + ASPD + Evasion."),
    ARCANE_ARCHER("Arcane Archer", HeroBaseClass.RANGER, "World Boss & Darkness specialist; prioritize Boss DMG, Crit DMG, Ignore DEF."),
    WIZARD("Wizard", HeroBaseClass.SORCERER, "Stack Skill Cast, Crit DMG, ATK%, and 40% Eva."),
    ILLUSIONIST("Illusionist", HeroBaseClass.SORCERER, "Best PvP & Chaos mage; 40% Evasion + Skill Cast + Crit DMG."),
    DARK_WIZARD("Dark Wizard", HeroBaseClass.SORCERER, "Huge boss burst; stack Boss DMG, Crit DMG, and ATK%."),
    SWORD_SAINT("Sword Saint", HeroBaseClass.DARK_LORD, "Ultra-fast melee carry; cap 0.25 ASPD, 50% Crit, max Crit DMG."),
    SHADOW_KNIGHT("Shadow Knight", HeroBaseClass.DARK_LORD, "Strong lifesteal bruiser; balance Crit DMG + DMG Reduction."),
    HELLBRINGER("Hellbringer", HeroBaseClass.DARK_LORD, "Endgame AoE & Boss destroyer; stack Crit DMG, ATK%, Boss/Demon DMG.");

    companion object {
        fun forClass(baseClass: HeroBaseClass): List<HeroAdvancement> =
            entries.filter { it.baseClass == baseClass }
    }
}

enum class HeroTrait(
    val displayName: String,
    val description: String,
    val bonusStats: StatMap
) {
    HEROIC("Heroic", "+7% ATK, DEF, HP, Crit, Eva, ASPD", StatMap(atkPct = 7f, defPct = 7f, hpPct = 7f, critChance = 7f, evasion = 7f, atkSpd = 0.02f)),
    SWIFT("Swift", "+10% Attack Speed bonus (~+0.03 ASPD)", StatMap(atkSpd = 0.03f)),
    SHARP("Sharp", "+10% Critical Chance", StatMap(critChance = 10f)),
    NIMBLE("Nimble", "+10% Evasion", StatMap(evasion = 10f)),
    CHARISMATIC("Charismatic / Strong", "+10% Attack", StatMap(atkPct = 10f)),
    THICK_SKINNED("Thick-Skinned", "+12% Defense", StatMap(defPct = 12f)),
    MAN_OF_STEEL("Man of Steel", "+14% Max HP", StatMap(hpPct = 14f)),
    NEUTRAL("Balanced / Other", "No extra trait combat modifier", StatMap())
}

enum class BuildArchetype(val displayName: String, val subtitle: String) {
    FIELD_CHAOS("Field Farming (Chaos / Boost)", "0.25 ASPD + 50% Crit + 40% Eva + Race DMG"),
    WORLD_BOSS("Darkness / World Boss", "0.25 ASPD + Boss DMG + Crit DMG + Ignore DEF"),
    COLOSSEUM_PVP("Colosseum / PvP", "40% Eva + DMG Reduction + Ignore DEF + HP"),
    TANK_FRONTLINE("Tank / Frontline Anchor", "DMG Reduction 65%+ + 40% Eva + DEF + Lifesteal"),
    MOLEN_RAID("Molen / Guild Raid", "Max Crit DMG + Boss DMG + ATK% + 0.25 ASPD")
}

enum class TargetRace(val displayName: String, val statType: StatType?) {
    ALL("All / Balanced", null),
    DEMON("Demon (Torment / Chaos)", StatType.DEMON_DMG),
    PRIMATE("Primate", StatType.PRIMATE_DMG),
    ANIMAL("Animal", StatType.ANIMAL_DMG),
    UNDEAD("Undead", StatType.UNDEAD_DMG),
    BOSS("Bosses Only", StatType.BOSS_DMG)
}

data class HeroProfile(
    val id: Long = 0L,
    val name: String,
    val baseClass: HeroBaseClass,
    val advancement: HeroAdvancement,
    val role: HeroRole = HeroRole.MAIN_CARRY,
    val assignedBuildId: Long? = null,
    val trait: HeroTrait = HeroTrait.HEROIC,
    val archetype: BuildArchetype = BuildArchetype.FIELD_CHAOS,
    val targetRace: TargetRace = TargetRace.DEMON,
    val runeSlots: Int = 5,
    val hasQuickStrikeBuff: Boolean = true,
    val guildAspdBuffPct: Float = 5f,
    val guildCritBuffPct: Float = 5f,
    val createdAt: Long = System.currentTimeMillis()
)
