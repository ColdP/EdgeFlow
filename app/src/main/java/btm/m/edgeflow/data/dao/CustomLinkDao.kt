package btm.m.edgeflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import btm.m.edgeflow.data.entity.CustomLink
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomLinkDao {
    @Query("SELECT * FROM custom_links ORDER BY id ASC")
    fun getAll(): Flow<List<CustomLink>>

    @Insert
    suspend fun insert(link: CustomLink)

    @Query("DELETE FROM custom_links WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_links")
    suspend fun count(): Int
}