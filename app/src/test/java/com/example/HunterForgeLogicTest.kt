package com.example

import com.example.domain.BuildCodeCodec
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.CustomBuild
import com.example.domain.EhtOcrParser
import com.example.domain.GearSlot
import com.example.domain.GearTier
import com.example.domain.HeroAdvancement
import com.example.domain.HeroBaseClass
import com.example.domain.HeroProfile
import com.example.domain.HeroRole
import com.example.domain.HeroTrait
import com.example.domain.ItemKind
import com.example.domain.StatMap
import com.example.domain.StatTargetRange
import com.example.domain.StatType
import com.example.domain.TargetRace
import com.example.domain.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HunterForgeLogicTest {

    @Test
    fun `ocr parser extracts chaos weapon stats and enhancement level`() {
        val sampleOcr = """
            +15 Chaos Destroyer Sword
            Weapon
            Attack +24%
            Attack Speed +0.12
            Crit Chance +13%
            Crit Damage +36%
            Demon Damage +34%
        """.trimIndent()

        val parsed = EhtOcrParser.parse(sampleOcr)
        assertNotNull(parsed)
        assertEquals(ItemKind.GEAR, parsed!!.kind)
        assertEquals(GearSlot.WEAPON, parsed.slot)
        assertEquals(GearTier.CHAOS, parsed.tier)
        assertEquals(15, parsed.enhancementLevel)
        assertEquals(24f, parsed.stats.atkPct, 0.01f)
        assertEquals(0.12f, parsed.stats.atkSpd, 0.005f)
        assertEquals(13f, parsed.stats.critChance, 0.01f)
        assertEquals(36f, parsed.stats.critDmg, 0.01f)
        assertEquals(34f, parsed.stats.demonDmg, 0.01f)
    }

    @Test
    fun `build code codec round-trips custom build with stat ranges`() {
        val original = CustomBuild(
            id = 42L,
            name = "Torment Executioner",
            role = HeroRole.FARMER_AOE,
            targetRace = TargetRace.DEMON,
            notes = "Hit 0.25 ASPD and stack Demon + Crit DMG",
            statRanges = mapOf(
                StatType.ATK_SPD to StatTargetRange(StatType.ATK_SPD, 0.25f, 0.25f, 3.0f),
                StatType.CRIT_CHANCE to StatTargetRange(StatType.CRIT_CHANCE, 45f, 50f, 3.0f),
                StatType.CRIT_DMG to StatTargetRange(StatType.CRIT_DMG, 180f, 320f, 2.0f)
            )
        )

        val code = BuildCodeCodec.encode(original)
        assertTrue(code.startsWith("HF1:"))

        val decoded = BuildCodeCodec.decode(code).getOrThrow()
        assertEquals("Torment Executioner", decoded.name)
        assertEquals(HeroRole.FARMER_AOE, decoded.role)
        assertEquals(TargetRace.DEMON, decoded.targetRace)
        assertEquals(3, decoded.statRanges.size)
        assertEquals(0.25f, decoded.statRanges[StatType.ATK_SPD]!!.minDesired, 0.001f)
        assertEquals(320f, decoded.statRanges[StatType.CRIT_DMG]!!.maxDesired, 0.01f)

        val bad = BuildCodeCodec.decode("BAD_CODE_123")
        assertTrue(bad.isFailure)
    }

    @Test
    fun `optimizer respects custom build ranges and community hard caps`() {
        val hero = HeroProfile(
            id = 1L,
            name = "Kaelen",
            baseClass = HeroBaseClass.BERSERKER,
            advancement = HeroAdvancement.EXECUTIONER,
            role = HeroRole.FARMER_AOE,
            trait = HeroTrait.HEROIC,
            runeSlots = 2,
            hasQuickStrikeBuff = true,
            guildAspdBuffPct = 5f,
            guildCritBuffPct = 5f
        )

        val tankBuild = CustomBuild(
            id = 7L,
            name = "Immortal Tank",
            role = HeroRole.TANK_ANCHOR,
            targetRace = TargetRace.ALL,
            statRanges = mapOf(
                StatType.DMG_REDUCTION to StatTargetRange(StatType.DMG_REDUCTION, 40f, 70f, 3.0f),
                StatType.DEF_PCT to StatTargetRange(StatType.DEF_PCT, 40f, 120f, 2.0f)
            )
        )

        val dpsArmor = VaultItem(
            id = 10L,
            name = "Chaos Berserk Plate",
            kind = ItemKind.GEAR,
            slot = GearSlot.ARMOR,
            tier = GearTier.CHAOS,
            stats = StatMap(atkPct = 24f, critDmg = 36f, atkSpd = 0.12f)
        )

        val tankArmor = VaultItem(
            id = 11L,
            name = "Primal Aegis Plate",
            kind = ItemKind.GEAR,
            slot = GearSlot.ARMOR,
            tier = GearTier.PRIMAL,
            stats = StatMap(dmgReduction = 17f, defPct = 24f, hpPct = 22f, evasion = 12f)
        )

        val runeCrit = VaultItem(
            id = 20L,
            name = "Crit DMG Rune",
            kind = ItemKind.RUNE,
            slot = GearSlot.RUNE,
            tier = GearTier.ORIGINAL,
            stats = StatMap(critDmg = 30f)
        )

        val runeTank = VaultItem(
            id = 21L,
            name = "Guardian Rune",
            kind = ItemKind.RUNE,
            slot = GearSlot.RUNE,
            tier = GearTier.ORIGINAL,
            stats = StatMap(dmgReduction = 14f, defPct = 18f)
        )

        val result = CommunityMinMaxEngine.optimizeLoadout(
            hero = hero,
            allItems = listOf(dpsArmor, tankArmor, runeCrit, runeTank),
            ignoreItemsEquippedByOtherHeroes = false,
            assignedBuild = tankBuild
        )

        val armorRec = result.slotRecommendations.first { it.slot == GearSlot.ARMOR }
        assertEquals(tankArmor.id, armorRec.bestItem?.id)
        assertTrue(result.buildRangeProgressList.isNotEmpty())
        assertTrue(result.effectiveTotals.dmgReduction <= CommunityMinMaxEngine.SOFT_CAP_DMG_REDUCTION)
    }

    @Test
    fun `planAllHeroGearChanges avoids double-assigning items and detects swaps`() {
        val hero1 = HeroProfile(
            id = 1L,
            name = "Kaelen",
            baseClass = HeroBaseClass.BERSERKER,
            advancement = HeroAdvancement.EXECUTIONER,
            role = HeroRole.MAIN_CARRY,
            runeSlots = 1
        )
        val hero2 = HeroProfile(
            id = 2L,
            name = "Aria",
            baseClass = HeroBaseClass.RANGER,
            advancement = HeroAdvancement.ARCANE_ARCHER,
            role = HeroRole.BOSS_SHREDDER,
            runeSlots = 1
        )

        val greatSword = VaultItem(
            id = 101L,
            name = "Chaos Destroyer",
            kind = ItemKind.GEAR,
            slot = GearSlot.WEAPON,
            tier = GearTier.CHAOS,
            stats = StatMap(atkPct = 25f, atkSpd = 0.12f, critDmg = 36f),
            equippedHeroId = 2L // Currently worn by Aria, should tell Kaelen "Take from Aria"
        )
        val backupBow = VaultItem(
            id = 102L,
            name = "Primal Hunter Bow",
            kind = ItemKind.GEAR,
            slot = GearSlot.WEAPON,
            tier = GearTier.PRIMAL,
            stats = StatMap(atkPct = 20f, bossDmg = 34f, critChance = 10f),
            equippedHeroId = null
        )

        val plan = CommunityMinMaxEngine.planAllHeroGearChanges(
            heroes = listOf(hero1, hero2),
            allItems = listOf(greatSword, backupBow),
            buildsById = emptyMap()
        )

        assertEquals(2, plan.heroGroups.size)
        val group1 = plan.heroGroups[0]
        val group2 = plan.heroGroups[1]

        // Kaelen gets Chaos Destroyer (taken from Aria), Aria gets Primal Hunter Bow
        assertEquals(1, group1.changes.size)
        assertEquals(greatSword.id, group1.changes[0].suggestedItem.id)
        assertEquals("Aria", group1.changes[0].previousOwnerHeroName)

        assertEquals(1, group2.changes.size)
        assertEquals(backupBow.id, group2.changes[0].suggestedItem.id)
        assertEquals(null, group2.changes[0].previousOwnerHeroName)
        assertEquals(2, plan.totalChangesCount)
    }
}
