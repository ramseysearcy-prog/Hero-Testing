package com.example.data

import androidx.room.withTransaction
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.CustomBuild
import com.example.domain.HeroProfile
import com.example.domain.OptimizedLoadoutResult
import com.example.domain.VaultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ForgeRepository(private val db: AppDatabase) {
    private val vaultDao = db.vaultDao()
    private val heroDao = db.heroDao()
    private val buildDao = db.buildDao()

    val vaultItems: Flow<List<VaultItem>> = vaultDao.observeAllItems()
        .map { entities -> entities.map { it.toDomain() } }

    val heroes: Flow<List<HeroProfile>> = heroDao.observeAllHeroes()
        .map { entities -> entities.map { it.toDomain() } }

    val customBuilds: Flow<List<CustomBuild>> = buildDao.observeAllBuilds()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun getAllItemsOnce(): List<VaultItem> =
        vaultDao.getAllItemsOnce().map { it.toDomain() }

    suspend fun getAllHeroesOnce(): List<HeroProfile> =
        heroDao.getAllHeroesOnce().map { it.toDomain() }

    suspend fun getAllBuildsOnce(): List<CustomBuild> =
        buildDao.getAllBuildsOnce().map { it.toDomain() }

    suspend fun saveItem(item: VaultItem): Long =
        vaultDao.insertItem(item.toEntity())

    suspend fun saveItemsDeduplicated(items: List<VaultItem>): Int {
        if (items.isEmpty()) return 0
        val existing = getAllItemsOnce()
        val existingSignatures = existing.map {
            "${it.kind.name}|${it.slot.name}|${it.tier.name}|${it.stats.signatureKey()}"
        }.toMutableSet()

        val newEntities = mutableListOf<VaultItemEntity>()
        for (item in items) {
            val sig = "${item.kind.name}|${item.slot.name}|${item.tier.name}|${item.stats.signatureKey()}"
            if (existingSignatures.add(sig)) {
                newEntities.add(item.toEntity())
            }
        }
        if (newEntities.isNotEmpty()) {
            vaultDao.insertItems(newEntities)
        }
        return newEntities.size
    }

    suspend fun deleteItem(id: Long) = vaultDao.deleteItemById(id)

    suspend fun saveHero(hero: HeroProfile): Long =
        heroDao.insertHero(hero.toEntity())

    suspend fun deleteHero(id: Long) {
        db.withTransaction {
            vaultDao.unequipHeroItems(id)
            heroDao.deleteHeroById(id)
        }
    }

    suspend fun saveBuild(build: CustomBuild): Long =
        buildDao.insertBuild(build.toEntity())

    suspend fun deleteBuild(buildId: Long) {
        db.withTransaction {
            heroDao.clearBuildAssignment(buildId)
            buildDao.deleteBuildById(buildId)
        }
    }

    suspend fun equipLoadoutToHero(heroId: Long, itemIds: List<Long>) {
        db.withTransaction {
            vaultDao.unequipHeroItems(heroId)
            if (itemIds.isNotEmpty()) {
                vaultDao.equipItemsToHero(heroId, itemIds)
            }
        }
    }

    suspend fun applySingleGearChange(heroId: Long, newItemId: Long, replacedItemId: Long?) {
        db.withTransaction {
            if (replacedItemId != null) {
                vaultDao.clearEquippedItemById(replacedItemId)
            }
            vaultDao.setItemEquippedHero(newItemId, heroId)
        }
    }

    suspend fun applyAllRosterLoadouts(heroLoadouts: List<Pair<Long, List<Long>>>) {
        db.withTransaction {
            for ((heroId, itemIds) in heroLoadouts) {
                vaultDao.unequipHeroItems(heroId)
                if (itemIds.isNotEmpty()) {
                    vaultDao.equipItemsToHero(heroId, itemIds)
                }
            }
        }
    }

    suspend fun runRosterOptimizationSummary(): List<OptimizedLoadoutResult> {
        val allItems = getAllItemsOnce()
        val allHeroes = getAllHeroesOnce()
        val buildsById = getAllBuildsOnce().associateBy { it.id }
        val plan = CommunityMinMaxEngine.planAllHeroGearChanges(allHeroes, allItems, buildsById)
        return plan.heroGroups.map { it.recommendedLoadout }
    }
}
