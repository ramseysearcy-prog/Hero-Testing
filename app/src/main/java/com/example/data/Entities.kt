package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.BuildArchetype
import com.example.domain.CustomBuild
import com.example.domain.GearSlot
import com.example.domain.GearTier
import com.example.domain.HeroAdvancement
import com.example.domain.HeroBaseClass
import com.example.domain.HeroProfile
import com.example.domain.HeroRole
import com.example.domain.HeroTrait
import com.example.domain.ItemKind
import com.example.domain.ScanSource
import com.example.domain.StatMap
import com.example.domain.StatTargetRange
import com.example.domain.StatType
import com.example.domain.TargetRace
import com.example.domain.VaultItem

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val kind: String,
    val slot: String,
    val tier: String,
    val enhancementLevel: Int,
    val atkPct: Float,
    val atkSpd: Float,
    val critChance: Float,
    val critDmg: Float,
    val evasion: Float,
    val defPct: Float,
    val hpPct: Float,
    val dmgReduction: Float,
    val lifesteal: Float,
    val bossDmg: Float,
    val demonDmg: Float,
    val primateDmg: Float,
    val animalDmg: Float,
    val undeadDmg: Float,
    val ignoreDef: Float,
    val skillCast: Float,
    val moveSpd: Float,
    val rawOcrText: String,
    val source: String,
    val equippedHeroId: Long?,
    val createdAt: Long
)

@Entity(tableName = "hero_profiles")
data class HeroProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val baseClass: String,
    val advancement: String,
    val role: String,
    val assignedBuildId: Long?,
    val trait: String,
    val archetype: String,
    val targetRace: String,
    val runeSlots: Int,
    val hasQuickStrikeBuff: Boolean,
    val guildAspdBuffPct: Float,
    val guildCritBuffPct: Float,
    val createdAt: Long
)

@Entity(tableName = "custom_builds")
data class CustomBuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val role: String,
    val targetRace: String,
    val notes: String,
    val encodedRanges: String,
    val createdAt: Long
)

fun VaultItemEntity.toDomain(): VaultItem = VaultItem(
    id = id,
    name = name,
    kind = enumValues<ItemKind>().firstOrNull { it.name == kind } ?: ItemKind.GEAR,
    slot = enumValues<GearSlot>().firstOrNull { it.name == slot } ?: GearSlot.WEAPON,
    tier = enumValues<GearTier>().firstOrNull { it.name == tier } ?: GearTier.HEROIC,
    enhancementLevel = enhancementLevel,
    stats = StatMap(
        atkPct = atkPct,
        atkSpd = atkSpd,
        critChance = critChance,
        critDmg = critDmg,
        evasion = evasion,
        defPct = defPct,
        hpPct = hpPct,
        dmgReduction = dmgReduction,
        lifesteal = lifesteal,
        bossDmg = bossDmg,
        demonDmg = demonDmg,
        primateDmg = primateDmg,
        animalDmg = animalDmg,
        undeadDmg = undeadDmg,
        ignoreDef = ignoreDef,
        skillCast = skillCast,
        moveSpd = moveSpd
    ),
    rawOcrText = rawOcrText,
    source = enumValues<ScanSource>().firstOrNull { it.name == source } ?: ScanSource.MANUAL,
    equippedHeroId = equippedHeroId,
    createdAt = createdAt
)

fun VaultItem.toEntity(): VaultItemEntity = VaultItemEntity(
    id = id,
    name = name,
    kind = kind.name,
    slot = slot.name,
    tier = tier.name,
    enhancementLevel = enhancementLevel,
    atkPct = stats.atkPct,
    atkSpd = stats.atkSpd,
    critChance = stats.critChance,
    critDmg = stats.critDmg,
    evasion = stats.evasion,
    defPct = stats.defPct,
    hpPct = stats.hpPct,
    dmgReduction = stats.dmgReduction,
    lifesteal = stats.lifesteal,
    bossDmg = stats.bossDmg,
    demonDmg = stats.demonDmg,
    primateDmg = stats.primateDmg,
    animalDmg = stats.animalDmg,
    undeadDmg = stats.undeadDmg,
    ignoreDef = stats.ignoreDef,
    skillCast = stats.skillCast,
    moveSpd = stats.moveSpd,
    rawOcrText = rawOcrText,
    source = source.name,
    equippedHeroId = equippedHeroId,
    createdAt = createdAt
)

fun HeroProfileEntity.toDomain(): HeroProfile = HeroProfile(
    id = id,
    name = name,
    baseClass = enumValues<HeroBaseClass>().firstOrNull { it.name == baseClass } ?: HeroBaseClass.BERSERKER,
    advancement = enumValues<HeroAdvancement>().firstOrNull { it.name == advancement } ?: HeroAdvancement.EXECUTIONER,
    role = enumValues<HeroRole>().firstOrNull { it.name == role } ?: HeroRole.MAIN_CARRY,
    assignedBuildId = assignedBuildId,
    trait = enumValues<HeroTrait>().firstOrNull { it.name == trait } ?: HeroTrait.HEROIC,
    archetype = enumValues<BuildArchetype>().firstOrNull { it.name == archetype } ?: BuildArchetype.FIELD_CHAOS,
    targetRace = enumValues<TargetRace>().firstOrNull { it.name == targetRace } ?: TargetRace.DEMON,
    runeSlots = runeSlots.coerceIn(1, 5),
    hasQuickStrikeBuff = hasQuickStrikeBuff,
    guildAspdBuffPct = guildAspdBuffPct,
    guildCritBuffPct = guildCritBuffPct,
    createdAt = createdAt
)

fun HeroProfile.toEntity(): HeroProfileEntity = HeroProfileEntity(
    id = id,
    name = name,
    baseClass = baseClass.name,
    advancement = advancement.name,
    role = role.name,
    assignedBuildId = assignedBuildId,
    trait = trait.name,
    archetype = archetype.name,
    targetRace = targetRace.name,
    runeSlots = runeSlots.coerceIn(1, 5),
    hasQuickStrikeBuff = hasQuickStrikeBuff,
    guildAspdBuffPct = guildAspdBuffPct,
    guildCritBuffPct = guildCritBuffPct,
    createdAt = createdAt
)

fun CustomBuildEntity.toDomain(): CustomBuild {
    val parsedRanges = encodedRanges
        .split(";")
        .mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 4) return@mapNotNull null
            val type = enumValues<StatType>().firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val minV = parts[1].toFloatOrNull() ?: 0f
            val maxV = parts[2].toFloatOrNull() ?: type.suggestedLoadoutMax
            val weight = parts[3].toFloatOrNull() ?: 2.0f
            type to StatTargetRange(type, minV, maxV.coerceAtLeast(minV), weight)
        }
        .toMap()

    return CustomBuild(
        id = id,
        name = name,
        role = enumValues<HeroRole>().firstOrNull { it.name == role } ?: HeroRole.MAIN_CARRY,
        targetRace = enumValues<TargetRace>().firstOrNull { it.name == targetRace } ?: TargetRace.DEMON,
        notes = notes,
        statRanges = parsedRanges,
        createdAt = createdAt
    )
}

fun CustomBuild.toEntity(): CustomBuildEntity {
    val encoded = statRanges.values.joinToString(";") { r ->
        "${r.statType.name}:${r.minDesired}:${r.maxDesired}:${r.priorityWeight}"
    }
    return CustomBuildEntity(
        id = id,
        name = name,
        role = role.name,
        targetRace = targetRace.name,
        notes = notes,
        encodedRanges = encoded,
        createdAt = createdAt
    )
}
