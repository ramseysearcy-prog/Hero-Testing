package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun observeAllItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items")
    suspend fun getAllItemsOnce(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VaultItemEntity>): List<Long>

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE vault_items SET equippedHeroId = NULL WHERE equippedHeroId = :heroId")
    suspend fun unequipHeroItems(heroId: Long)

    @Query("UPDATE vault_items SET equippedHeroId = :heroId WHERE id = :itemId")
    suspend fun setItemEquippedHero(itemId: Long, heroId: Long)
}

@Dao
interface HeroDao {
    @Query("SELECT * FROM hero_profiles ORDER BY createdAt ASC")
    fun observeAllHeroes(): Flow<List<HeroProfileEntity>>

    @Query("SELECT * FROM hero_profiles ORDER BY createdAt ASC")
    suspend fun getAllHeroesOnce(): List<HeroProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHero(hero: HeroProfileEntity): Long

    @Update
    suspend fun updateHero(hero: HeroProfileEntity)

    @Query("DELETE FROM hero_profiles WHERE id = :id")
    suspend fun deleteHeroById(id: Long)

    @Query("UPDATE hero_profiles SET assignedBuildId = NULL WHERE assignedBuildId = :buildId")
    suspend fun clearBuildAssignment(buildId: Long)
}

@Dao
interface BuildDao {
    @Query("SELECT * FROM custom_builds ORDER BY createdAt ASC")
    fun observeAllBuilds(): Flow<List<CustomBuildEntity>>

    @Query("SELECT * FROM custom_builds ORDER BY createdAt ASC")
    suspend fun getAllBuildsOnce(): List<CustomBuildEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: CustomBuildEntity): Long

    @Query("DELETE FROM custom_builds WHERE id = :id")
    suspend fun deleteBuildById(id: Long)
}
