package com.example

import com.example.domain.CommunityMinMaxEngine
import com.example.domain.EhtOcrParser
import com.example.domain.GearSlot
import com.example.domain.GearTier
import com.example.domain.HeroAdvancement
import com.example.domain.HeroBaseClass
import com.example.domain.HeroProfile
import com.example.domain.ItemKind
import com.example.domain.StatMap
import com.example.domain.StatType
import com.example.domain.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GearSafetyTest {
    @Test
    fun `reads red penalties as losses and tank mitigation as an activation chance`() {
        val item = requireNotNull(EhtOcrParser.parse("""
            Original Gloves
            Glove Type
            Critical Chance − 9%
            14% chance of decreasing damage taken by 25%
            Increase Against Boss type 27% damage
            Empty rune slot(s)
        """.trimIndent()))

        assertEquals(ItemKind.GEAR, item.kind)
        assertEquals(GearSlot.GLOVES, item.slot)
        assertEquals(-9f, item.stats.critChance)
        assertEquals(14f, item.stats.dmgReduction)
        assertEquals(27f, item.stats.bossDmg)
        assertEquals("-9%", StatType.CRIT_CHANCE.formatValue(item.stats.critChance))
        assertTrue("CRIT_CHANCE=-9.0" in item.stats.signatureKey())
    }

    @Test
    fun `keeps a single penalty and ignores unrelated mitigation mechanics`() {
        val item = requireNotNull(EhtOcrParser.parse("""
            Original Boots
            Boot Type
            Decrease Attack Speed by 6%
            Damage Reduction 25%
        """.trimIndent()))

        assertEquals(-0.06f, item.stats.atkSpd, 0.0001f)
        assertEquals(0f, item.stats.dmgReduction)
    }

    @Test
    fun `allocates a drop once and never borrows equipped gear or runes`() {
        val first = hunter(1L)
        val second = hunter(2L)
        val worn = weapon(11L, attack = 20f, owner = second.id)
        val drop = weapon(12L, attack = 25f)
        val rune = VaultItem(13L, "Attack rune", ItemKind.RUNE, GearSlot.RUNE,
            GearTier.CHAOS, stats = StatMap(atkPct = 100f))

        val plan = CommunityMinMaxEngine.planAllHeroGearChanges(
            listOf(first, second), listOf(worn, drop, rune), emptyMap()
        )

        assertEquals(1, plan.totalChangesCount)
        assertEquals(drop, plan.heroGroups.first().changes.single().suggestedItem)
        assertEquals(
            CommunityMinMaxEngine.heroBaseBuffStats(first).atkPct + drop.stats.atkPct,
            plan.heroGroups.first().recommendedLoadout.rawTotals.atkPct
        )
        assertEquals(worn, plan.heroGroups.last().recommendedLoadout.slotRecommendations.first().bestItem)
    }

    @Test
    fun `keeps the current weapon if a higher tier drop is a downgrade`() {
        val hunter = hunter(1L)
        val worn = weapon(11L, attack = 25f, owner = hunter.id).copy(tier = GearTier.PRIMAL)
        val drop = weapon(12L, attack = 5f)
        val loadout = CommunityMinMaxEngine.optimizeLoadout(hunter, listOf(worn, drop))

        assertEquals(worn, loadout.slotRecommendations.first().bestItem)
        assertTrue(CommunityMinMaxEngine.gearChangesFor(loadout).isEmpty())
    }

    private fun hunter(id: Long) = HeroProfile(
        id = id,
        name = "Hunter $id",
        baseClass = HeroBaseClass.BERSERKER,
        advancement = HeroAdvancement.EXECUTIONER
    )

    private fun weapon(id: Long, attack: Float, owner: Long? = null) = VaultItem(
        id = id,
        name = "Sword $id",
        kind = ItemKind.GEAR,
        slot = GearSlot.WEAPON,
        tier = GearTier.CHAOS,
        stats = StatMap(atkPct = attack),
        equippedHeroId = owner
    )
}
