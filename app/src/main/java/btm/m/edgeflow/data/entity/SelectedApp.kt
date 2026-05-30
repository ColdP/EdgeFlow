package btm.m.edgeflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-selected app displayed in the sidebar grid.
 */
@Entity(tableName = "selected_apps")
data class SelectedApp(
    @PrimaryKey
    val packageName: String,
    val label: String,
    val sortOrder: Int = 0
)